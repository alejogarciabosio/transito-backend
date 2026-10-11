package ar.edu.uade.transito.transito_backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectClosureRequest(
        @NotBlank(message = "El motivo de rechazo es obligatorio")
        @Size(max = 1000, message = "El motivo de rechazo no puede superar 1000 caracteres")
        String rejectionReason) {
}
