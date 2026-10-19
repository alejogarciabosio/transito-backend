package ar.edu.uade.transito.transito_backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnulRequest(
        @NotBlank(message = "El motivo de anulación es obligatorio")
        @Size(max = 1000, message = "El motivo de anulación no puede superar 1000 caracteres")
        String annulmentReason) {
}
