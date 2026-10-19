package ar.edu.uade.transito.transito_backend.integration;

import ar.edu.uade.transito.transito_backend.event.payload.RoadAccidentRegisteredPayload;
import ar.edu.uade.transito.transito_backend.model.enums.InjuryLevel;
import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import ar.edu.uade.transito.transito_backend.support.Payloads;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Siniestros viales (/accidents)")
class RoadAccidentIntegrationTest extends AbstractIntegrationTest {

    private String register() throws Exception {
        return read(post("/accidents", INSPECTOR, Payloads.accident()).andExpect(status().isCreated()).andReturn(), "$.accidentId");
    }

    @Test
    @DisplayName("TC-BE-ACC-01: INSPECTOR/OPERADOR registran → PRELIMINARY y evento roadAccidentRegistered con vehículos y personas")
    void register_publishesEvent() throws Exception {
        post("/accidents", OPERADOR, Payloads.accident())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PRELIMINARY"))
                .andExpect(jsonPath("$.vehicles", hasSize(1)))
                .andExpect(jsonPath("$.persons[0].injuryLevel").value("MINOR"));

        var p = events.lastPayload("roadAccidentRegistered", RoadAccidentRegisteredPayload.class);
        assertThat(p.accidentId()).isNotNull();
        assertThat(p.involvesInfrastructureDamage()).isTrue();
        assertThat(p.damageDescription()).isEqualTo("Semáforo derribado");
        assertThat(p.vehicles()).extracting("licensePlate").containsExactly("AB123CD");
        assertThat(p.persons()).extracting("injuryLevel").containsExactly(InjuryLevel.MINOR);
        assertThat(p.inspectorId()).isEqualTo("u-1001");
        assertThat(p.location().getStreet()).isEqualTo("Av. 9 de Julio");
    }

    @Test
    @DisplayName("TC-BE-ACC-01b: accidente sin daño de infraestructura no se publica a Obras")
    void register_withoutInfrastructureDamage_doesNotPublishEvent() throws Exception {
        post("/accidents", INSPECTOR, Payloads.accident().replace("\"involvesInfrastructureDamage\": true",
                "\"involvesInfrastructureDamage\": false"))
                .andExpect(status().isCreated());

        assertThat(events.all()).extracting("eventType").doesNotContain("roadAccidentRegistered");
    }

    @Test
    @DisplayName("TC-BE-ACC-01c: daño de infraestructura sin descripción se rechaza")
    void register_withInfrastructureDamageWithoutDescription_isRejected() throws Exception {
        post("/accidents", INSPECTOR, Payloads.accident().replace("\"damageDescription\": \"Semáforo derribado\"",
                "\"damageDescription\": \"\""))
                .andExpect(status().isBadRequest());

        assertThat(events.all()).isEmpty();
    }

    @Test
    @DisplayName("TC-BE-ACC-02: permisos — Ciudadano no registra ni consulta")
    void permissions() throws Exception {
        post("/accidents", CIUDADANO, Payloads.accident()).andExpect(status().isForbidden());
        String id = register();
        get("/accidents", AUDITOR).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        get("/accidents/" + id, SUPERVISOR).andExpect(status().isOk());
        get("/accidents", CIUDADANO).andExpect(status().isForbidden());
        patch("/accidents/" + id, ADMIN, Payloads.accidentUpdate(null)).andExpect(status().isForbidden());
        get("/accidents/" + UUID.randomUUID(), INSPECTOR).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-BE-ACC-03: editar PRELIMINARY sin status mantiene PRELIMINARY")
    void updatePreliminary() throws Exception {
        String id = register();
        patch("/accidents/" + id, INSPECTOR, Payloads.accidentUpdate(null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRELIMINARY"))
                .andExpect(jsonPath("$.damageDescription").value("Sin daños"))
                .andExpect(jsonPath("$.involvesInfrastructureDamage").value(false))
                .andExpect(jsonPath("$.vehicles", hasSize(0)));
    }

    @Test
    @DisplayName("TC-BE-ACC-04: verificar (status VERIFIED) y luego cualquier edición → 409 (acta cerrada)")
    void verifiedIsFinal() throws Exception {
        String id = register();
        patch("/accidents/" + id, INSPECTOR, Payloads.accidentUpdate("VERIFIED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VERIFIED"));
        patch("/accidents/" + id, INSPECTOR, Payloads.accidentUpdate("PRELIMINARY")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-BE-ACC-05: enviar status PRELIMINARY no hace nada especial (no hay 'des-verificar')")
    void preliminaryStatusIgnored() throws Exception {
        String id = register();
        patch("/accidents/" + id, INSPECTOR, Payloads.accidentUpdate("PRELIMINARY"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PRELIMINARY"));
    }

    @Test
    @DisplayName("TC-BE-ACC-06: enum inválido en el body → 400")
    void invalidEnum() throws Exception {
        post("/accidents", INSPECTOR, Payloads.accident().replace("MINOR", "MUERTO")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-BE-ACC-CIU-01: /accidents/mine devuelve los siniestros donde el ciudadano figura, sin duplicados")
    void mine() throws Exception {
        // Payloads.accident() incluye al CIUDADANO como persona y como titular: debe aparecer una sola vez.
        String id = register();
        post("/accidents", INSPECTOR, Payloads.accident().replace("30777888", "11111111")).andExpect(status().isCreated());

        get("/accidents/mine", CIUDADANO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accidentId").value(id));
        get("/accidents/mine", OTRO_CIUDADANO).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        get("/accidents/mine", INSPECTOR).andExpect(status().isForbidden());
        get("/accidents/mine", null).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-BE-ACC-CIU-02: /accidents/mine también encuentra al ciudadano solo como titular de un vehículo")
    void mine_asVehicleOwnerOnly() throws Exception {
        String soloTitular = Payloads.accident()
                .replace("{\"nationalId\": \"30777888\", \"injuryLevel\": \"MINOR\"}", "{\"nationalId\": \"22222222\", \"injuryLevel\": \"NONE\"}");
        assertThat(soloTitular).doesNotContain("\"nationalId\": \"30777888\"");
        post("/accidents", INSPECTOR, soloTitular).andExpect(status().isCreated());

        get("/accidents/mine", CIUDADANO).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }
}
