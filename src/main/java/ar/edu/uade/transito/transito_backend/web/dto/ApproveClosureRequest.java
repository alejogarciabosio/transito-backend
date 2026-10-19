package ar.edu.uade.transito.transito_backend.web.dto;

import java.util.List;
import java.time.OffsetDateTime;

import ar.edu.uade.transito.transito_backend.model.Detour;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApproveClosureRequest(
        @NotNull(message = "La lista de desvíos es obligatoria") List<@Valid Detour> detours,
        @NotNull(message = "La lista de condiciones es obligatoria") List<@NotBlank(message = "Las condiciones no pueden estar vacías") String> conditions,
        OffsetDateTime authorizedFrom,
        OffsetDateTime authorizedTo) {
}
