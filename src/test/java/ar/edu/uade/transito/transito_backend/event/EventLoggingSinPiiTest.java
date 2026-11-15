package ar.edu.uade.transito.transito_backend.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** DEF-05: al publicar eventos solo se loguean metadatos del sobre, nunca el payload. */
@ExtendWith(OutputCaptureExtension.class)
class EventLoggingSinPiiTest {

    static final String DNI = "30111222";
    static final String PATENTE = "AB123CD";
    static final String INSPECTOR = "Inspectora Pérez";

    static EventEnvelope<Map<String, Object>> eventoConDatosPersonales(String type) {
        return EventEnvelope.of(type,
                Map.of("debtorId", DNI, "ownerNationalId", DNI, "licensePlate", PATENTE, "inspectorName", INSPECTOR),
                UUID.randomUUID(), null);
    }

    @Test
    void loggingPublisherSoloLogueaMetadatos(CapturedOutput output) {
        EventEnvelope<Map<String, Object>> event = eventoConDatosPersonales("infractionConfirmed");

        new LoggingEventPublisher().publish(event);

        assertThat(output.getAll())
                .contains("[event-published]", "infractionConfirmed", event.getEventId().toString(),
                        event.getCorrelationId().toString())
                .doesNotContain(DNI, PATENTE, INSPECTOR);
    }

    @Test
    void rabbitPublisherSoloLogueaMetadatos(CapturedOutput output) {
        RabbitTemplate template = mock(RabbitTemplate.class);
        EventEnvelope<Map<String, Object>> event = eventoConDatosPersonales("infractionConfirmed");

        new RabbitMqEventPublisher(template, "muni.inbox", "module_transito").publish(event);

        verify(template).convertAndSend(eq("muni.inbox"), anyString(), eq(event), any(MessagePostProcessor.class));
        assertThat(output.getAll())
                .contains("[event-published] type=infractionConfirmed eventId=" + event.getEventId()
                        + " correlationId=" + event.getCorrelationId())
                .doesNotContain(DNI, PATENTE, INSPECTOR);
    }
}
