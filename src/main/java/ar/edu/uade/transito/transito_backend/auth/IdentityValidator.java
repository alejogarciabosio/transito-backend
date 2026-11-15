package ar.edu.uade.transito.transito_backend.auth;

import java.util.Optional;

/**
 * Valida credenciales contra la fuente de verdad de identidad. Las contraseñas
 * no las maneja Tránsito (viven en el módulo de Usuarios), por eso esta pieza
 * queda detrás de una interfaz: hoy la implementa un stub local, y el día que
 * el equipo cierre el contrato con Usuarios, se reemplaza por un cliente REST
 * sin tocar el resto del flujo de login.
 */
public interface IdentityValidator {

    AuthenticatedUser validate(String username, String password);

    /**
     * DNI del usuario identificado por el subject del JWT, si la fuente de identidad
     * puede resolverlo sin credenciales (DEF-06). Por defecto no puede: el DNI se
     * conoce solo al validar el login y queda en {@link NationalIdRegistry}.
     */
    default Optional<String> findNationalId(String subject) {
        return Optional.empty();
    }
}
