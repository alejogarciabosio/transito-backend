package ar.edu.uade.transito.transito_backend.event.payload;

import java.util.UUID;

public record StreetClosureRejectedPayload(
        UUID streetClosureId,
        String reason) {
}
