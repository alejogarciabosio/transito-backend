package ar.edu.uade.transito.transito_backend.listener.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record StreetClosureRequestedPayload(
        String closureRequestId,
        String previousClosureRequestId,
        String sourceModule,
        String sourceRef,
        String reason,
        List<AffectedSectionPayload> affectedSections,
        OffsetDateTime requestedFrom,
        OffsetDateTime requestedTo,
        String closureType,
        OffsetDateTime requestedAt) {
}
