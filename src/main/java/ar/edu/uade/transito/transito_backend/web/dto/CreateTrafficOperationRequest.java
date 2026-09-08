package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.LocalDateTime;
import java.util.List;

import ar.edu.uade.transito.transito_backend.model.enums.OperationType;

public record CreateTrafficOperationRequest(
        OperationType type,
        String zone,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        List<String> assignedInspectors,
        List<String> assignedUnits,
        String ticketId) {
}
