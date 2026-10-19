package ar.edu.uade.transito.transito_backend.event.payload;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.Detour;
import ar.edu.uade.transito.transito_backend.model.StreetAffectedSection;
public record StreetClosureApprovedPayload(
        UUID streetClosureId,
        String closureRequestId,
        String requestingModule,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        List<StreetAffectedSection> affectedSections,
        List<Detour> detours,
        List<String> conditions) {
}
