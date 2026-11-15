package ar.edu.uade.transito.transito_backend.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Configuración de IA (solo variables de entorno; la clave jamás se versiona ni se loguea). */
@Component
public class AiProperties {

    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final long timeoutMs;
    private final String url;
    private final int breakerFailures;
    private final long breakerOpenMs;

    public AiProperties(@Value("${ai.enabled:false}") boolean enabled,
            @Value("${ai.groq.api-key:}") String apiKey,
            @Value("${ai.model:openai/gpt-oss-20b}") String model,
            @Value("${ai.timeout-ms:5000}") long timeoutMs,
            @Value("${ai.groq.url:https://api.groq.com/openai/v1/chat/completions}") String url,
            @Value("${ai.breaker.failures:3}") int breakerFailures,
            @Value("${ai.breaker.open-ms:30000}") long breakerOpenMs) {
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.timeoutMs = timeoutMs;
        this.url = url;
        this.breakerFailures = breakerFailures;
        this.breakerOpenMs = breakerOpenMs;
    }

    /** La IA solo está activa si el flag está en true Y hay clave. */
    public boolean isActive() {
        return enabled && !apiKey.isEmpty();
    }

    public String apiKey() { return apiKey; }
    public String model() { return model; }
    public long timeoutMs() { return timeoutMs; }
    public String url() { return url; }
    public int breakerFailures() { return breakerFailures; }
    public long breakerOpenMs() { return breakerOpenMs; }

    @Override
    public String toString() {
        return "AiProperties[enabled=" + enabled + ", model=" + model + "]"; // sin clave
    }
}
