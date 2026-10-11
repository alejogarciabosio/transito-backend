package ar.edu.uade.transito.transito_backend.integration;

import ar.edu.uade.transito.transito_backend.model.ParkingSession;
import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import ar.edu.uade.transito.transito_backend.support.Payloads;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Estacionamiento medido — zonas, tarifas y sesiones")
class ParkingIntegrationTest extends AbstractIntegrationTest {

    private String createZone(String name) throws Exception {
        return read(post("/parking-zones", ADMIN, Payloads.zone(name)).andExpect(status().isCreated()).andReturn(), "$.zoneId");
    }

    private String createTariff(String zoneId) throws Exception {
        return read(post("/parking-tariffs", ADMIN, Payloads.tariff(zoneId)).andExpect(status().isCreated()).andReturn(), "$.tariffId");
    }

    private String startSession(String zoneId) throws Exception {
        return read(post("/parking-sessions", CIUDADANO, Payloads.session(zoneId)).andExpect(status().isCreated()).andReturn(), "$.sessionId");
    }

    @Nested
    @DisplayName("Zonas (/parking-zones)")
    class Zones {

        @Test
        @DisplayName("TC-BE-PZ-01: ADMIN crea zona → 201 y nace ACTIVE")
        void create() throws Exception {
            post("/parking-zones", ADMIN, Payloads.zone("Microcentro"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.zoneId", notNullValue()))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.allowedHours").value("08:00-20:00"));
        }

