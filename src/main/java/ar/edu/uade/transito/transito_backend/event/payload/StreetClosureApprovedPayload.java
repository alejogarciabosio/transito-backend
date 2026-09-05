package ar.edu.uade.transito.transito_backend.event.payload;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.Detour;

public record StreetClosureApprovedPayload(
        UUID streetClosureId,
        LocalDate startDate,
        LocalDate endDate,
        List<Detour> detours,
        List<String> conditions) {
}
