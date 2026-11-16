package ar.edu.uade.transito.transito_backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.JwtService;
import ar.edu.uade.transito.transito_backend.model.ParkingZone;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.ParkingZoneStatus;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.model.enums.Role;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;
import ar.edu.uade.transito.transito_backend.repository.ParkingZoneRepository;
import ar.edu.uade.transito.transito_backend.repository.StreetClosureRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ApiWorkflowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ParkingZoneRepository parkingZoneRepository;

    @Autowired
    private StreetClosureRepository streetClosureRepository;

    @Test
    void operationCompletesItsHttpLifecycle() throws Exception {
        String inspector = token("inspector-api", "30111111", Role.INSPECTOR);
        MvcResult created = mockMvc.perform(post("/api/v1/operations")
                        .header("Authorization", inspector)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"ROUTINE_CONTROL",
                                  "zone":"Centro",
                                  "startDateTime":"2026-10-10T20:00:00",
                                  "assignedInspectors":["inspector-api"],
                                  "assignedUnits":["movil-1"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        UUID id = idFromLocation(created);

        mockMvc.perform(patch("/api/v1/operations/{id}", id)
                        .header("Authorization", inspector)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zone":"Norte","assignedUnits":["movil-2"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zone").value("Norte"));

        mockMvc.perform(post("/api/v1/operations/{id}/closure", id)
                        .header("Authorization", inspector)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolution\":\"NO_ACTION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void parkingZoneAndTariffCompleteAdministrativeLifecycle() throws Exception {
        String admin = token("admin-api", "30222222", Role.ADMINISTRADOR);
        MvcResult zoneCreated = mockMvc.perform(post("/api/v1/parking-zones")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Zona QA","boundaries":"A-B-C-D","allowedHours":"08:00-20:00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        UUID zoneId = idFromLocation(zoneCreated);

        MvcResult tariffCreated = mockMvc.perform(post("/api/v1/parking-tariffs")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "zoneId":"%s",
                                  "pricePerHour":1500,
                                  "validFrom":"2026-10-01",
                                  "validTo":"2026-12-31"
                                }
                                """.formatted(zoneId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        UUID tariffId = idFromLocation(tariffCreated);

        mockMvc.perform(put("/api/v1/parking-tariffs/{id}", tariffId)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pricePerHour":1800,"validFrom":"2026-10-01","validTo":"2027-01-31"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricePerHour").value(1800));

        mockMvc.perform(delete("/api/v1/parking-tariffs/{id}", tariffId)
                        .header("Authorization", admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/parking-zones/{id}", zoneId)
                        .header("Authorization", admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void vehicleImpoundmentCompletesDepositLifecycle() throws Exception {
        String inspector = token("inspector-deposito", "30333333", Role.INSPECTOR);
        String deposit = token("deposito-api", "30444444", Role.ADMINISTRADOR);
        MvcResult created = mockMvc.perform(post("/api/v1/impoundments")
                        .header("Authorization", inspector)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "licensePlate":"AC456EF",
                                  "ownerNationalId":"40123456",
                                  "reason":"Falta grave",
                                  "depotLocation":"Depósito Norte",
                                  "entryDateTime":"2026-10-10T19:00:00",
                                  "originType":"INFRACTION",
                                  "originId":"%s"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IMPOUNDED"))
                .andReturn();
        UUID id = idFromLocation(created);

        mockMvc.perform(patch("/api/v1/impoundments/{id}", id)
                        .header("Authorization", deposit)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"depotLocation\":\"Depósito Sur\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depotLocation").value("Depósito Sur"));

        mockMvc.perform(post("/api/v1/impoundments/{id}/release", id)
                        .header("Authorization", deposit)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verifiedRequirements":["DOCUMENTATION_OK"],
                                  "towingAndStorageFee":5000,
                                  "paymentReference":"PAY-QA-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"))
                .andExpect(jsonPath("$.releasedBy").value("deposito-api"));
    }

    @Test
    void streetClosureCompletesItsHttpLifecycle() throws Exception {
        StreetClosure closure = streetClosureRepository.save(StreetClosure.builder()
                .closureRequestId("REQ-" + UUID.randomUUID())
                .sourceEventId(UUID.randomUUID())
                .sourceCorrelationId(UUID.randomUUID())
                .sourceRef("WO-API-1")
                .reason("Reparación de calzada")
                .requestedAt(OffsetDateTime.now())
                .requestingModule(RequestingModule.OBRAS)
                // DEF-42: la activación exige estar dentro de la ventana (este test usa el reloj real)
                .startDate(OffsetDateTime.now().minusHours(1))
                .endDate(OffsetDateTime.now().plusDays(1))
                .affectedStreet(List.of("Av. Corrientes"))
                .status(StreetClosureStatus.REQUESTED)
                .build());
        String admin = token("admin-corte", "30555555", Role.ADMINISTRADOR);
        String operator = token("operador-corte", "30666666", Role.INSPECTOR);

        mockMvc.perform(post("/api/v1/street-closures/{id}/approval", closure.getStreetClosureId())
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"detours\":[],\"conditions\":[\"Acceso peatonal\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(post("/api/v1/street-closures/{id}/activation", closure.getStreetClosureId())
                        .header("Authorization", operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(post("/api/v1/street-closures/{id}/completion", closure.getStreetClosureId())
                        .header("Authorization", operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Calle liberada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void citizenCannotCreateInfraction() throws Exception {
        String citizen = token("ciudadano-forbidden", "40999999", Role.CIUDADANO);

        mockMvc.perform(post("/api/v1/infractions")
                        .header("Authorization", citizen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "licensePlate":"AB123CD",
                                  "infractionType":"ILLEGAL_PARKING",
                                  "infractionDateTime":"2026-10-10T12:00:00",
                                  "location":{"street":"Av. Cabildo"},
                                  "evidenceUrl":["https://evidencias/foto1.jpg"],
                                  "baseAmount":1000,
                                  "aggravatingFactors":[],
                                  "finalAmount":1000
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private String token(String userId, String nationalId, Role role) {
        return "Bearer " + jwtService.generateToken(new AuthenticatedUser(userId, nationalId, role));
    }

    private UUID idFromLocation(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }
}
