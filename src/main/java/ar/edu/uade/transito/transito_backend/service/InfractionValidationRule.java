package ar.edu.uade.transito.transito_backend.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.exception.InvalidRequestException;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.enums.Role;

/**
 * Regla de negocio HU-INF-01 / RF-INF-05 (DEF-16): "Toda infracción necesita evidencia o validación del inspector".
 * Se acepta si (a) trae al menos una URL de evidencia no vacía, o (b) la registra un usuario autenticado con rol
 * INSPECTOR y el inspectorId de la infracción es el suyo. Si falta el inspectorId, se completa con el del inspector.
 */
final class InfractionValidationRule {

    static final String MESSAGE = "La infracción requiere evidencia o la validación de un inspector";

    private InfractionValidationRule() {
    }

    static void verify(Infraction infraction) {
        if (hasEvidence(infraction.getEvidenceUrl())) {
            return;
        }
        AuthenticatedUser user = currentUser();
        if (user != null && user.role() == Role.INSPECTOR && user.userId() != null) {
            if (infraction.getInspectorId() == null || infraction.getInspectorId().isBlank()) {
                infraction.setInspectorId(user.userId());
            }
            if (user.userId().equals(infraction.getInspectorId())) {
                return;
            }
        }
        throw new InvalidRequestException(MESSAGE);
    }

    private static boolean hasEvidence(List<String> urls) {
        return urls != null && urls.stream().anyMatch(url -> url != null && !url.isBlank());
    }

    private static AuthenticatedUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AuthenticatedUser user ? user : null;
    }
}
