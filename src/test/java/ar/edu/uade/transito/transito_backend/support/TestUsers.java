package ar.edu.uade.transito.transito_backend.support;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.model.enums.Role;

/** Mismos usuarios que StubIdentityValidator (modelo de datos de prueba). */
public final class TestUsers {

    private TestUsers() {
    }

    public static final AuthenticatedUser INSPECTOR = new AuthenticatedUser("u-1001", "30111222", Role.INSPECTOR);
    public static final AuthenticatedUser ADMIN = new AuthenticatedUser("u-1002", "30222333", Role.ADMINISTRADOR);
    // Alias temporales para que los tests de permisos expresen las capacidades que se consolidaron.
    public static final AuthenticatedUser SUPERVISOR = ADMIN;
    public static final AuthenticatedUser AUDITOR = ADMIN;
    public static final AuthenticatedUser OPERADOR = INSPECTOR;
    public static final AuthenticatedUser DEPOSITO = ADMIN;
    public static final AuthenticatedUser CIUDADANO = new AuthenticatedUser("u-1007", "30777888", Role.CIUDADANO);
    /** Otro ciudadano (no existe en el stub, pero el token es válido) para probar acceso a datos ajenos. */
    public static final AuthenticatedUser OTRO_CIUDADANO = new AuthenticatedUser("u-9999", "99999999", Role.CIUDADANO);

    public static AuthenticatedUser of(Role role) {
        return switch (role) {
            case ADMINISTRADOR -> ADMIN;
            case INSPECTOR -> INSPECTOR;
            case CIUDADANO -> CIUDADANO;
        };
    }
}
