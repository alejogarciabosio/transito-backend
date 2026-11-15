package ar.edu.uade.transito.transito_backend.web.dto;

import ar.edu.uade.transito.transito_backend.model.OutboxEvent;
import ar.edu.uade.transito.transito_backend.model.enums.OutboxStatus;

import java.time.Instant;
import java.util.UUID;

/** Fila del registro de eventos (DEF-04). Sin payload: puede tener datos personales. */
public record OutboxEventResponse(
        UUID eventId,
        String eventType,
        UUID correlationId,
        UUID causationId,
        OutboxStatus status,
        int attempts,
        String lastError,
        Instant createdAt,
        Instant publishedAt) {

    public static OutboxEventResponse from(OutboxEvent e) {
        return new OutboxEventResponse(e.getEventId(), e.getEventType(), e.getCorrelationId(), e.getCausationId(),
                e.getStatus(), e.getAttempts(), e.getLastError(), e.getCreatedAt(), e.getPublishedAt());
    }
}
