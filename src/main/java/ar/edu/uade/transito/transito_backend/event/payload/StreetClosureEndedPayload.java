package ar.edu.uade.transito.transito_backend.event.payload;

import java.time.LocalDateTime;
import java.util.UUID;

public record StreetClosureEndedPayload(
        UUID streetClosureId,
        String closureRequestId,
        LocalDateTime completionDateTime,
        String notes) {
}
