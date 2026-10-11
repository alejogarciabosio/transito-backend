package ar.edu.uade.transito.transito_backend.bugs;

import ar.edu.uade.transito.transito_backend.event.payload.InfractionAnulledPayload;
import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureApprovedPayload;
import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import ar.edu.uade.transito.transito_backend.support.Payloads;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cada test describe el comportamiento CORRECTO esperado. Hoy FALLAN porque reproducen un bug abierto.
 * Están excluidos del build normal (@Tag("bug")). Para correrlos:
 *   ./mvnw test -Dbugs.excluded=none -Dgroups=bug -Djacoco.skip=true
 * Cuando el bug se arregle, el test pasa: mover el test a la suite normal y sacarle el @Tag.
 */
@Tag("bug")
@DisplayName("Reproducción de bugs abiertos (deben FALLAR hasta que se arreglen)")
class BugReproductionTest extends AbstractIntegrationTest {

    private String zone() throws Exception {
        return read(post("/parking-zones", ADMIN, Payloads.zone("Z")).andReturn(), "$.zoneId");
    }

    private String session(String zoneId) throws Exception {
        return read(post("/parking-sessions", CIUDADANO, Payloads.session(zoneId)).andReturn(), "$.sessionId");
    }

    private String infraction() throws Exception {
        return read(post("/infractions", INSPECTOR, Payloads.infraction(CIUDADANO.nationalId())).andReturn(), "$.infractionId");
    }

    private String closure() {
        dispatchIncoming("streetClosureRequested", "{\"closureRequestId\":\"REQ-1\",\"sourceModule\":\"M3\","
                + "\"requestedFrom\":\"2026-10-01T00:00:00-03:00\",\"requestedTo\":\"2026-10-02T00:00:00-03:00\"}");
        return streetClosureRepository.findAll().get(0).getStreetClosureId().toString();
    }

    // ---------------------------------------------------------------- Críticos / Altos

