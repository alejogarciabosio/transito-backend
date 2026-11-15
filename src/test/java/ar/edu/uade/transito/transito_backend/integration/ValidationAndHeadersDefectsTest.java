package ar.edu.uade.transito.transito_backend.integration;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import ar.edu.uade.transito.transito_backend.support.Payloads;

/**
 * Regresión de DEF-27 (KI-3), DEF-28 (KI-4), DEF-29 (KI-5/5b) y DEF-30 (CORP).
 */
@DisplayName("Defectos DEF-27..DEF-30: validación, Problem Details y CORP")
class ValidationAndHeadersDefectsTest extends AbstractIntegrationTest {

    private String startSession() throws Exception {
        String zoneId = read(post("/parking-zones", ADMIN, Payloads.zone("Z-" + UUID.randomUUID()))
                .andExpect(status().isCreated()).andReturn(), "$.zoneId");
        return read(post("/parking-sessions", CIUDADANO, Payloads.session(zoneId))
                .andExpect(status().isCreated()).andReturn(), "$.sessionId");
    }

    @ParameterizedTest(name = "body {0}")
    @ValueSource(strings = { "{}", "{\"baseAmount\":0}", "{\"baseAmount\":-5}", "{\"baseAmount\":null}" })
    @DisplayName("DEF-29: unpaid-flag con importe ausente, 0 o negativo → 400 Problem Details y no crea infracción")
    void unpaidFlagRejectsInvalidAmount(String body) throws Exception {
        String id = startSession();
        long before = infractionRepository.count();
        post("/parking-sessions/" + id + "/unpaid-flag", INSPECTOR, body)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.baseAmount").exists());
        assertThat(infractionRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("DEF-29: unpaid-flag con importe válido sigue funcionando")
    void unpaidFlagValidAmountStillWorks() throws Exception {
        String id = startSession();
        post("/parking-sessions/" + id + "/unpaid-flag", INSPECTOR, "{\"baseAmount\":1500.50}")
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("DEF-27/KI-3: rol sin permiso con body inválido recibe 403 (no 400 que revele reglas)")
    void authorizationBeforeValidation() throws Exception {
        post("/impoundments", CIUDADANO, "{}")
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors").doesNotExist());
        post("/parking-sessions/" + UUID.randomUUID() + "/unpaid-flag", CIUDADANO, "{}")
                .andExpect(status().isForbidden());
        put("/parking-zones/" + UUID.randomUUID(), INSPECTOR, "{}")
                .andExpect(status().isForbidden());
        // JSON malformado: también 403 para el rol sin permiso, 400 para el autorizado.
        post("/impoundments", CIUDADANO, "{no-json").andExpect(status().isForbidden());
        post("/impoundments", INSPECTOR, "{no-json").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DEF-27/KI-3: el rol autorizado con body inválido sigue recibiendo 400 con errores")
    void authorizedRoleStillGetsValidationErrors() throws Exception {
        post("/impoundments", INSPECTOR, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("DEF-28/KI-4: parámetro obligatorio faltante → 400 application/problem+json")
    void missingRequestParamIsProblemDetails() throws Exception {
        get("/parking-tariffs", null)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail", containsString("zoneId")))
                .andExpect(jsonPath("$.instance").value("/api/v1/parking-tariffs"));
    }

    @Test
    @DisplayName("DEF-28/KI-4: parámetro con tipo inválido → 400 application/problem+json")
    void typeMismatchIsProblemDetails() throws Exception {
        get("/parking-tariffs?zoneId=no-es-uuid", null)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        get("/infractions/no-es-uuid", INSPECTOR)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("DEF-30: las respuestas incluyen Cross-Origin-Resource-Policy: same-site")
    void corpHeaderPresent() throws Exception {
        get("/street-closures/current", null)
                .andExpect(header().string("Cross-Origin-Resource-Policy", "same-site"));
        get("/infractions", null)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cross-Origin-Resource-Policy", "same-site"));
    }
}
