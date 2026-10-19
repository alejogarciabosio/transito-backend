package ar.edu.uade.transito.transito_backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FinishClosureRequest(
        @NotBlank(message = "Las observaciones de finalización son obligatorias")
        @Size(max = 2000, message = "Las observaciones no pueden superar 2000 caracteres")
        String notes) {
}
