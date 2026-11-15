package ar.edu.uade.transito.transito_backend.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ar.edu.uade.transito.transito_backend.exception.InvalidCredentialsException;
import ar.edu.uade.transito.transito_backend.exception.UsersAccountForbiddenException;
import ar.edu.uade.transito.transito_backend.exception.UsersServiceBadGatewayException;
import ar.edu.uade.transito.transito_backend.exception.UsersServiceUnavailableException;
import ar.edu.uade.transito.transito_backend.model.enums.Role;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Adaptador de autenticación hacia M1. M1 expone REST; M7 puede ofrecer SOAP
 * hacia sus propios consumidores sin trasladar esa obligación a Usuarios.
 */
@Component
@ConditionalOnProperty(name = "users.rest.enabled", havingValue = "true")
public class RestIdentityValidator implements IdentityValidator {

    private static final Set<String> ACTOR_TYPES = Set.of("CIUDADANO", "EMPLEADO");

    private final HttpClient httpClient;
    private final URI endpoint;
    private final Duration timeout;
    private final ObjectMapper objectMapper;

    public RestIdentityValidator(@Value("${users.rest.url}") String url,
            @Value("${users.rest.timeout-ms:5000}") long timeoutMs,
            ObjectMapper objectMapper) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("USERS_REST_URL es obligatorio cuando USERS_REST_ENABLED=true");
        }
        this.timeout = Duration.ofMillis(timeoutMs);
        this.endpoint = URI.create(url);
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    @Override
    public AuthenticatedUser validate(String gmail, String password) {
        try {
            String body = objectMapper.writeValueAsString(Map.of("gmail", gmail, "password", password));
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                throw new InvalidCredentialsException("Usuario o contraseña inválidos");
            }
            if (response.statusCode() == 403) {
                throw new UsersAccountForbiddenException("La cuenta está bloqueada o inactiva");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new UsersServiceBadGatewayException("Usuarios respondió HTTP " + response.statusCode());
            }
            return parseResponse(response.body());
        } catch (InvalidCredentialsException | UsersAccountForbiddenException | UsersServiceBadGatewayException ex) {
            throw ex;
        } catch (java.net.http.HttpTimeoutException | java.net.ConnectException ex) {
            throw new UsersServiceUnavailableException("El servicio de Usuarios no está disponible", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new UsersServiceUnavailableException("La validación con Usuarios fue interrumpida", ex);
        } catch (Exception ex) {
            throw new UsersServiceBadGatewayException("No fue posible procesar la respuesta de Usuarios", ex);
        }
    }

    private AuthenticatedUser parseResponse(String json) throws Exception {
        Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>() {});
        String actorId = text(response, "actorId");
        String actorType = text(response, "actorType");
        String role = text(response, "role");
        if (!Boolean.TRUE.equals(response.get("auth")) || actorId == null || actorType == null || role == null) {
            throw new UsersServiceBadGatewayException("Respuesta de Usuarios sin auth, actorId, actorType o role válidos");
        }
        if (!ACTOR_TYPES.contains(actorType)) {
            throw new UsersServiceBadGatewayException("actorType no válido de Usuarios: " + actorType);
        }

        String citizenId = text(response, "citizenId");
        validateUuid(actorId, "actorId");
        if (citizenId != null) {
            validateUuid(citizenId, "citizenId");
        }
        if ("CIUDADANO".equals(actorType) && citizenId == null) {
            throw new UsersServiceBadGatewayException("Respuesta de Usuarios sin citizenId para un ciudadano");
        }
        if ("EMPLEADO".equals(actorType) && citizenId != null) {
            throw new UsersServiceBadGatewayException("Respuesta de Usuarios con citizenId para un empleado");
        }
        String dni = citizenId == null ? null : validDni(text(response, "dni"));
        return new AuthenticatedUser(actorType + ":" + actorId, dni, mapRole(role), actorType, citizenId);
    }

    private String text(Map<String, Object> response, String key) {
        Object value = response.get(key);
        return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }

    private Role mapRole(String m1Role) {
        return switch (m1Role) {
            case "CIUDADANO" -> Role.CIUDADANO;
            case "MESA_ENTRADAS", "RESPONSABLE_AREA" -> Role.INSPECTOR;
            case "ADMINISTRATIVO" -> Role.ADMINISTRADOR;
            case "AUDITOR" -> Role.AUDITOR;
            default -> throw new UsersServiceBadGatewayException("Rol no reconocido de Usuarios: " + m1Role);
        };
    }

    private void validateUuid(String value, String field) {
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new UsersServiceBadGatewayException("Respuesta de Usuarios con " + field + " inválido", ex);
        }
    }

    private String validDni(String dni) {
        if (dni == null || dni.equals("0") || !dni.matches("\\d{7,8}")) {
            throw new UsersServiceBadGatewayException("Respuesta de Usuarios sin DNI válido para un ciudadano");
        }
        return dni;
    }
}
