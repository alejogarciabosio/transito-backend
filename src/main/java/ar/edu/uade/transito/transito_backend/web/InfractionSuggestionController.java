package ar.edu.uade.transito.transito_backend.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.transito.transito_backend.service.InfractionSuggestionService;
import ar.edu.uade.transito.transito_backend.web.dto.InfractionSuggestionRequest;
import ar.edu.uade.transito.transito_backend.web.dto.InfractionSuggestionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Ayuda opcional de IA para el inspector. Una persona siempre confirma; este endpoint no persiste nada. */
@RestController
@RequestMapping("/api/v1/infractions/suggestions")
@RequiredArgsConstructor
public class InfractionSuggestionController {

    private final InfractionSuggestionService suggestionService;

    @PreAuthorize("hasRole('INSPECTOR')")
    @PostMapping
    public InfractionSuggestionResponse suggest(@Valid @RequestBody InfractionSuggestionRequest request) {
        return suggestionService.suggest(request.description());
    }
}
