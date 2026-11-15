package ar.edu.uade.transito.transito_backend.web.dto;

import java.util.List;

import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;

/** {@code source} es "AI" o "MANUAL"; en MANUAL el tipo es null, la lista vacía y promptVersion null. */
public record InfractionSuggestionResponse(
        InfractionType infractionType,
        List<String> aggravatingFactors,
        String source,
        String promptVersion) {

    public static InfractionSuggestionResponse manual() {
        return new InfractionSuggestionResponse(null, List.of(), "MANUAL", null);
    }
}
