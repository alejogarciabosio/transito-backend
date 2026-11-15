package ar.edu.uade.transito.transito_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.sun.net.httpserver.HttpServer;

import ar.edu.uade.transito.transito_backend.auth.IdentityValidator;
import ar.edu.uade.transito.transito_backend.auth.RestIdentityValidator;
import ar.edu.uade.transito.transito_backend.auth.StubIdentityValidator;
import tools.jackson.databind.ObjectMapper;

/**
 * Con {@code users.rest.enabled=true} el contexto completo debe arrancar (antes fallaba por pedir el
 * ObjectMapper de Jackson 2) y el login debe resolverse contra M1 simulado.
 */
@SpringBootTest(properties = "users.rest.enabled=true")
@AutoConfigureMockMvc
@DisplayName("Contexto con users.rest.enabled=true")
class RestIdentityContextTest {

    private static final String ACTOR = "11111111-1111-4111-8111-111111111111";
    private static final String CITIZEN = "22222222-2222-4222-8222-222222222222";

    private static HttpServer m1;
    private static final AtomicInteger STATUS = new AtomicInteger(200);
    private static final java.util.concurrent.atomic.AtomicLong DELAY = new java.util.concurrent.atomic.AtomicLong(0);
    private static final AtomicReference<String> BODY = new AtomicReference<>("{}");

    @BeforeAll
    static void startM1() throws IOException {
        m1 = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        m1.createContext("/auth/usuario/login", exchange -> {
            exchange.getRequestBody().readAllBytes();
            try {
                Thread.sleep(DELAY.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = BODY.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(STATUS.get(), bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        m1.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        m1.start();
    }

    @AfterAll
    static void stopM1() {
        m1.stop(0);
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) throws IOException {
        // El puerto se necesita antes de que arranque el contexto.
        if (m1 == null) {
            startM1();
        }
        registry.add("users.rest.url", () -> "http://127.0.0.1:" + m1.getAddress().getPort() + "/auth/usuario/login");
        registry.add("users.rest.timeout-ms", () -> "500");
    }

    @Autowired private ApplicationContext context;
    @Autowired private MockMvc mockMvc;

    private void m1Responds(int status, String body) {
        STATUS.set(status);
        BODY.set(body);
    }

    private org.springframework.test.web.servlet.ResultActions login() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"gmail\":\"ciudadano.mock@example.com\",\"password\":\"x\"}"));
    }

    @Test
    @DisplayName("el contexto arranca y el validador activo es RestIdentityValidator")
    void beanExists() {
        assertThat(context.getBean(IdentityValidator.class)).isInstanceOf(RestIdentityValidator.class);
        assertThat(context.getBeansOfType(StubIdentityValidator.class)).isEmpty();
    }

    @Test
    @DisplayName("200 con auth=true → token para el usuario")
    void loginOk() throws Exception {
        m1Responds(200, "{\"auth\":true,\"actorId\":\"" + ACTOR + "\",\"actorType\":\"CIUDADANO\",\"citizenId\":\""
                + CITIZEN + "\",\"dni\":\"30777888\",\"role\":\"CIUDADANO\"}");
        login().andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CIUDADANO"));
    }

    @Test
    @DisplayName("401 de M1 → 401 credenciales inválidas")
    void loginInvalid() throws Exception {
        m1Responds(401, "{}");
        login().andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("200 con auth=false → sin token (implementación actual: 502)")
    void loginAuthFalse() throws Exception {
        m1Responds(200, "{\"auth\":false}");
        login().andExpect(status().isBadGateway());
    }

    @Test
    @DisplayName("M1 responde 500 → 502 controlado")
    void loginUpstreamError() throws Exception {
        m1Responds(500, "{}");
        login().andExpect(status().isBadGateway());
    }

    @Test
    @DisplayName("M1 sin conexión → 503 controlado")
    void loginConnectionRefused() throws Exception {
        RestIdentityValidator down = new RestIdentityValidator("http://127.0.0.1:1/auth/usuario/login", 500,
                context.getBean(ObjectMapper.class));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> down.validate("a", "b"))
                .isInstanceOf(ar.edu.uade.transito.transito_backend.exception.UsersServiceUnavailableException.class);
    }

    @Test
    @DisplayName("M1 lento (timeout) → 503 controlado")
    void loginTimeout() throws Exception {
        m1Responds(200, "{}");
        DELAY.set(1500);
        try {
            login().andExpect(status().isServiceUnavailable());
        } finally {
            DELAY.set(0);
        }
    }

    @Test
    @DisplayName("sin definir o en false se usa el validador local (producción no cambia)")
    void localValidatorByDefault() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withUserConfiguration(StubIdentityValidator.class, RestIdentityValidator.class)
                .withPropertyValues("users.rest.url=http://127.0.0.1:1/x");
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(StubIdentityValidator.class);
            assertThat(ctx).doesNotHaveBean(RestIdentityValidator.class);
        });
        runner.withPropertyValues("users.rest.enabled=false").run(ctx -> {
            assertThat(ctx).hasSingleBean(StubIdentityValidator.class);
            assertThat(ctx).doesNotHaveBean(RestIdentityValidator.class);
        });
        runner.withPropertyValues("users.rest.enabled=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(RestIdentityValidator.class);
            assertThat(ctx).doesNotHaveBean(StubIdentityValidator.class);
        });
    }
}
