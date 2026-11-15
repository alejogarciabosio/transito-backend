package ar.edu.uade.transito.transito_backend;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import ar.edu.uade.transito.transito_backend.support.Payloads;

/** DEF-16 / HU-INF-01: "Toda infracción necesita evidencia o validación del inspector" (vía HTTP). */
class InfractionEvidenceRuleTests extends AbstractIntegrationTest {

    private static String withoutEvidence() {
        return Payloads.infraction("1").replace("[\"https://evidencias/foto1.jpg\"]", "[]");
    }

    @Test
    @DisplayName("Con evidencia -> 201")
    void withEvidenceIsCreated() throws Exception {
        post("/infractions", INSPECTOR, Payloads.infraction("1")).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Inspector sin evidencia -> 201, queda validada por el inspector autenticado")
    void inspectorWithoutEvidenceIsCreated() throws Exception {
        post("/infractions", INSPECTOR, withoutEvidence())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inspectorId").value(INSPECTOR.userId()))
                .andExpect(jsonPath("$.evidenceUrl").isEmpty());
    }

    @Test
    @DisplayName("inspectorId ajeno en el body se ignora: firma el inspector del token")
    void foreignInspectorIdInBodyIsIgnored() throws Exception {
        String body = withoutEvidence().replace("\"inspectorId\": \"u-1001\"", "\"inspectorId\": \"u-OTRO\"");
        post("/infractions", INSPECTOR, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inspectorId").value(INSPECTOR.userId()));
    }

    @Test
    @DisplayName("Administrador no puede crear infracciones (403, permisos sin cambios)")
    void adminCannotCreate() throws Exception {
        post("/infractions", ADMIN, withoutEvidence()).andExpect(status().isForbidden());
        post("/infractions", ADMIN, Payloads.infraction("1")).andExpect(status().isForbidden());
        assertThat(infractionRepository.count()).isZero();
    }

    @Test
    @DisplayName("flagUnpaid (inspector, sin evidencia) sigue creando la infracción")
    void flagUnpaidStillWorks() throws Exception {
        String zoneId = read(post("/parking-zones", ADMIN, Payloads.zone("Z")).andReturn(), "$.zoneId");
        String sessionId = read(post("/parking-sessions", CIUDADANO, Payloads.session(zoneId)).andReturn(), "$.sessionId");
        post("/parking-sessions/" + sessionId + "/unpaid-flag", INSPECTOR, "{\"baseAmount\":12000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.infractionType").value("UNPAID_PARKING"))
                .andExpect(jsonPath("$.inspectorId").value(INSPECTOR.userId()));
    }
}
