package ar.edu.uade.transito.transito_backend.auth;

import ar.edu.uade.transito.transito_backend.model.enums.Role;

public record AuthenticatedUser(
        String userId,
        String nationalId,
        Role role) {
}
