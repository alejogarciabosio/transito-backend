package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.OperationResolution;
import ar.edu.uade.transito.transito_backend.model.enums.OperationStatus;
import ar.edu.uade.transito.transito_backend.model.enums.OperationType;

public record TrafficOperationResponse(
        UUID operationId,
        OperationType type,
        String zone,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        List<String> assignedInspectors,
        List<String> assignedUnits,
        String ticketId,
        OperationStatus status,
        OperationResolution resolution) {
}
