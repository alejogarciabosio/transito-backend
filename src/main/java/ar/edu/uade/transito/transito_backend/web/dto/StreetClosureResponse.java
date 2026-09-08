package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.Detour;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;

public record StreetClosureResponse(
        UUID streetClosureId,
        String closureRequestId,
        RequestingModule requestingModule,
        LocalDate startDate,
        LocalDate endDate,
        String affectedStreet,
        List<Detour> detours,
        List<String> conditions,
        StreetClosureStatus status) {
}