        @Test
        @DisplayName("TC-BE-PZ-02: solo ADMIN_MUNICIPAL crea/edita/borra → INSPECTOR 403")
        void forbidden() throws Exception {
            String id = createZone("Z");
            post("/parking-zones", INSPECTOR, Payloads.zone("X")).andExpect(status().isForbidden());
            put("/parking-zones/" + id, INSPECTOR, "{\"name\":\"X\",\"allowedHours\":\"1\",\"status\":\"ACTIVE\"}").andExpect(status().isForbidden());
            delete("/parking-zones/" + id, CIUDADANO).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TC-BE-PZ-03: GET de zonas es público (sin token)")
        void publicGet() throws Exception {
            String id = createZone("Palermo");
            get("/parking-zones", null).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
            get("/parking-zones/" + id, null).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Palermo"));
            get("/parking-zones/" + UUID.randomUUID(), null).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("TC-BE-PZ-04: PUT actualiza nombre, horario y estado")
        void update() throws Exception {
            String id = createZone("Z");
            put("/parking-zones/" + id, ADMIN, "{\"name\":\"Z2\",\"boundaries\":\"b\",\"allowedHours\":\"00-24\",\"status\":\"INACTIVE\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Z2"))
                    .andExpect(jsonPath("$.status").value("INACTIVE"));
        }

        @Test
        @DisplayName("TC-BE-PZ-05: DELETE es baja lógica → 204 y la zona queda INACTIVE (sigue existiendo)")
        void logicalDelete() throws Exception {
            String id = createZone("Z");
            delete("/parking-zones/" + id, ADMIN).andExpect(status().isNoContent());
            get("/parking-zones/" + id, null).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
            delete("/parking-zones/" + UUID.randomUUID(), ADMIN).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Tarifas (/parking-tariffs)")
    class Tariffs {

        @Test
        @DisplayName("TC-BE-PT-01: ADMIN crea tarifa y se consulta públicamente por zona e id")
        void createAndQuery() throws Exception {
            String zone = createZone("Z");
            String other = createZone("Otra");
            String tariff = createTariff(zone);
            createTariff(other);
            get("/parking-tariffs?zoneId=" + zone, null).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].pricePerHour").value(1500));
            get("/parking-tariffs/" + tariff, null).andExpect(status().isOk()).andExpect(jsonPath("$.validTo").value("2026-12-31"));
            get("/parking-tariffs/" + UUID.randomUUID(), null).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("TC-BE-PT-02: GET sin zoneId → 400")
        void missingZoneId() throws Exception {
            get("/parking-tariffs", null).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-BE-PT-03: PUT actualiza precio y vigencia")
        void update() throws Exception {
            String tariff = createTariff(createZone("Z"));
            put("/parking-tariffs/" + tariff, ADMIN, "{\"pricePerHour\":2000,\"validFrom\":\"2026-02-01\",\"validTo\":null}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricePerHour").value(2000))
                    .andExpect(jsonPath("$.validTo", nullValue()));
            put("/parking-tariffs/" + tariff, OPERADOR, "{\"pricePerHour\":1}").andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TC-BE-PT-04: borrar tarifa sin sesiones abiertas en la zona → 204 (baja física)")
        void deleteWithoutOpenSessions() throws Exception {
            String zone = createZone("Z");
            String tariff = createTariff(zone);
            String session = startSession(zone);
            patch("/parking-sessions/" + session + "/close", CIUDADANO, null).andExpect(status().isOk());
            delete("/parking-tariffs/" + tariff, ADMIN).andExpect(status().isNoContent());
            get("/parking-tariffs/" + tariff, null).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("TC-BE-PT-05: borrar tarifa con sesión abierta en la zona → 409")
        void deleteWithOpenSessions() throws Exception {
            String zone = createZone("Z");
            String tariff = createTariff(zone);
            startSession(zone);
            delete("/parking-tariffs/" + tariff, ADMIN).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("sesiones activas")));
        }
    }

    @Nested
    @DisplayName("Sesiones (/parking-sessions)")
    class Sessions {

        @Test
        @DisplayName("TC-BE-PS-01: CIUDADANO u OPERADOR inicia sesión → 201, impaga y abierta (ignora paid/endDateTime del cliente)")
        void start() throws Exception {
            String zone = createZone("Z");
            String body = Payloads.session(zone).replace("{", "{\"paid\":true,\"endDateTime\":\"2026-09-20T10:00:00\",\"paymentReference\":\"X\",");
            post("/parking-sessions", CIUDADANO, body)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paid").value(false))
                    .andExpect(jsonPath("$.endDateTime", nullValue()))
                    .andExpect(jsonPath("$.paymentReference", nullValue()));
            post("/parking-sessions", OPERADOR, Payloads.session(zone)).andExpect(status().isCreated());
            post("/parking-sessions", INSPECTOR, Payloads.session(zone)).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TC-BE-PS-02: listado para INSPECTOR/OPERADOR/SUPERVISOR; detalle para INSPECTOR/CIUDADANO")
        void queries() throws Exception {
            String id = startSession(createZone("Z"));
            get("/parking-sessions", INSPECTOR).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
            get("/parking-sessions", SUPERVISOR).andExpect(status().isOk());
            get("/parking-sessions", CIUDADANO).andExpect(status().isForbidden());
            get("/parking-sessions/" + id, CIUDADANO).andExpect(status().isOk());
            get("/parking-sessions/" + id, AUDITOR).andExpect(status().isForbidden());
            get("/parking-sessions/" + UUID.randomUUID(), INSPECTOR).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("TC-BE-PS-03: cerrar sesión → endDateTime seteado; cerrar dos veces → 409")
        void close() throws Exception {
            String id = startSession(createZone("Z"));
            patch("/parking-sessions/" + id + "/close", OPERADOR, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.endDateTime", notNullValue()));
            patch("/parking-sessions/" + id + "/close", CIUDADANO, null).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("TC-BE-PS-05: no se marca impaga una sesión paga → 409; solo INSPECTOR → 403")
        void flagUnpaidRules() throws Exception {
            String id = startSession(createZone("Z"));
            post("/parking-sessions/" + id + "/flag-unpaid", OPERADOR, "{\"baseAmount\":1}").andExpect(status().isForbidden());

            ParkingSession session = parkingSessionRepository.findById(UUID.fromString(id)).orElseThrow();
            session.setPaid(true);
            parkingSessionRepository.save(session);
            post("/parking-sessions/" + id + "/flag-unpaid", INSPECTOR, "{\"baseAmount\":1}").andExpect(status().isConflict());
            assertThat(events.all()).isEmpty();
        }
    }
}
