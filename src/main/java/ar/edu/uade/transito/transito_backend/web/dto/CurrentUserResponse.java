package ar.edu.uade.transito.transito_backend.web.dto;

import ar.edu.uade.transito.transito_backend.model.enums.Role;

/** Datos del usuario autenticado (solo el propio). El DNI sale del servidor, no del JWT (DEF-06). */
public record CurrentUserResponse(String userId, Role role, String nationalId, String actorType, String citizenId) {
}
