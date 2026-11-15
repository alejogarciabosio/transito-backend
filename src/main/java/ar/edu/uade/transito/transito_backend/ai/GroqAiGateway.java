package ar.edu.uade.transito.transito_backend.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptador a Groq (API compatible con OpenAI: POST /openai/v1/chat/completions).
 * Presupuesto total de {@code ai.timeout-ms} (5 s por defecto) repartido entre el intento y, como máximo, un reintento.
 */
@Component
public class GroqAiGateway implements AiGateway {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final HttpClient httpClient;
    private final AiProperties props;

    @org.springframework.beans.factory.annotation.Autowired
    public GroqAiGateway(AiProperties props) {
        this(props, HttpClient.newBuilder().connectTimeout(Duration.ofMillis(props.timeoutMs())).build());
    }

    GroqAiGateway(AiProperties props, HttpClient httpClient) {
        this.props = props;
        this.httpClient = httpClient;
    }

    @Override
    public AiCompletion complete(String systemPrompt, String userData) {
        String body = MAPPER.writeValueAsString(Map.of(
                "model", props.model(),
                "temperature", 0,
                "max_tokens", 300,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "<DATO>\n" + userData + "\n</DATO>"))));
        long deadline = System.nanoTime() + Duration.ofMillis(props.timeoutMs()).toNanos();
        AiGatewayException last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            long remainingMs = (deadline - System.nanoTime()) / 1_000_000;
            if (remainingMs <= 0) {
                throw last != null ? last : new AiGatewayException("Timeout del proveedor de IA", false);
            }
            try {
                return attemptOnce(body, remainingMs);
            } catch (AiGatewayException ex) {
                last = ex;
                if (!ex.isRetryable()) {
                    throw ex;
                }
            }
        }
        throw last;
    }

    private AiCompletion attemptOnce(String body, long timeoutMs) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(props.url()))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + props.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException ex) {
            throw new AiGatewayException("Timeout del proveedor de IA", false, ex);
        } catch (IOException ex) {
            throw new AiGatewayException("Error de red hacia el proveedor de IA", true, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiGatewayException("Llamada a IA interrumpida", false, ex);
        }
        int status = response.statusCode();
        if (status == 429 || status >= 500) {
            throw new AiGatewayException("El proveedor de IA respondió HTTP " + status, true);
        }
        if (status < 200 || status >= 300) {
            throw new AiGatewayException("El proveedor de IA respondió HTTP " + status, false);
        }
        try {
            JsonNode root = MAPPER.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isString()) {
                throw new AiGatewayException("Respuesta del proveedor sin contenido", false);
            }
            return new AiCompletion(content.asString(), root.path("usage").path("total_tokens").asInt(-1));
        } catch (AiGatewayException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AiGatewayException("Respuesta ilegible del proveedor de IA", false);
        }
    }
}
