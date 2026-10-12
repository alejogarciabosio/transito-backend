package ar.edu.uade.transito.transito_backend.integration;

import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureActivatedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureApprovedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureEndedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureRejectedPayload;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Cortes de calle (/street-closures) — nacen del evento streetClosureRequested")
class StreetClosureIntegrationTest extends AbstractIntegrationTest {

    private static final String REQUESTED_FROM = "2026-10-01T00:00:00-03:00";
    private static final String REQUESTED_TO = "2026-10-02T00:00:00-03:00";

    private String requestClosure(String moduleCode) {
        dispatchIncoming("streetClosureRequested", "{\"closureRequestId\":\"REQ-" + UUID.randomUUID() + "\","
                + "\"sourceModule\":\"" + moduleCode + "\","
                + "\"requestedFrom\":\"" + REQUESTED_FROM + "\","
                + "\"requestedTo\":\"" + REQUESTED_TO + "\"}");
        return streetClosureRepository.findAll().get((int) streetClosureRepository.count() - 1).getStreetClosureId().toString();
    }

    private static final String APPROVE = """
            {"detours":[{"street":"Av. Santa Fe","alternateRoute":"Av. Córdoba"}],"conditions":["Señalización preventiva"]}
            """;

    @Test
    @DisplayName("TC-BE-SC-01: evento streetClosureRequested (M3) crea el corte en REQUESTED con módulo OBRAS")
    void createdFromEvent() throws Exception {
        String id = requestClosure("M3");
        get("/street-closures/" + id, SUPERVISOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.requestingModule").value("OBRAS"))
                .andExpect(jsonPath("$.startDate").value("2026-10-01T03:00:00Z"));
        requestClosure("M6");
        get("/street-closures", OPERADOR).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        assertThat(events.all()).as("crear desde solicitud no publica eventos").isEmpty();
    }

    @Test
    @DisplayName("TC-BE-SC-02: código de módulo desconocido en el evento → se rechaza (IllegalArgumentException)")
    void unknownModule() {
        assertThatThrownBy(() -> dispatchIncoming("streetClosureRequested", "{\"closureRequestId\":\"REQ\","
                + "\"sourceModule\":\"M9\",\"requestedFrom\":\"" + REQUESTED_FROM + "\",\"requestedTo\":\"" + REQUESTED_TO + "\"}"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(streetClosureRepository.count()).isZero();
    }

    @Test
    @DisplayName("TC-BE-SC-03: aprobar → APPROVED con desvíos/condiciones y evento streetClosureApproved")
    void approve() throws Exception {
        String id = requestClosure("M3");
        post("/street-closures/" + id + "/approval", INSPECTOR, APPROVE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.detours[0].alternateRoute").value("Av. Córdoba"))
                .andExpect(jsonPath("$.conditions[0]").value("Señalización preventiva"));
        var p = events.lastPayload("streetClosureApproved", StreetClosureApprovedPayload.class);
        assertThat(p.streetClosureId()).hasToString(id);
        assertThat(p.closureRequestId()).startsWith("REQ-");
        assertThat(p.requestingModule()).isEqualTo(RequestingModule.OBRAS);
        assertThat(p.startDate()).isEqualTo(OffsetDateTime.parse(REQUESTED_FROM));
        assertThat(p.detours()).hasSize(1);
        assertThat(p.conditions()).containsExactly("Señalización preventiva");
    }

    @Test
    @DisplayName("TC-BE-SC-05: rechazar → REJECTED y evento streetClosureRejected con motivo y módulo")
    void reject() throws Exception {
        String id = requestClosure("M6");
        post("/street-closures/" + id + "/rejection", SUPERVISOR, "{\"rejectionReason\":\"Horario pico\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        var p = events.lastPayload("streetClosureRejected", StreetClosureRejectedPayload.class);
        assertThat(p.rejectionReason()).isEqualTo("Horario pico");
        assertThat(p.requestingModule()).isEqualTo(RequestingModule.AMBIENTE);
        // un rechazado no se puede aprobar ni activar
        post("/street-closures/" + id + "/approval", SUPERVISOR, APPROVE).andExpect(status().isConflict());
        post("/street-closures/" + id + "/activation", OPERADOR, null).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-BE-SC-06: flujo completo REQUESTED→APPROVED→ACTIVE→FINISHED con eventos activated/ended")
    void fullLifecycle() throws Exception {
        String id = requestClosure("M3");
        post("/street-closures/" + id + "/approval", SUPERVISOR, APPROVE).andExpect(status().isOk());
        post("/street-closures/" + id + "/activation", OPERADOR, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        var activated = events.lastPayload("streetClosureActivated", StreetClosureActivatedPayload.class);
        assertThat(activated.streetClosureId()).hasToString(id);
        assertThat(activated.sourceModule()).isEqualTo("M3");
        assertThat(activated.activatedBy()).isEqualTo(OPERADOR.userId());
        assertThat(activated.activatedAt()).isNotNull();
        assertThat(activated.sourceRequestId()).startsWith("REQ-");

        post("/street-closures/" + id + "/completion", OPERADOR, "{\"notes\":\"Circulación restablecida\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINISHED"));
        var ended = events.lastPayload("streetClosureEnded", StreetClosureEndedPayload.class);
        assertThat(ended.notes()).isEqualTo("Circulación restablecida");
        assertThat(ended.completionDateTime()).isNotNull();

        assertThat(events.all()).extracting("eventType")
                .containsExactly("streetClosureApproved", "streetClosureActivated", "streetClosureEnded");
    }

    @Test
    @DisplayName("TC-BE-SC-07: transiciones inválidas → 409 (activar REQUESTED, finalizar APPROVED, aprobar dos veces)")
    void invalidTransitions() throws Exception {
        String id = requestClosure("M3");
        post("/street-closures/" + id + "/activation", OPERADOR, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", org.hamcrest.Matchers.containsString("debe estar en estado APPROVED")));
        post("/street-closures/" + id + "/completion", OPERADOR, "{\"notes\":\"x\"}").andExpect(status().isConflict());
        post("/street-closures/" + id + "/approval", SUPERVISOR, APPROVE).andExpect(status().isOk());
        post("/street-closures/" + id + "/approval", SUPERVISOR, APPROVE).andExpect(status().isConflict());
        post("/street-closures/" + id + "/rejection", SUPERVISOR, "{\"rejectionReason\":\"x\"}").andExpect(status().isConflict());
        post("/street-closures/" + id + "/completion", OPERADOR, "{\"notes\":\"x\"}").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-BE-SC-08: permisos — activar/finalizar solo OPERADOR_CONTROL; aprobar no OPERADOR; consultar no CIUDADANO")
    void permissions() throws Exception {
        String id = requestClosure("M3");
        post("/street-closures/" + id + "/approval", OPERADOR, APPROVE).andExpect(status().isForbidden());
        post("/street-closures/" + id + "/approval", SUPERVISOR, APPROVE);
        post("/street-closures/" + id + "/activation", INSPECTOR, null).andExpect(status().isForbidden());
        post("/street-closures/" + id + "/completion", SUPERVISOR, "{\"notes\":\"x\"}").andExpect(status().isForbidden());
        get("/street-closures", CIUDADANO).andExpect(status().isForbidden());
        get("/street-closures/" + UUID.randomUUID(), INSPECTOR).andExpect(status().isNotFound());
    }
}
