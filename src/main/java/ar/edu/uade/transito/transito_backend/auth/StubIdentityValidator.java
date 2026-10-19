package ar.edu.uade.transito.transito_backend.auth;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import ar.edu.uade.transito.transito_backend.exception.InvalidCredentialsException;
import ar.edu.uade.transito.transito_backend.model.enums.Role;

// ATENCIÓN: hay reemplazar por un cliente REST al módulo de Usuarios cuando definan el contrato de validación de identidad.
@Component
@ConditionalOnProperty(name = "users.soap.enabled", havingValue = "false", matchIfMissing = true)
public class StubIdentityValidator implements IdentityValidator {

    private record StubUser(String password, String userId, String nationalId, Role role) {
    }

    private static final Map<String, StubUser> USERS = Map.of(
            "inspector1", new StubUser("inspector123", "u-1001", "30111222", Role.INSPECTOR),
            "admin1", new StubUser("admin123", "u-1002", "30222333", Role.ADMINISTRADOR),
            "ciudadano1", new StubUser("ciudadano123", "u-1007", "30777888", Role.CIUDADANO));

    @Override
    public AuthenticatedUser validate(String username, String password) {
        StubUser user = USERS.get(username);
        if (user == null || !user.password().equals(password)) {
            throw new InvalidCredentialsException("Usuario o contraseña inválidos");
        }
        return new AuthenticatedUser(user.userId(), user.nationalId(), user.role());
    }
}
