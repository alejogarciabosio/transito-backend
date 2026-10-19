package ar.edu.uade.transito.transito_backend.event.payload;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StreetClosureEndedPayload(
        UUID streetClosureId,
        String closureRequestId,
        OffsetDateTime completionDateTime,
        String notes) {
}
