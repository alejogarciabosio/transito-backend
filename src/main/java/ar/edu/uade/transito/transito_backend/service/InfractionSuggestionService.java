package ar.edu.uade.transito.transito_backend.service;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ar.edu.uade.transito.transito_backend.ai.AiGateway;
import ar.edu.uade.transito.transito_backend.ai.AiGateway.AiCompletion;
import ar.edu.uade.transito.transito_backend.ai.AiGatewayException;
import ar.edu.uade.transito.transito_backend.ai.AiProperties;
import ar.edu.uade.transito.transito_backend.ai.PiiMasker;
import ar.edu.uade.transito.transito_backend.ai.SimpleCircuitBreaker;
import ar.edu.uade.transito.transito_backend.ai.SuggestionPrompt;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;
import ar.edu.uade.transito.transito_backend.web.dto.InfractionSuggestionResponse;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sugiere tipo y agravantes con IA. Es solo una ayuda: nunca registra, confirma ni anula nada.
 * Cualquier problema (apagada, falla, timeout, salida inválida, circuito abierto) degrada a MANUAL.
 */
@Service
public class InfractionSuggestionService {

    /** Catálogo de agravantes del sistema (mismo que usa el frontend en AGGRAVATING_FACTORS). */
    public static final Set<String> AGGRAVATING_CATALOG =
            Set.of("REINCIDENCIA", "ZONA_ESCOLAR", "ALTA_VELOCIDAD", "RESISTENCIA");

    private static final Logger log = LoggerFactory.getLogger(InfractionSuggestionService.class);
    private static final JsonMapper STRICT = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();
    private static final int MAX_CHARS = 500;

    private final AiGateway gateway;
    private final AiProperties props;
    private final SimpleCircuitBreaker breaker;
    private final String systemPrompt;

    public InfractionSuggestionService(AiGateway gateway, AiProperties props) {
        this.gateway = gateway;
        this.props = props;
        this.breaker = new SimpleCircuitBreaker(props.breakerFailures(), props.breakerOpenMs(), Clock.systemUTC());
        this.systemPrompt = SuggestionPrompt.load();
    }

    public InfractionSuggestionResponse suggest(String description) {
        long start = System.nanoTime();
        if (!props.isActive()) {
            return done(InfractionSuggestionResponse.manual(), "disabled", start, -1);
        }
        if (!breaker.allowRequest()) {
            return done(InfractionSuggestionResponse.manual(), "circuit_open", start, -1);
        }
        AiCompletion completion;
        try {
            completion = gateway.complete(systemPrompt, sanitize(description));
            breaker.recordSuccess();
        } catch (RuntimeException ex) {
            breaker.recordFailure();
            // solo la clase y el mensaje controlado; jamás texto del usuario ni respuesta cruda
            return done(InfractionSuggestionResponse.manual(),
                    "provider_error:" + (ex instanceof AiGatewayException ? ex.getMessage() : ex.getClass().getSimpleName()),
                    start, -1);
        }
        InfractionSuggestionResponse parsed = completion == null ? null : parseStrict(completion.content());
        if (parsed == null) {
            return done(InfractionSuggestionResponse.manual(), "invalid_output", start, completion == null ? -1 : completion.totalTokens());
        }
        return done(parsed, "ok", start, completion.totalTokens());
    }

    /** Solo para tests: vuelve el circuito a cerrado. */
    public void resetCircuit() {
        breaker.reset();
    }

    /** Enmascara PII, limita a 500 caracteres y neutraliza los delimitadores del bloque DATO. */
    static String sanitize(String description) {
        String text = PiiMasker.mask(description == null ? "" : description.strip());
        text = text.replace('<', ' ').replace('>', ' ');
        return text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
    }

    /** Parseo estricto: objeto con exactamente las claves esperadas, enum válido y agravantes del catálogo. */
    static InfractionSuggestionResponse parseStrict(String raw) {
        try {
            JsonNode root = STRICT.readTree(raw);
            if (root == null || !root.isObject() || root.size() != 2
                    || !root.has("infractionType") || !root.has("aggravatingFactors")) {
                return null;
            }
            JsonNode type = root.get("infractionType");
            JsonNode factors = root.get("aggravatingFactors");
            if (!factors.isArray() || factors.size() > AGGRAVATING_CATALOG.size() || type.isNull()) {
                return null; // null = la IA no pudo/quiso clasificar → carga manual
            }
            if (!type.isString()) {
                return null;
            }
            InfractionType infractionType = InfractionType.valueOf(type.asString());
            Set<String> valid = new LinkedHashSet<>();
            for (JsonNode f : factors) {
                if (!f.isString() || !AGGRAVATING_CATALOG.contains(f.asString())) {
                    return null;
                }
                valid.add(f.asString());
            }
            return new InfractionSuggestionResponse(infractionType, List.copyOf(valid), "AI", SuggestionPrompt.VERSION);
        } catch (RuntimeException ex) { // JacksonException e IllegalArgumentException (enum inválido)
            return null;
        }
    }

    private InfractionSuggestionResponse done(InfractionSuggestionResponse response, String outcome, long start, int tokens) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        log.info("ai.suggestion promptVersion={} latencyMs={} tokens={} source={} outcome={}",
                SuggestionPrompt.VERSION, ms, tokens, response.source(), outcome);
        return response;
    }
}
