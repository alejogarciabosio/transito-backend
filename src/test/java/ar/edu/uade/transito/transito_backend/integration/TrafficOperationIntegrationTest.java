package ar.edu.uade.transito.transito_backend.integration;

import ar.edu.uade.transito.transito_backend.event.payload.TrafficOperationCreatedPayload;
import ar.edu.uade.transito.transito_backend.model.enums.OperationType;
import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import ar.edu.uade.transito.transito_backend.support.Payloads;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Operativos de control (/operations)")
class TrafficOperationIntegrationTest extends AbstractIntegrationTest {

    private String create() throws Exception {
        return read(post("/operations", INSPECTOR, Payloads.operation()).andExpect(status().isCreated()).andReturn(), "$.operationId");
    }

    @Test
    @DisplayName("TC-BE-OP-01: crear → OPEN, sin resolución y evento trafficOperationCreated")
    void create_publishesEvent() throws Exception {
        String body = Payloads.operation().replace("\"type\"", "\"status\":\"CLOSED\",\"resolution\":\"NO_ACTION\",\"type\"");
        post("/operations", OPERADOR, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.resolution", nullValue()));
        var p = events.lastPayload("trafficOperationCreated", TrafficOperationCreatedPayload.class);
        assertThat(p.operationId()).isNotNull();
        assertThat(p.type()).isEqualTo(OperationType.ROUTINE_CONTROL);
        assertThat(p.zone()).isEqualTo("Microcentro");
        assertThat(p.assignedInspectors()).containsExactly("u-1001");
        assertThat(p.assignedUnits()).containsExactly("MOV-12");
        assertThat(p.ticketId()).isEqualTo("TCK-5");
    }

    @Test
    @DisplayName("TC-BE-OP-02: permisos de alta/consulta/edición")
    void permissions() throws Exception {
        post("/operations", SUPERVISOR, Payloads.operation()).andExpect(status().isForbidden());
        String id = create();
        get("/operations", AUDITOR).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        get("/operations/" + id, SUPERVISOR).andExpect(status().isOk());
        get("/operations", OPERADOR).andExpect(status().isForbidden());
        patch("/operations/" + id, OPERADOR, "{\"zone\":\"x\"}").andExpect(status().isForbidden());
        post("/operations/" + id + "/close", SUPERVISOR, "{\"resolution\":\"NO_ACTION\"}").andExpect(status().isForbidden());
        get("/operations/" + UUID.randomUUID(), INSPECTOR).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-BE-OP-03: editar operativo abierto")
    void update() throws Exception {
        String id = create();
        patch("/operations/" + id, INSPECTOR,
                "{\"zone\":\"Once\",\"endDateTime\":\"2026-09-20T12:00:00\",\"assignedInspectors\":[\"u-1\",\"u-2\"],\"assignedUnits\":[]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zone").value("Once"))
                .andExpect(jsonPath("$.assignedInspectors", hasSize(2)))
                .andExpect(jsonPath("$.endDateTime").value("2026-09-20T12:00:00"));
    }

    @Test
    @DisplayName("TC-BE-OP-04: cerrar con resolución → CLOSED; si no tenía fin, se completa con 'ahora'")
    void closeSetsEnd() throws Exception {
        String id = create();
        post("/operations/" + id + "/close", INSPECTOR, "{\"resolution\":\"INFRACTION_CREATED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.resolution").value("INFRACTION_CREATED"))
                .andExpect(jsonPath("$.endDateTime", notNullValue()));
    }

    @Test
    @DisplayName("TC-BE-OP-05: cerrar respeta endDateTime existente")
    void closeKeepsEnd() throws Exception {
        String id = create();
        patch("/operations/" + id, INSPECTOR, "{\"zone\":\"Z\",\"endDateTime\":\"2026-09-20T12:00:00\",\"assignedInspectors\":[],\"assignedUnits\":[]}");
        post("/operations/" + id + "/close", INSPECTOR, "{\"resolution\":\"NO_ACTION\"}")
                .andExpect(jsonPath("$.endDateTime").value("2026-09-20T12:00:00"));
    }

    @Test
    @DisplayName("TC-BE-OP-06: operativo cerrado no se edita ni se vuelve a cerrar → 409")
    void closedIsFinal() throws Exception {
        String id = create();
        post("/operations/" + id + "/close", INSPECTOR, "{\"resolution\":\"NO_ACTION\"}");
        patch("/operations/" + id, INSPECTOR, "{\"zone\":\"x\"}").andExpect(status().isConflict());
        post("/operations/" + id + "/close", INSPECTOR, "{\"resolution\":\"NO_ACTION\"}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("ya está cerrado")));
    }
}
