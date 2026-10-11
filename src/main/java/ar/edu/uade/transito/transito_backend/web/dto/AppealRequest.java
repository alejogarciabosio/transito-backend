package ar.edu.uade.transito.transito_backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppealRequest(
        @NotBlank(message = "El motivo del descargo es obligatorio")
        @Size(max = 2000, message = "El motivo del descargo no puede superar 2000 caracteres")
        String reason) {
}
