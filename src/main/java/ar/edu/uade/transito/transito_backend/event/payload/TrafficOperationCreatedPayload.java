package ar.edu.uade.transito.transito_backend.event.payload;

import java.time.LocalDateTime;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.OperationType;

public record TrafficOperationCreatedPayload(
        UUID operationId,
        OperationType type,
        String zone,
        LocalDateTime startDateTime,
        String ticketId) {
}
