package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.Detour;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;

public record StreetClosureResponse(
        UUID streetClosureId,
        String closureRequestId,
        RequestingModule requestingModule,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        List<String> affectedStreet,
        List<Detour> detours,
        List<String> conditions,
        StreetClosureStatus status) {
}
