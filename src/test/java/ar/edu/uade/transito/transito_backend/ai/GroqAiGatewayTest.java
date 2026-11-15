package ar.edu.uade.transito.transito_backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/** Adaptador a Groq contra un servidor HTTP local que simula al proveedor (sin red externa). */
@DisplayName("GroqAiGateway")
class GroqAiGatewayTest {

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<Integer> status = new AtomicReference<>(200);
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> lastAuth = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private volatile long delayMs = 0;
    private volatile int failFirst = 0;

    private static final String OK = "{\"choices\":[{\"message\":{\"content\":\"{\\\"infractionType\\\":\\\"SPEEDING\\\"}\"}}],\"usage\":{\"total_tokens\":42}}";

    @BeforeEach
    void start() throws IOException {
        body.set(OK);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat", ex -> {
            int n = calls.incrementAndGet();
            lastAuth.set(ex.getRequestHeaders().getFirst("Authorization"));
            lastBody.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try { Thread.sleep(delayMs); } catch (InterruptedException ignored) { }
            int code = n <= failFirst ? 503 : status.get();
            byte[] out = body.get().getBytes(StandardCharsets.UTF_8);
            try {
                ex.sendResponseHeaders(code, out.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(out); }
            } catch (IOException ignored) { /* cliente cortó por timeout */ }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private GroqAiGateway gateway(long timeoutMs) {
        AiProperties props = new AiProperties(true, "secret-test-key", "test-model", timeoutMs,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/chat", 3, 1000);
        return new GroqAiGateway(props);
    }

    @Test
    @DisplayName("éxito: arma el pedido OpenAI-compatible y lee contenido y tokens")
    void success() {
        var result = gateway(2000).complete("SYS", "texto");
        assertThat(result.content()).contains("SPEEDING");
        assertThat(result.totalTokens()).isEqualTo(42);
        assertThat(result.toString()).doesNotContain("SPEEDING");
        assertThat(lastAuth.get()).isEqualTo("Bearer secret-test-key");
        assertThat(lastBody.get()).contains("\"temperature\":0", "json_object", "test-model", "<DATO>", "texto");
    }

    @Test
    @DisplayName("un 503 se reintenta una sola vez y luego anda")
    void retriesOnce() {
        failFirst = 1;
        assertThat(gateway(2000).complete("S", "t").content()).contains("SPEEDING");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("503 persistente: máximo 2 llamadas y falla reintentable")
    void failsAfterOneRetry() {
        status.set(503);
        assertThatThrownBy(() -> gateway(2000).complete("S", "t")).isInstanceOf(AiGatewayException.class)
                .hasMessageContaining("503");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("un 400/401 no se reintenta")
    void noRetryOnClientError() {
        status.set(401);
        assertThatThrownBy(() -> gateway(2000).complete("S", "t")).isInstanceOf(AiGatewayException.class);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("timeout: corta dentro del presupuesto y no reintenta")
    void timeout() {
        delayMs = 1500;
        long t0 = System.nanoTime();
        assertThatThrownBy(() -> gateway(300).complete("S", "t")).isInstanceOf(AiGatewayException.class)
                .hasMessageContaining("Timeout");
        assertThat((System.nanoTime() - t0) / 1_000_000).isLessThan(1200);
    }

    @Test
    @DisplayName("respuesta del proveedor ilegible o sin contenido")
    void unreadableResponses() {
        body.set("no es json");
        assertThatThrownBy(() -> gateway(2000).complete("S", "t")).isInstanceOf(AiGatewayException.class)
                .hasMessageContaining("ilegible");
        body.set("{\"choices\":[]}");
        assertThatThrownBy(() -> gateway(2000).complete("S", "t")).isInstanceOf(AiGatewayException.class)
                .hasMessageContaining("sin contenido");
    }

    @Test
    @DisplayName("sin tokens informados devuelve -1; proveedor caído da error reintentable de red")
    void noUsageAndConnectionRefused() {
        body.set("{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}");
        assertThat(gateway(2000).complete("S", "t").totalTokens()).isEqualTo(-1);
        GroqAiGateway down = gateway(500);
        server.stop(0);
        assertThatThrownBy(() -> down.complete("S", "t")).isInstanceOf(AiGatewayException.class);
    }

    @Test
    @DisplayName("AiProperties: activa solo con flag y clave; toString sin clave")
    void properties() {
        assertThat(new AiProperties(true, "k", "m", 1, "u", 1, 1).isActive()).isTrue();
        assertThat(new AiProperties(true, "  ", "m", 1, "u", 1, 1).isActive()).isFalse();
        assertThat(new AiProperties(false, "k", "m", 1, "u", 1, 1).isActive()).isFalse();
        assertThat(new AiProperties(true, null, "m", 1, "u", 1, 1).isActive()).isFalse();
        assertThat(new AiProperties(true, "supersecret", "m", 1, "u", 1, 1).toString()).doesNotContain("supersecret");
    }
}
