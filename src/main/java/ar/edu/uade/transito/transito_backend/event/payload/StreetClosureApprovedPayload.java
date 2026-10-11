package ar.edu.uade.transito.transito_backend.event.payload;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.Detour;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;

public record StreetClosureApprovedPayload(
        UUID streetClosureId,
        String closureRequestId,
        RequestingModule requestingModule,
        LocalDate startDate,
        LocalDate endDate,
        List<String> affectedStreet,
        List<Detour> detours,
        List<String> conditions) {
}
