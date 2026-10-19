package ar.edu.uade.transito.transito_backend.web.dto;

import java.util.List;
import java.time.OffsetDateTime;

import ar.edu.uade.transito.transito_backend.model.Detour;

public record ApproveClosureRequest(
        List<Detour> detours,
        List<String> conditions,
        OffsetDateTime authorizedFrom,
        OffsetDateTime authorizedTo) {
}