    @Test
    @DisplayName("BUG-BE-01: flag-unpaid debería crear la infracción UNPAID_PARKING (hoy 500: location null)")
    void bug01_flagUnpaidAlways500() throws Exception {
        String id = session(zone());
        post("/parking-sessions/" + id + "/flag-unpaid", INSPECTOR, "{\"baseAmount\":12000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.infractionType").value("UNPAID_PARKING"));
    }

    @Test
    @DisplayName("BUG-BE-02: aprobar corte sin detours/conditions debería responder 200 (hoy 500: List.of() inmutable)")
    void bug02_approveWithoutDetours() throws Exception {
        post("/street-closures/" + closure() + "/approve", SUPERVISOR, "{}").andExpect(status().isOk());
    }

    @Test
    @DisplayName("BUG-BE-03: login sin username debería ser 401 (hoy 500: NPE en Map.of().get(null))")
    void bug03_loginWithoutUsername() throws Exception {
        post("/auth/login", null, "{}").andExpect(status().isUnauthorized());
        post("/auth/login", null, "{\"password\":\"x\"}").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("BUG-BE-04: RN 'toda infracción necesita evidencia' — alta sin evidencia debería rechazarse (hoy 201)")
    void bug04_infractionWithoutEvidence() throws Exception {
        String body = Payloads.infraction("1").replace("[\"https://evidencias/foto1.jpg\"]", "[]");
        post("/infractions", INSPECTOR, body).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BUG-BE-05: inspectorId debería salir del token, no del body (hoy se puede firmar a nombre de otro)")
    void bug05_inspectorIdSpoofing() throws Exception {
        String body = Payloads.infraction("1").replace("\"inspectorId\": \"u-1001\"", "\"inspectorId\": \"u-OTRO\"");
        post("/infractions", INSPECTOR, body).andExpect(jsonPath("$.inspectorId").value(INSPECTOR.userId()));
    }

    @Test
    @DisplayName("BUG-BE-05b: idem en siniestros — inspectorId del body")
    void bug05b_accidentInspectorIdSpoofing() throws Exception {
        String body = Payloads.accident().replace("\"inspectorId\": \"u-1001\"", "\"inspectorId\": \"u-OTRO\"");
        post("/accidents", INSPECTOR, body).andExpect(jsonPath("$.inspectorId").value(INSPECTOR.userId()));
    }

    @Test
    @DisplayName("BUG-BE-06: RN 'vehículo retenido no se libera sin cumplir requisitos' — liberar sin requisitos ni pago debería rechazarse (hoy 200)")
    void bug06_releaseWithoutRequirements() throws Exception {
        String id = read(post("/impoundments", INSPECTOR, Payloads.impoundment("1")).andReturn(), "$.impoundmentId");
        post("/impoundments/" + id + "/release", DEPOSITO, "{}").andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("BUG-BE-07: RN 'anulaciones requieren autorización' — authorizedBy debería ser el supervisor autenticado (hoy viene del body, puede ser null)")
    void bug07_annulAuthorizedByFromBody() throws Exception {
        String id = infraction();
        post("/infractions/" + id + "/annul", SUPERVISOR, "{\"annulmentReason\":\"x\"}").andExpect(status().isOk());
        assertThat(events.lastPayload("infractionAnulled", InfractionAnulledPayload.class).authorizedBy())
                .isEqualTo(SUPERVISOR.userId());
    }

    // ---------------------------------------------------------------- Medios

    @Test
    @DisplayName("BUG-BE-08: campos obligatorios faltantes deberían dar 400 con ErrorResponse (hoy 500)")
    void bug08_missingRequiredFieldsGive500() throws Exception {
        post("/parking-zones", ADMIN, "{}").andExpect(status().isBadRequest());
        post("/infractions", INSPECTOR, "{\"licensePlate\":\"AA\"}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BUG-BE-09a: PATCH /infractions parcial debería conservar lo no enviado (hoy pisa con null → 500)")
    void bug09a_patchInfractionPartial() throws Exception {
        String id = infraction();
        patch("/infractions/" + id, INSPECTOR, "{\"finalAmount\":1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value("AB123CD"));
    }

    @Test
    @DisplayName("BUG-BE-09b: PATCH /operations parcial no debería borrar inspectores/unidades")
    void bug09b_patchOperationPartial() throws Exception {
        String id = read(post("/operations", INSPECTOR, Payloads.operation()).andReturn(), "$.operationId");
        patch("/operations/" + id, INSPECTOR, "{\"zone\":\"Once\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedInspectors", hasSize(1)));
    }

    @Test
    @DisplayName("BUG-BE-09c: PATCH /impoundments parcial debería conservar depósito (hoy 500)")
    void bug09c_patchImpoundmentPartial() throws Exception {
        String id = read(post("/impoundments", INSPECTOR, Payloads.impoundment("1")).andReturn(), "$.impoundmentId");
        patch("/impoundments/" + id, DEPOSITO, "{\"reason\":\"Sin VTV\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depotLocation").value("Playa Sur"));
    }

    @Test
    @DisplayName("BUG-BE-09d: PATCH /accidents parcial debería conservar fecha y ubicación (hoy 500)")
    void bug09d_patchAccidentPartial() throws Exception {
        String id = read(post("/accidents", INSPECTOR, Payloads.accident()).andReturn(), "$.accidentId");
        patch("/accidents/" + id, INSPECTOR, "{\"damageDescription\":\"x\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.street").value("Av. 9 de Julio"));
    }

    @Test
    @DisplayName("BUG-BE-10: iniciar sesión de estacionamiento en zona inexistente o INACTIVE debería rechazarse (hoy 201)")
    void bug10_sessionInInvalidZone() throws Exception {
        post("/parking-sessions", CIUDADANO, Payloads.session(UUID.randomUUID().toString())).andExpect(status().is4xxClientError());
        String z = zone();
        delete("/parking-zones/" + z, ADMIN);
        post("/parking-sessions", CIUDADANO, Payloads.session(z)).andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("BUG-BE-11: tarifa con vigencia invertida o precio negativo debería rechazarse (hoy 201)")
    void bug11_invalidTariff() throws Exception {
        String z = zone();
        post("/parking-tariffs", ADMIN, "{\"zoneId\":\"" + z + "\",\"pricePerHour\":-10,\"validFrom\":\"2026-12-31\",\"validTo\":\"2026-01-01\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BUG-BE-12: JSON mal formado debería devolver el formato ErrorResponse con 'message' (guía §3)")
    void bug12_malformedJsonFormat() throws Exception {
        post("/infractions", INSPECTOR, "{esto no es json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("BUG-BE-13: JWT válido sin claim 'role' debería ser 403 (hoy NPE → 500)")
    void bug13_tokenWithoutRole() throws Exception {
        String forged = io.jsonwebtoken.Jwts.builder().subject("u").claim("nationalId", "1")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "test-secret-de-al-menos-32-bytes-para-hs256-0123456789".getBytes()))
                .compact();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/infractions")
                .header("Authorization", "Bearer " + forged)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BUG-BE-14: streetClosureApproved debería informar affectedStreet (campo requerido por contrato; hoy siempre [])")
    void bug14_affectedStreetAlwaysEmpty() throws Exception {
        String id = closure();
        post("/street-closures/" + id + "/approve", SUPERVISOR,
                "{\"detours\":[{\"street\":\"A\",\"alternateRoute\":\"B\"}],\"conditions\":[\"c\"]}");
        assertThat(events.lastPayload("streetClosureApproved", StreetClosureApprovedPayload.class).affectedStreet()).isNotEmpty();
    }

    @Test
    @DisplayName("BUG-BE-15: no debería poder marcarse impaga dos veces la misma sesión (duplica la multa)")
    void bug15_flagUnpaidTwice() throws Exception {
        String id = session(zone());
        post("/parking-sessions/" + id + "/flag-unpaid", INSPECTOR, "{\"baseAmount\":1}");
        post("/parking-sessions/" + id + "/flag-unpaid", INSPECTOR, "{\"baseAmount\":1}").andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- Bajos

    @Test
    @DisplayName("BUG-BE-16: SUPERVISOR puede listar y confirmar, pero no ver el detalle de una infracción (403)")
    void bug16_supervisorCannotSeeDetail() throws Exception {
        get("/infractions/" + infraction(), SUPERVISOR).andExpect(status().isOk());
    }
}
