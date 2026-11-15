package ar.edu.uade.transito.transito_backend.event;

import ar.edu.uade.transito.transito_backend.event.payload.InfractionConfirmedPayload;
import ar.edu.uade.transito.transito_backend.model.Location;
import ar.edu.uade.transito.transito_backend.model.OutboxEvent;
import ar.edu.uade.transito.transito_backend.model.enums.DebtorIdType;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;
import ar.edu.uade.transito.transito_backend.model.enums.OutboxStatus;
import ar.edu.uade.transito.transito_backend.repository.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * DEF-14: el outbox NO cambia el contrato con Core. Compara el mensaje que se publicaba antes
 * (convertAndSend del sobre con el JacksonJsonMessageConverter + postprocesador) con el que publica
 * ahora el relay (JSON guardado en outbox_events y reenviado tal cual).
 */
class OutboxContractTest {

    static EventEnvelope<InfractionConfirmedPayload> sobreReal() {
        InfractionConfirmedPayload data = new InfractionConfirmedPayload(UUID.randomUUID(), "30111222", DebtorIdType.DNI,
                "AB123CD", InfractionType.SPEEDING, OffsetDateTime.parse("2026-09-20T10:15:00-03:00"),
                new BigDecimal("15000.00"), List.of("REINCIDENCIA"), new BigDecimal("22500.50"), "u-1001",
                new Location("Av. Corrientes 1234", "Uruguay", -34.6037, -58.3816));
        return EventEnvelope.of("infractionConfirmed", data, UUID.randomUUID(), UUID.randomUUID());
    }

    /** Lo que hacía RabbitMqEventPublisher antes del outbox. */
    static Message mensajeAnterior(EventEnvelope<?> envelope, String userId) {
        Message message = new JacksonJsonMessageConverter().toMessage(envelope, new MessageProperties());
        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        message.getMessageProperties().setUserId(userId);
        return message;
    }

    @Test
    @DisplayName("TC-BE-OUT-01: el mensaje publicado por el relay es idéntico al publicado antes (cuerpo y propiedades)")
    void contratoIdentico() {
        EventEnvelope<InfractionConfirmedPayload> envelope = sobreReal();
        OutboxEventRepository repo = mock(OutboxEventRepository.class);
        new OutboxEventPublisher(repo).publish(envelope);
        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repo).save(saved.capture());
        OutboxEvent row = saved.getValue();

        RabbitTemplate template = mock(RabbitTemplate.class);
        new RabbitMqEventPublisher(template, "muni.inbox", "module_transito").send(row);
        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(template).send(eq("muni.inbox"), eq(""), sent.capture());

        Message antes = mensajeAnterior(envelope, "module_transito");
        Message ahora = sent.getValue();
        assertThat(new String(ahora.getBody(), StandardCharsets.UTF_8))
                .isEqualTo(new String(antes.getBody(), StandardCharsets.UTF_8));
        MessageProperties a = antes.getMessageProperties();
        MessageProperties b = ahora.getMessageProperties();
        assertThat(b.getContentType()).isEqualTo(a.getContentType());
        assertThat(b.getContentEncoding()).isEqualTo(a.getContentEncoding());
        assertThat(b.getContentLength()).isEqualTo(a.getContentLength());
        assertThat(b.getHeaders()).isEqualTo(a.getHeaders());
        assertThat(b.getDeliveryMode()).isEqualTo(a.getDeliveryMode());
        assertThat(b.getUserId()).isEqualTo(a.getUserId());
        assertThat(b.getMessageId()).isEqualTo(a.getMessageId());
    }

    @Test
    @DisplayName("TC-BE-OUT-02: la fila del outbox guarda los mismos ids del sobre y queda PENDING")
    void filaConMetadatosDelSobre() {
        EventEnvelope<InfractionConfirmedPayload> envelope = sobreReal();
        OutboxEventRepository repo = mock(OutboxEventRepository.class);
        new OutboxEventPublisher(repo).publish(envelope);
        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repo).save(saved.capture());
        OutboxEvent row = saved.getValue();
        assertThat(row.getEventId()).isEqualTo(envelope.getEventId());
        assertThat(row.getEventType()).isEqualTo("infractionConfirmed");
        assertThat(row.getCorrelationId()).isEqualTo(envelope.getCorrelationId());
        assertThat(row.getCausationId()).isEqualTo(envelope.getCausationId());
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(row.getAttempts()).isZero();
        assertThat(row.getPayload()).contains("\"eventId\":\"" + envelope.getEventId() + "\"",
                "\"eventVersion\":\"1.0\"", "\"sourceModule\":\"transito\"", "\"finalAmount\":22500.50");
        assertThat(row.getNextAttemptAt()).isEqualTo(row.getCreatedAt());
    }

    @Test
    @DisplayName("TC-BE-OUT-03: los eventos internos de Tránsito no se envían a Core")
    void eventoInternoNoSeEnvia() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        OutboxEvent interno = OutboxEvent.builder().eventId(UUID.randomUUID()).eventType("infractionRegistered")
                .payload("{}").build();
        new RabbitMqEventPublisher(template, "muni.inbox", "module_transito").send(interno);
        verify(template, never()).send(any(String.class), any(String.class), any(Message.class));
    }

    @Test
    @DisplayName("TC-BE-OUT-04: el publicador de log (sin broker) también sirve de transporte del relay")
    void loggingTransport() {
        OutboxEvent row = OutboxEvent.builder().eventId(UUID.randomUUID()).eventType("x").payload("{}").build();
        new LoggingEventPublisher().send(row);
        assertThat(CoreMessageFormat.toJson(EventEnvelope.of("x", null))).contains("\"eventType\":\"x\"");
    }
}
