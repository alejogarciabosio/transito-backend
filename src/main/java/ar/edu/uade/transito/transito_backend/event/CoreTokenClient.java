package ar.edu.uade.transito.transito_backend.event;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Pide el JWT de módulo a Core (POST /api/v1/auth/module-token) usando el secret de máquina.
 * El token dura 15 minutos y no tiene refresh: se cachea en memoria y se renueva solo cuando
 * está vencido o a punto de vencer.
 */
@Component
@ConditionalOnProperty(name = "core.rabbitmq.enabled", havingValue = "true")
public class CoreTokenClient {

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper;
    private final URI tokenEndpoint;
    private final String secret;

    private String cachedToken;
    private Instant cachedTokenExpiresAt = Instant.EPOCH;

    public CoreTokenClient(@Value("${core.base-url}") String baseUrl,
            @Value("${core.module-secret}") String secret,
            ObjectMapper objectMapper) {
        this.tokenEndpoint = URI.create(baseUrl + "/api/v1/auth/module-token");
        this.secret = secret;
        this.objectMapper = objectMapper;
    }

    /** Token vigente; pide uno nuevo a Core si el que tenemos en memoria ya venció. */
    public synchronized String getToken() {
        if (cachedToken == null || Instant.now().isAfter(cachedTokenExpiresAt)) {
            cachedToken = requestNewToken();
        }
        return cachedToken;
    }

    private String requestNewToken() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("CORE_MODULE_SECRET no está configurado");
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of("module", "transito", "secret", secret));
            HttpRequest request = HttpRequest.newBuilder(tokenEndpoint)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Core respondió " + response.statusCode() + " al pedir el token de módulo");
            }
            JsonNode parsed = objectMapper.readTree(response.body());
            if (!parsed.hasNonNull("accessToken")) {
                // No se incluye el cuerpo: podría contener credenciales y el mensaje termina en logs.
                throw new IllegalStateException("Core no devolvió accessToken");
            }
            long expiresInSeconds = parsed.path("expiresIn").asLong(900);
            // Margen de 30s para no quedarnos con un token a punto de vencer.
            this.cachedTokenExpiresAt = Instant.now().plusSeconds(Math.max(expiresInSeconds - 30, 0));
            return parsed.path("accessToken").asText();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo obtener el token de módulo de Core", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La solicitud de token a Core fue interrumpida", ex);
        }
    }
}
