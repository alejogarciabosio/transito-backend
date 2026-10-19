package ar.edu.uade.transito.transito_backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        String username,
        String dni,
        @NotBlank(message = "La contraseña es obligatoria") String password) {
}
