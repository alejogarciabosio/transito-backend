package ar.edu.uade.transito.transito_backend.event.payload;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InfractionAnulledPayload(
        UUID infractionId,
        String annulmentReason,
        String authorizedBy,
        OffsetDateTime annulmentDate) {
}
