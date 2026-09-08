package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateTrafficOperationRequest(
        String zone,
        LocalDateTime endDateTime,
        List<String> assignedInspectors,
        List<String> assignedUnits) {
}
