package ar.edu.uade.transito.transito_backend.event;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

/** Declares every event emitted by Tránsito in Core's integration catalogue. */
@Component
@ConditionalOnProperty(name = { "core.rabbitmq.enabled", "core.publications.registration.enabled" }, havingValue = "true")
public class CorePublicationRegistrar {

    private static final Logger log = LoggerFactory.getLogger(CorePublicationRegistrar.class);

    private final CoreTokenClient tokenClient;
    private final ObjectMapper objectMapper;
    private final URI publicationsEndpoint;
    private final HttpClient httpClient;

    @Autowired
    public CorePublicationRegistrar(CoreTokenClient tokenClient, ObjectMapper objectMapper,
            @Value("${core.base-url}") String baseUrl) {
        this(tokenClient, objectMapper, URI.create(baseUrl + "/api/v1/publications"),
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    CorePublicationRegistrar(CoreTokenClient tokenClient, ObjectMapper objectMapper, URI publicationsEndpoint,
            HttpClient httpClient) {
        this.tokenClient = tokenClient;
        this.objectMapper = objectMapper;
        this.publicationsEndpoint = publicationsEndpoint;
        this.httpClient = httpClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerCorePublications() {
        registerAll();
    }

    void registerAll() {
        for (String eventType : CorePublishedEventTypes.ALL) {
            try {
                register(eventType);
            } catch (RuntimeException ex) {
                // A catalogue declaration is documentary; an unavailable Core must not prevent API startup.
                log.warn("No se pudo registrar la publicación {} en Core: {}", eventType, ex.getMessage());
            }
        }
    }

    void register(String eventType) {
        try {
            String body = objectMapper.writeValueAsString(Map.of("eventType", eventType));
            HttpRequest request = HttpRequest.newBuilder(publicationsEndpoint)
                    .timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Bearer " + tokenClient.getToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                // Solo el código HTTP: el mensaje se loguea y el cuerpo de Core no aporta al diagnóstico.
                throw new IllegalStateException("Core respondió " + response.statusCode());
            }
            log.info("Publicación {} registrada en Core", eventType);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo registrar la publicación en Core", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("El registro de publicación en Core fue interrumpido", ex);
        }
    }
}
