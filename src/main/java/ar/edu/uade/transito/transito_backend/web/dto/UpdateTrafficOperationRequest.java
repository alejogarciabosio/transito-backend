package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTrafficOperationRequest(
        @Size(min = 1, max = 200, message = "La zona debe tener entre 1 y 200 caracteres") String zone,
        LocalDateTime endDateTime,
        List<@NotBlank(message = "Los identificadores de inspector no pueden estar vacíos") @Size(max = 255, message = "No puede superar 255 caracteres") String> assignedInspectors,
        List<@NotBlank(message = "Los identificadores de móvil no pueden estar vacíos") @Size(max = 255, message = "No puede superar 255 caracteres") String> assignedUnits) {
}
