package ar.edu.uade.transito.transito_backend.integration;

import ar.edu.uade.transito.transito_backend.event.payload.VehicleImpoundedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.VehicleReleasedPayload;
import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentOriginType;
import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import ar.edu.uade.transito.transito_backend.support.Payloads;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Retención de vehículos (/impoundments)")
class VehicleImpoundmentIntegrationTest extends AbstractIntegrationTest {

    private String register(String owner) throws Exception {
        return read(post("/impoundments", INSPECTOR, Payloads.impoundment(owner)).andExpect(status().isCreated()).andReturn(), "$.impoundmentId");
    }

    @Test
    @DisplayName("TC-BE-IMP-01: registrar → IMPOUNDED, sin referencia de pago, y evento vehicleImpounded")
    void register_publishesEvent() throws Exception {
        String body = Payloads.impoundment(CIUDADANO.nationalId()).replace("{", "{\"paymentReference\":\"TRAMPA\",\"status\":\"RELEASED\",");
        post("/impoundments", DEPOSITO, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IMPOUNDED"))
                .andExpect(jsonPath("$.paymentReference", nullValue()));
        var p = events.lastPayload("vehicleImpounded", VehicleImpoundedPayload.class);
        assertThat(p.impoundmentId()).isNotNull();
        assertThat(p.licensePlate()).isEqualTo("AB123CD");
        assertThat(p.ownerNationalId()).isEqualTo(CIUDADANO.nationalId());
        assertThat(p.depotLocation()).isEqualTo("Playa Sur");
        assertThat(p.originType()).isEqualTo(ImpoundmentOriginType.OPERATION);
        assertThat(p.originId()).hasToString("5a51284e-8fbe-4ab2-8db4-823d37c27d12");
    }

    @Test
    @DisplayName("TC-BE-IMP-02: consultas — ciudadano solo ve su propio vehículo")
    void ownership() throws Exception {
        String id = register(CIUDADANO.nationalId());
        get("/impoundments", SUPERVISOR).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        get("/impoundments", CIUDADANO).andExpect(status().isForbidden());
        get("/impoundments/" + id, CIUDADANO).andExpect(status().isOk());
        get("/impoundments/" + id, OTRO_CIUDADANO).andExpect(status().isForbidden());
        get("/impoundments/" + id, DEPOSITO).andExpect(status().isOk());
        get("/impoundments/" + UUID.randomUUID(), INSPECTOR).andExpect(status().isNotFound());
        post("/impoundments", CIUDADANO, Payloads.impoundment("1")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-BE-IMP-03: PERSONAL_DEPOSITO edita motivo/depósito/titular mientras está retenido")
    void update() throws Exception {
        String id = register("1");
        patch("/impoundments/" + id, DEPOSITO, "{\"reason\":\"Sin VTV\",\"depotLocation\":\"Playa Norte\",\"ownerNationalId\":\"2\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Sin VTV"))
                .andExpect(jsonPath("$.depotLocation").value("Playa Norte"))
                .andExpect(jsonPath("$.ownerNationalId").value("2"));
        patch("/impoundments/" + id, INSPECTOR, "{\"reason\":\"x\"}").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-BE-IMP-04: liberar con requisitos → RELEASED, releasedBy del token y evento vehicleReleased")
    void release() throws Exception {
        String id = register("1");
        post("/impoundments/" + id + "/release", DEPOSITO, Payloads.release())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"))
                .andExpect(jsonPath("$.releasedBy").value(DEPOSITO.userId()))
                .andExpect(jsonPath("$.paymentReference").value("PAY-777"))
                .andExpect(jsonPath("$.verifiedRequirements", hasSize(2)));
        var p = events.lastPayload("vehicleReleased", VehicleReleasedPayload.class);
        assertThat(p.impoundmentId()).hasToString(id);
        assertThat(p.releaseDateTime()).isNotNull();
        assertThat(p.towingAndStorageFee()).isEqualByComparingTo("25000");
        assertThat(p.verifiedRequirements()).containsExactly("Licencia vigente", "Seguro vigente");
        assertThat(p.releasedBy()).isEqualTo(DEPOSITO.userId());
    }

    @Test
    @DisplayName("TC-BE-IMP-05: liberado no se edita ni se libera de nuevo → 409; solo DEPOSITO libera → 403")
    void releasedIsFinal() throws Exception {
        String id = register("1");
        post("/impoundments/" + id + "/release", INSPECTOR, Payloads.release()).andExpect(status().isForbidden());
        post("/impoundments/" + id + "/release", DEPOSITO, Payloads.release()).andExpect(status().isOk());
        post("/impoundments/" + id + "/release", DEPOSITO, Payloads.release()).andExpect(status().isConflict());
        patch("/impoundments/" + id, DEPOSITO, "{\"reason\":\"x\",\"depotLocation\":\"y\"}").andExpect(status().isConflict());
        assertThat(events.ofType("vehicleReleased")).hasSize(1);
    }

    @Test
    @DisplayName("TC-BE-IMP-CIU-01: /impoundments/mine devuelve solo los vehículos del ciudadano logueado")
    void mine() throws Exception {
        String propio = register(CIUDADANO.nationalId());
        register(OTRO_CIUDADANO.nationalId());
        register("1");

        get("/impoundments/mine", CIUDADANO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].impoundmentId").value(propio))
                .andExpect(jsonPath("$[0].status").value("IMPOUNDED"));
        get("/impoundments/mine", OTRO_CIUDADANO).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        get("/impoundments/mine", DEPOSITO).andExpect(status().isForbidden());
        get("/impoundments/mine", null).andExpect(status().isUnauthorized());
    }
}
