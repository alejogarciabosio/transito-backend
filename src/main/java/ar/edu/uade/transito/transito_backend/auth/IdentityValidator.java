package ar.edu.uade.transito.transito_backend.auth;

/**
 * Valida credenciales contra la fuente de verdad de identidad. Las contraseñas
 * no las maneja Tránsito (viven en el módulo de Usuarios), por eso esta pieza
 * queda detrás de una interfaz: hoy la implementa un stub local, y el día que
 * el equipo cierre el contrato con Usuarios, se reemplaza por un cliente REST
 * sin tocar el resto del flujo de login.
 */
public interface IdentityValidator {

    AuthenticatedUser validate(String username, String password);
}
