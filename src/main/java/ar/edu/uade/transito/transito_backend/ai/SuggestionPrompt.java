package ar.edu.uade.transito.transito_backend.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Prompt versionado, cargado desde {@code prompts/infraction-suggestion.v1.md}. */
public final class SuggestionPrompt {

    public static final String VERSION = "v1";
    private static final String RESOURCE = "/prompts/infraction-suggestion." + VERSION + ".md";

    private SuggestionPrompt() {
    }

    public static String load() {
        try (InputStream in = SuggestionPrompt.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Falta el prompt " + RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el prompt " + RESOURCE, ex);
        }
    }
}
