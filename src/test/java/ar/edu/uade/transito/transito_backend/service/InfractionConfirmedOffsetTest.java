package ar.edu.uade.transito.transito_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.Location;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;

/** Contrato v4: las fechas del evento infractionConfirmed van en ISO 8601 con offset (-03:00). */
class InfractionConfirmedOffsetTest {

    @Test
    @DisplayName("infractionConfirmed publica infractionDateTime con offset -03:00 (mismo conversor que RabbitMQ)")
    void publishesInfractionDateTimeWithArgentinaOffset() {
        InfractionRepository repository = mock(InfractionRepository.class);
        EventPublisher publisher = mock(EventPublisher.class);
        UUID id = UUID.randomUUID();
        Infraction infraction = Infraction.builder()
                .infractionId(id)
                .licensePlate("AB123CD")
                .infractionType(InfractionType.values()[0])
                .infractionDateTime(LocalDateTime.of(2026, 11, 15, 14, 14, 29))
                .location(new Location("Av. Corrientes", null, null, null))
                .baseAmount(new BigDecimal("50000"))
                .finalAmount(new BigDecimal("50000"))
                .inspectorId("u-1001")
                .status(InfractionStatus.PENDING_APPEAL)
                .build();
        when(repository.findById(id)).thenReturn(Optional.of(infraction));
        when(repository.save(any(Infraction.class))).thenAnswer(inv -> inv.getArgument(0));

        new InfractionService(repository, publisher).confirm(id);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass((Class) EventEnvelope.class);
        verify(publisher).publish(captor.capture());
        String json = new String(new JacksonJsonMessageConverter()
                .toMessage(captor.getValue(), new MessageProperties()).getBody(), StandardCharsets.UTF_8);

        assertThat(json).contains("\"infractionDateTime\":\"2026-11-15T14:14:29-03:00\"");
    }

    @Test
    @DisplayName("toMunicipalOffset tolera null")
    void nullStaysNull() {
        assertThat(InfractionService.toMunicipalOffset(null)).isNull();
    }
}
