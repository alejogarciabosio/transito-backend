package ar.edu.uade.transito.transito_backend.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Registra el catálogo de M7 ante Core al habilitar la integración. */
@Component
@ConditionalOnProperty(prefix = "core.integration", name = "enabled", havingValue = "true")
public class CoreRegistrationClient {
    private static final Logger log = LoggerFactory.getLogger(CoreRegistrationClient.class);
    private static final List<String> PRODUCED = List.of("streetClosureApproved", "streetClosureRejected",
            "streetClosureActivated", "streetClosureEnded", "roadAccidentRegistered");
    private final RestClient client;
    private final String secret;

    public CoreRegistrationClient(@org.springframework.beans.factory.annotation.Value("${core.base-url}") String baseUrl,
            @org.springframework.beans.factory.annotation.Value("${core.module-secret}") String secret) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.secret = secret;
    }

    @EventListener(ApplicationReadyEvent.class)
    void register() {
        Token token = client.post().uri("/api/v1/auth/module-token")
                .body(new ModuleCredentials("transito", secret)).retrieve().body(Token.class);
        String bearer = "Bearer " + token.accessToken();
        PRODUCED.forEach(type -> client.post().uri("/api/v1/publications")
                .header("Authorization", bearer).body(new EventType(type)).retrieve().toBodilessEntity());
        client.post().uri("/api/v1/subscriptions").header("Authorization", bearer)
                .body(new Subscription("streetClosureRequested", 4)).retrieve().toBodilessEntity();
        log.info("[core] publicaciones y suscripción de Tránsito registradas");
    }

    private record ModuleCredentials(String module, String secret) {}
    private record Token(String accessToken) {}
    private record EventType(String eventType) {}
    private record Subscription(String eventType, int maxAttempts) {}
}
