package ar.edu.uade.transito.transito_backend.web.dto;

import ar.edu.uade.transito.transito_backend.model.enums.Role;

public record LoginResponse(String token, Role role, long expiresInSeconds) {
}
