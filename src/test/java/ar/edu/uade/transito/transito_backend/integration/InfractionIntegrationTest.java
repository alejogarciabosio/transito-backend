package ar.edu.uade.transito.transito_backend.integration;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.payload.InfractionAnulledPayload;
import ar.edu.uade.transito.transito_backend.event.payload.InfractionAppealedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.InfractionConfirmedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.InfractionRegisteredPayload;
import ar.edu.uade.transito.transito_backend.model.enums.DebtorIdType;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;
import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import ar.edu.uade.transito.transito_backend.support.Payloads;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Infracciones (/infractions) — ciclo de vida, permisos y eventos")
class InfractionIntegrationTest extends AbstractIntegrationTest {

    private String registerAsInspector(String offenderNationalId) throws Exception {
        var result = post("/infractions", INSPECTOR, Payloads.infraction(offenderNationalId))
                .andExpect(status().isCreated()).andReturn();
        return read(result, "$.infractionId");
    }

    @Nested
    @DisplayName("Alta")
    class Register {

        @Test
        @DisplayName("TC-BE-INF-01: inspector registra infracción → 201, PENDING_APPEAL y evento infractionRegistered")
        void inspectorRegisters() throws Exception {
            post("/infractions", INSPECTOR, Payloads.infraction(CIUDADANO.nationalId()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.infractionId", notNullValue()))
                    .andExpect(jsonPath("$.status").value("PENDING_APPEAL"))
                    .andExpect(jsonPath("$.licensePlate").value("AB123CD"))
                    .andExpect(jsonPath("$.evidenceUrl", hasSize(1)))
                    .andExpect(jsonPath("$.location.street").value("Av. Corrientes"));

            assertThat(events.all()).hasSize(1);
            EventEnvelope<?> envelope = events.ofType("infractionRegistered").get(0);
            assertThat(envelope.getEventId()).isNotNull();
            assertThat(envelope.getOccurredAt()).isNotNull();
            assertThat(envelope.getSourceModule()).isEqualTo("transito");
            var payload = (InfractionRegisteredPayload) envelope.getData();
            assertThat(payload.infractionId()).isNotNull();
            assertThat(payload.licensePlate()).isEqualTo("AB123CD");
            assertThat(payload.infractionType()).isEqualTo(InfractionType.SPEEDING);
            assertThat(payload.baseAmount()).isEqualByComparingTo("50000");
            assertThat(payload.finalAmount()).isEqualByComparingTo("75000");
            assertThat(payload.aggravatingFactors()).containsExactly("zona escolar");
            assertThat(payload.originTicketId()).isEqualTo("TCK-1");
        }

        @Test
        @DisplayName("TC-BE-INF-02: el estado enviado por el cliente se ignora, siempre nace PENDING_APPEAL")
        void statusIsForced() throws Exception {
            String body = Payloads.infraction("1").replace("\"licensePlate\"", "\"status\": \"CONFIRMED\", \"licensePlate\"");
            post("/infractions", INSPECTOR, body).andExpect(jsonPath("$.status").value("PENDING_APPEAL"));
        }

        @Test
        @DisplayName("TC-BE-INF-03: Rentas solo recibe confirmadas — registrar NO publica infractionConfirmed")
        void registerDoesNotNotifyRentas() throws Exception {
            registerAsInspector("1");
            assertThat(events.ofType("infractionConfirmed")).isEmpty();
        }

        @Test
        @DisplayName("TC-BE-INF-04: roles distintos de INSPECTOR no pueden registrar → 403")
        void otherRolesForbidden() throws Exception {
            for (var user : new ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser[]{SUPERVISOR, AUDITOR, CIUDADANO, ADMIN, DEPOSITO, OPERADOR}) {
                post("/infractions", user, Payloads.infraction("1")).andExpect(status().isForbidden());
            }
            post("/infractions", null, Payloads.infraction("1")).andExpect(status().isForbidden());
            assertThat(events.all()).isEmpty();
            assertThat(infractionRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Queries {

        @Test
        @DisplayName("TC-BE-INF-05: listado para INSPECTOR / SUPERVISOR / AUDITOR, prohibido para CIUDADANO")
        void listByRole() throws Exception {
            registerAsInspector("1");
            registerAsInspector("2");
            get("/infractions", INSPECTOR).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
            get("/infractions", SUPERVISOR).andExpect(status().isOk());
            get("/infractions", AUDITOR).andExpect(status().isOk());
            get("/infractions", CIUDADANO).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TC-BE-INF-06: /infractions/mine devuelve solo las del ciudadano autenticado")
        void mine() throws Exception {
            registerAsInspector(CIUDADANO.nationalId());
            registerAsInspector(CIUDADANO.nationalId());
            registerAsInspector(OTRO_CIUDADANO.nationalId());
            get("/infractions/mine", CIUDADANO).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
            get("/infractions/mine", OTRO_CIUDADANO).andExpect(jsonPath("$", hasSize(1)));
            get("/infractions/mine", INSPECTOR).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TC-BE-INF-07: detalle — ciudadano ve la propia, no la ajena; inspector/auditor ven cualquiera")
        void detailOwnership() throws Exception {
            String id = registerAsInspector(CIUDADANO.nationalId());
            get("/infractions/" + id, CIUDADANO).andExpect(status().isOk()).andExpect(jsonPath("$.infractionId").value(id));
            get("/infractions/" + id, OTRO_CIUDADANO).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value("No tenés permiso para acceder a este recurso"));
            get("/infractions/" + id, INSPECTOR).andExpect(status().isOk());
            get("/infractions/" + id, AUDITOR).andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-BE-INF-08: id inexistente → 404 con ErrorResponse")
        void notFound() throws Exception {
            get("/infractions/" + UUID.randomUUID(), INSPECTOR)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.message", org.hamcrest.Matchers.startsWith("Infraction no encontrada")));
        }

        @Test
        @DisplayName("TC-BE-INF-09: id con formato inválido → 400")
        void badUuid() throws Exception {
            get("/infractions/no-es-un-uuid", INSPECTOR).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Edición")
    class Update {

        @Test
        @DisplayName("TC-BE-INF-10: inspector edita infracción no firme → 200 con los cambios")
        void updateOpen() throws Exception {
            String id = registerAsInspector(CIUDADANO.nationalId());
            patch("/infractions/" + id, INSPECTOR, Payloads.infractionUpdate())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.licensePlate").value("ZZ999ZZ"))
                    .andExpect(jsonPath("$.finalAmount").value(60000))
                    .andExpect(jsonPath("$.status").value("PENDING_APPEAL"));
        }

        @Test
        @DisplayName("TC-BE-INF-11: actas cerradas no se modifican — editar CONFIRMED o ANNULLED → 409")
        void updateFinal() throws Exception {
            String confirmed = registerAsInspector("1");
            post("/infractions/" + confirmed + "/confirm", SUPERVISOR, null).andExpect(status().isOk());
            patch("/infractions/" + confirmed, INSPECTOR, Payloads.infractionUpdate()).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));

            String annulled = registerAsInspector("1");
            post("/infractions/" + annulled + "/annul", SUPERVISOR, "{\"annulmentReason\":\"x\",\"authorizedBy\":\"u-1002\"}");
            patch("/infractions/" + annulled, INSPECTOR, Payloads.infractionUpdate()).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("TC-BE-INF-12: solo INSPECTOR edita → SUPERVISOR 403")
        void updateForbidden() throws Exception {
            String id = registerAsInspector("1");
            patch("/infractions/" + id, SUPERVISOR, Payloads.infractionUpdate()).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Apelación / descargo")
    class Appeal {

        @Test
        @DisplayName("TC-BE-INF-13: ciudadano titular apela → APPEALED y evento infractionAppealed con el motivo")
        void ownerAppeals() throws Exception {
            String id = registerAsInspector(CIUDADANO.nationalId());
            post("/infractions/" + id + "/appeal", CIUDADANO, "{\"reason\":\"No era mi auto\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPEALED"));

            var payload = events.lastPayload("infractionAppealed", InfractionAppealedPayload.class);
            assertThat(payload.infractionId()).hasToString(id);
            assertThat(payload.reason()).isEqualTo("No era mi auto");
            assertThat(events.ofType("infractionConfirmed")).isEmpty();
        }

        @Test
        @DisplayName("TC-BE-INF-14: apelar dos veces → 409; apelar una confirmada → 409")
        void appealInvalidState() throws Exception {
            String id = registerAsInspector(CIUDADANO.nationalId());
            post("/infractions/" + id + "/appeal", CIUDADANO, "{\"reason\":\"a\"}").andExpect(status().isOk());
            post("/infractions/" + id + "/appeal", CIUDADANO, "{\"reason\":\"b\"}").andExpect(status().isConflict());

            String confirmed = registerAsInspector(CIUDADANO.nationalId());
            post("/infractions/" + confirmed + "/confirm", INSPECTOR, null);
            post("/infractions/" + confirmed + "/appeal", CIUDADANO, "{\"reason\":\"c\"}").andExpect(status().isConflict());
        }

        @Test
        @DisplayName("TC-BE-INF-15: ciudadano no titular no puede apelar → 403; inspector no puede apelar → 403")
        void appealForbidden() throws Exception {
            String id = registerAsInspector(CIUDADANO.nationalId());
            post("/infractions/" + id + "/appeal", OTRO_CIUDADANO, "{\"reason\":\"x\"}").andExpect(status().isForbidden());
            post("/infractions/" + id + "/appeal", INSPECTOR, "{\"reason\":\"x\"}").andExpect(status().isForbidden());
            assertThat(events.ofType("infractionAppealed")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Confirmación (hacia Rentas)")
    class Confirm {

        @Test
        @DisplayName("TC-BE-INF-16: confirmar PENDING_APPEAL → CONFIRMED y evento infractionConfirmed completo para Rentas")
        void confirmPending() throws Exception {
            String id = registerAsInspector(CIUDADANO.nationalId());
            post("/infractions/" + id + "/confirm", SUPERVISOR, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));

            var p = events.lastPayload("infractionConfirmed", InfractionConfirmedPayload.class);
            assertThat(p.infractionId()).hasToString(id);
            assertThat(p.debtorId()).isEqualTo(CIUDADANO.nationalId());
            assertThat(p.debtorIdType()).isEqualTo(DebtorIdType.DNI);
            assertThat(p.licensePlate()).isEqualTo("AB123CD");
            assertThat(p.finalAmount()).isEqualByComparingTo(new BigDecimal("75000"));
            assertThat(p.inspectorId()).isEqualTo("u-1001");
            assertThat(p.location().getStreet()).isEqualTo("Av. Corrientes");
        }

        @Test
        @DisplayName("TC-BE-INF-17: apelación rechazada → confirmar APPEALED → CONFIRMED")
        void confirmAppealed() throws Exception {
            String id = registerAsInspector(CIUDADANO.nationalId());
            post("/infractions/" + id + "/appeal", CIUDADANO, "{\"reason\":\"x\"}");
            post("/infractions/" + id + "/confirm", INSPECTOR, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
            assertThat(events.ofType("infractionConfirmed")).hasSize(1);
        }

        @Test
        @DisplayName("TC-BE-INF-18: confirmar una ya CONFIRMED o ANNULLED → 409 y no se republica el evento")
        void confirmInvalid() throws Exception {
            String id = registerAsInspector("1");
            post("/infractions/" + id + "/confirm", SUPERVISOR, null);
            post("/infractions/" + id + "/confirm", SUPERVISOR, null).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("CONFIRMED")));
            assertThat(events.ofType("infractionConfirmed")).hasSize(1);

            String annulled = registerAsInspector("1");
            post("/infractions/" + annulled + "/annul", SUPERVISOR, "{\"annulmentReason\":\"x\",\"authorizedBy\":\"u-1002\"}");
            post("/infractions/" + annulled + "/confirm", SUPERVISOR, null).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("TC-BE-INF-19: AUDITOR / CIUDADANO no pueden confirmar → 403")
        void confirmForbidden() throws Exception {
            String id = registerAsInspector("1");
            post("/infractions/" + id + "/confirm", AUDITOR, null).andExpect(status().isForbidden());
            post("/infractions/" + id + "/confirm", CIUDADANO, null).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Anulación")
    class Annul {

        @Test
        @DisplayName("TC-BE-INF-20: supervisor anula → ANNULLED y evento infractionAnulled con motivo, autorizante y fecha")
        void supervisorAnnuls() throws Exception {
            String id = registerAsInspector("1");
            post("/infractions/" + id + "/annul", SUPERVISOR,
                    "{\"annulmentReason\":\"Error de carga\",\"authorizedBy\":\"u-1002\"}")
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ANNULLED"));
            var p = events.lastPayload("infractionAnulled", InfractionAnulledPayload.class);
            assertThat(p.infractionId()).hasToString(id);
            assertThat(p.annulmentReason()).isEqualTo("Error de carga");
            assertThat(p.authorizedBy()).isEqualTo("u-1002");
            assertThat(p.annulmentDate()).isNotNull();
        }

        @Test
        @DisplayName("TC-BE-INF-21: una apelada puede anularse (descargo aceptado)")
        void annulAppealed() throws Exception {
            String id = registerAsInspector(CIUDADANO.nationalId());
            post("/infractions/" + id + "/appeal", CIUDADANO, "{\"reason\":\"x\"}");
            post("/infractions/" + id + "/annul", SUPERVISOR, "{\"annulmentReason\":\"ok\",\"authorizedBy\":\"u-1002\"}")
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ANNULLED"));
        }

        @Test
        @DisplayName("TC-BE-INF-22: anulaciones requieren autorización — INSPECTOR → 403; confirmada → 409")
        void annulRules() throws Exception {
            String id = registerAsInspector("1");
            post("/infractions/" + id + "/annul", INSPECTOR, "{\"annulmentReason\":\"x\",\"authorizedBy\":\"u\"}")
                    .andExpect(status().isForbidden());
            post("/infractions/" + id + "/confirm", SUPERVISOR, null);
            post("/infractions/" + id + "/annul", SUPERVISOR, "{\"annulmentReason\":\"x\",\"authorizedBy\":\"u\"}")
                    .andExpect(status().isConflict());
            assertThat(events.ofType("infractionAnulled")).isEmpty();
        }
    }
}
