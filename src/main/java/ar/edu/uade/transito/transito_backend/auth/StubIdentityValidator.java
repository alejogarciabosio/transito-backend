package ar.edu.uade.transito.transito_backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Component;

import ar.edu.uade.transito.transito_backend.exception.InvalidCredentialsException;
import ar.edu.uade.transito.transito_backend.model.enums.Role;

// Validador local de usuarios de prueba. En integración se activa RestIdentityValidator.
// Las contraseñas NO están en el código: cada una sale de la propiedad stub.password.<usuario>,
// que Spring resuelve desde la variable de entorno STUB_PASSWORD_<USUARIO> (sin valor por defecto).
// Si falta o está vacía, ese usuario queda deshabilitado (login 401) y se avisa al arrancar.
@Component
@ConditionalOnProperty(name = "users.rest.enabled", havingValue = "false", matchIfMissing = true)
public class StubIdentityValidator implements IdentityValidator {

    private static final Logger log = LoggerFactory.getLogger(StubIdentityValidator.class);

    private record StubUser(String username, String userId, String nationalId, Role role) {
    }

    private static final List<StubUser> USERS = List.of(
            new StubUser("inspector1", "u-1001", "30111222", Role.INSPECTOR),
            new StubUser("admin1", "u-1002", "30222333", Role.ADMINISTRADOR),
            new StubUser("ciudadano1", "u-1007", "30777888", Role.CIUDADANO));

    private final Map<String, StubUser> enabledUsers = new HashMap<>();
    private final Map<String, byte[]> passwords = new HashMap<>();

    public StubIdentityValidator(PropertyResolver properties) {
        for (StubUser user : USERS) {
            String password = properties.getProperty(propertyName(user.username()));
            if (password == null || password.isEmpty()) {
                log.warn("Usuario de prueba '{}' deshabilitado: falta la variable de entorno {}",
                        user.username(), envVarName(user.username()));
                continue;
            }
            enabledUsers.put(user.username(), user);
            passwords.put(user.username(), password.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Propiedad de la que se lee la contraseña (p. ej. stub.password.inspector1). */
    static String propertyName(String username) {
        return "stub.password." + username;
    }

    /** Variable de entorno equivalente (p. ej. STUB_PASSWORD_INSPECTOR1). */
    static String envVarName(String username) {
        return "STUB_PASSWORD_" + username.toUpperCase(Locale.ROOT);
    }

    @Override
    public AuthenticatedUser validate(String username, String password) {
        StubUser user = username == null ? null : enabledUsers.get(username);
        if (user == null || password == null
                || !MessageDigest.isEqual(passwords.get(username), password.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidCredentialsException("Usuario o contraseña inválidos");
        }
        return new AuthenticatedUser(user.userId(), user.nationalId(), user.role());
    }

    @Override
    public Optional<String> findNationalId(String subject) {
        return USERS.stream()
                .filter(u -> u.userId().equals(subject))
                .map(StubUser::nationalId)
                .findFirst();
    }
}
