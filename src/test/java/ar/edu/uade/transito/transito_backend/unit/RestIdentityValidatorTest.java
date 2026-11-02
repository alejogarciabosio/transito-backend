package ar.edu.uade.transito.transito_backend.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.RestIdentityValidator;
import ar.edu.uade.transito.transito_backend.exception.InvalidCredentialsException;
import ar.edu.uade.transito.transito_backend.exception.UsersAccountForbiddenException;
import ar.edu.uade.transito.transito_backend.exception.UsersServiceBadGatewayException;
import ar.edu.uade.transito.transito_backend.exception.UsersServiceUnavailableException;
import ar.edu.uade.transito.transito_backend.model.enums.Role;

/** Cliente REST de M1 (Usuarios) contra un servidor HTTP local que simula sus respuestas. */
@DisplayName("RestIdentityValidator (login vía M1)")
class RestIdentityValidatorTest {

    private static final String ACTOR = "11111111-1111-4111-8111-111111111111";
    private static final String CITIZEN = "22222222-2222-4222-8222-222222222222";

    private HttpServer server;
    private final AtomicReference<Integer> status = new AtomicReference<>(200);
    private final AtomicReference<String> body = new AtomicReference<>("{}");
    private final AtomicReference<Long> delayMs = new AtomicReference<>(0L);
    private final AtomicReference<String> received = new AtomicReference<>();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/auth/usuario/login", exchange -> {
            received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(delayMs.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private RestIdentityValidator validator(long timeoutMs) {
        return new RestIdentityValidator("http://127.0.0.1:" + server.getAddress().getPort() + "/auth/usuario/login",
                timeoutMs, new ObjectMapper());
    }

    private void respond(int code, String json) {
        status.set(code);
        body.set(json);
    }

    @Test
    @DisplayName("ciudadano válido: devuelve rol, DNI y envía gmail y contraseña a M1")
    void citizenOk() {
        respond(200, "{\"auth\":true,\"actorId\":\"" + ACTOR + "\",\"actorType\":\"CIUDADANO\",\"citizenId\":\"" + CITIZEN
                + "\",\"dni\":\"30777888\",\"role\":\"CIUDADANO\"}");
        AuthenticatedUser user = validator(2000).validate("vecino@gmail.com", "secreta");
        assertThat(user.role()).isEqualTo(Role.CIUDADANO);
        assertThat(user.nationalId()).isEqualTo("30777888");
        assertThat(user.userId()).isEqualTo("CIUDADANO:" + ACTOR);
        assertThat(received.get()).contains("\"gmail\":\"vecino@gmail.com\"").contains("\"password\":\"secreta\"");
    }

    @ParameterizedTest
    @CsvSource({ "MESA_ENTRADAS,INSPECTOR", "RESPONSABLE_AREA,INSPECTOR", "ADMINISTRATIVO,ADMINISTRADOR", "AUDITOR,AUDITOR" })
    @DisplayName("empleado: los roles de M1 se traducen a los roles de Tránsito")
    void employeeRoles(String m1Role, Role expected) {
        respond(200, "{\"auth\":true,\"actorId\":\"" + ACTOR + "\",\"actorType\":\"EMPLEADO\",\"role\":\"" + m1Role + "\"}");
        AuthenticatedUser user = validator(2000).validate("empleado@muni.gob.ar", "x");
        assertThat(user.role()).isEqualTo(expected);
        assertThat(user.nationalId()).isNull();
    }

    @Test
    @DisplayName("401 de M1 → credenciales inválidas; 403 → cuenta bloqueada; 5xx → bad gateway")
    void errorStatuses() {
        respond(401, "{}");
        assertThatThrownBy(() -> validator(2000).validate("a", "b")).isInstanceOf(InvalidCredentialsException.class);
        respond(403, "{}");
        assertThatThrownBy(() -> validator(2000).validate("a", "b")).isInstanceOf(UsersAccountForbiddenException.class);
        respond(500, "{}");
        assertThatThrownBy(() -> validator(2000).validate("a", "b")).isInstanceOf(UsersServiceBadGatewayException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"auth\":false,\"actorId\":\"" + ACTOR + "\",\"actorType\":\"EMPLEADO\",\"role\":\"AUDITOR\"}",
            "{\"auth\":true,\"actorId\":\"" + ACTOR + "\",\"actorType\":\"PROVEEDOR\",\"role\":\"AUDITOR\"}",
            "{\"auth\":true,\"actorId\":\"no-es-uuid\",\"actorType\":\"EMPLEADO\",\"role\":\"AUDITOR\"}",
            "{\"auth\":true,\"actorId\":\"" + ACTOR + "\",\"actorType\":\"CIUDADANO\",\"role\":\"CIUDADANO\",\"dni\":\"30777888\"}",
            "{\"auth\":true,\"actorId\":\"" + ACTOR + "\",\"actorType\":\"EMPLEADO\",\"citizenId\":\"" + CITIZEN + "\",\"role\":\"AUDITOR\"}",
            "{\"auth\":true,\"actorId\":\"" + ACTOR + "\",\"actorType\":\"CIUDADANO\",\"citizenId\":\"" + CITIZEN + "\",\"dni\":\"0\",\"role\":\"CIUDADANO\"}",
            "{\"auth\":true,\"actorId\":\"" + ACTOR + "\",\"actorType\":\"EMPLEADO\",\"role\":\"JARDINERO\"}",
            "no es json"
    })
    @DisplayName("respuestas inválidas de M1 → bad gateway (502), nunca se emite token")
    void invalidResponses(String json) {
        respond(200, json);
        assertThatThrownBy(() -> validator(2000).validate("a", "b")).isInstanceOf(UsersServiceBadGatewayException.class);
    }

    @Test
    @DisplayName("M1 no responde a tiempo → servicio no disponible (503)")
    void timeout() {
        delayMs.set(1500L);
        respond(200, "{}");
        assertThatThrownBy(() -> validator(300).validate("a", "b")).isInstanceOf(UsersServiceUnavailableException.class);
    }

    @Test
    @DisplayName("M1 caído (conexión rechazada) → servicio no disponible (503)")
    void connectionRefused() {
        int port = server.getAddress().getPort();
        server.stop(0);
        RestIdentityValidator v = new RestIdentityValidator("http://127.0.0.1:" + port + "/auth/usuario/login", 1000, new ObjectMapper());
        assertThatThrownBy(() -> v.validate("a", "b")).isInstanceOf(UsersServiceUnavailableException.class);
        server = null;
        try {
            start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("sin URL configurada no arranca")
    void requiresUrl() {
        assertThatThrownBy(() -> new RestIdentityValidator(" ", 1000, new ObjectMapper())).isInstanceOf(IllegalStateException.class);
    }
}
