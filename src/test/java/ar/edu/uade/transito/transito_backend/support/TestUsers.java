package ar.edu.uade.transito.transito_backend.support;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.model.enums.Role;

/** Mismos usuarios que StubIdentityValidator (modelo de datos de prueba). */
public final class TestUsers {

    private TestUsers() {
    }

    public static final AuthenticatedUser INSPECTOR = new AuthenticatedUser("u-1001", "30111222", Role.INSPECTOR);
    public static final AuthenticatedUser SUPERVISOR = new AuthenticatedUser("u-1002", "30222333", Role.SUPERVISOR);
    public static final AuthenticatedUser AUDITOR = new AuthenticatedUser("u-1003", "30333444", Role.AUDITOR);
    public static final AuthenticatedUser OPERADOR = new AuthenticatedUser("u-1004", "30444555", Role.OPERADOR_CONTROL);
    public static final AuthenticatedUser DEPOSITO = new AuthenticatedUser("u-1005", "30555666", Role.PERSONAL_DEPOSITO);
    public static final AuthenticatedUser ADMIN = new AuthenticatedUser("u-1006", "30666777", Role.ADMIN_MUNICIPAL);
    public static final AuthenticatedUser CIUDADANO = new AuthenticatedUser("u-1007", "30777888", Role.CIUDADANO);
    /** Otro ciudadano (no existe en el stub, pero el token es válido) para probar acceso a datos ajenos. */
    public static final AuthenticatedUser OTRO_CIUDADANO = new AuthenticatedUser("u-9999", "99999999", Role.CIUDADANO);

    public static AuthenticatedUser of(Role role) {
        return switch (role) {
            case INSPECTOR -> INSPECTOR;
            case SUPERVISOR -> SUPERVISOR;
            case AUDITOR -> AUDITOR;
            case OPERADOR_CONTROL -> OPERADOR;
            case PERSONAL_DEPOSITO -> DEPOSITO;
            case ADMIN_MUNICIPAL -> ADMIN;
            case CIUDADANO -> CIUDADANO;
        };
    }
}
