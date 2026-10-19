package ar.edu.uade.transito.transito_backend.unit;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.LoggingEventPublisher;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Infraestructura de eventos")
class EventInfrastructureTest {

    @Test
    @DisplayName("TC-BE-EVT-04: EventEnvelope.of arma el sobre estándar (eventId, eventType, occurredAt, sourceModule=transito, data)")
    void envelope() {
        OffsetDateTime before = OffsetDateTime.now();
        EventEnvelope<String> e = EventEnvelope.of("algoPaso", "payload");
        assertThat(e.getEventId()).isNotNull();
        assertThat(e.getEventType()).isEqualTo("algoPaso");
        assertThat(e.getSourceModule()).isEqualTo("transito");
        assertThat(e.getEventVersion()).isEqualTo("1.0");
        assertThat(e.getCorrelationId()).isNotNull();
        assertThat(e.getCausationId()).isNull();
        assertThat(e.getOccurredAt()).isAfterOrEqualTo(before);
        assertThat(e.getData()).isEqualTo("payload");
        assertThat(EventEnvelope.of("x", 1).getEventId()).isNotEqualTo(e.getEventId());
        assertThat(new EventEnvelope<>().getData()).isNull();
    }

    @Test
    @DisplayName("TC-BE-EVT-05: LoggingEventPublisher publica sin lanzar excepciones")
    void loggingPublisher() {
        assertThatCode(() -> new LoggingEventPublisher().publish(EventEnvelope.of("x", null))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TC-BE-EVT-06: RequestingModule usa M3/M6 dentro de data y mantiene alias de envelope")
    void requestingModule() {
        assertThat(RequestingModule.OBRAS.sourceModule()).isEqualTo("M3");
        assertThat(RequestingModule.AMBIENTE.sourceModule()).isEqualTo("M6");
        assertThat(RequestingModule.OBRAS.displayName()).isEqualTo("Obras");
        assertThat(RequestingModule.AMBIENTE.displayName()).isEqualTo("Ambiente");
        assertThat(RequestingModule.fromSourceModule("M3")).isEqualTo(RequestingModule.OBRAS);
        assertThat(RequestingModule.fromSourceModule("M6")).isEqualTo(RequestingModule.AMBIENTE);
        assertThat(RequestingModule.fromSourceModule("obras")).isEqualTo(RequestingModule.OBRAS);
        assertThat(RequestingModule.fromSourceModule("ambiente")).isEqualTo(RequestingModule.AMBIENTE);
        assertThatThrownBy(() -> RequestingModule.fromSourceModule("M9")).isInstanceOf(IllegalArgumentException.class);
    }
}
