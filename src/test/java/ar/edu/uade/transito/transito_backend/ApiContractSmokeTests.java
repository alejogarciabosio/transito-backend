package ar.edu.uade.transito.transito_backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.JwtService;
import ar.edu.uade.transito.transito_backend.model.enums.Role;

@SpringBootTest
@AutoConfigureMockMvc
class ApiContractSmokeTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void loginUsesVersionedEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"inspector1","password":"inspector123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("INSPECTOR"))
                .andExpect(jsonPath("$.expiresInSeconds").value(7200));
    }

    @Test
    void invalidCredentialsReturnProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"inspector1","password":"incorrecta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value("/api/v1/auth/login"));
    }

    @Test
    void invalidRequestReturnsFieldValidationProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://transito.uade.edu.ar/problems/validation-error"))
                .andExpect(jsonPath("$.errors.password").value("La contraseña es obligatoria"));
    }

    @Test
    void publicContractAndParkingZonesAreAvailable() throws Exception {
        mockMvc.perform(get("/openapi.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("openapi: 3.0.3")));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API de Tránsito Municipal"))
                .andExpect(jsonPath("$.paths['/api/v1/street-closures']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/impoundments']").exists());

        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urls[0].url").value("/v3/api-docs"))
                .andExpect(jsonPath("$.urls[1].url").value("/openapi.yaml"));

        mockMvc.perform(get("/api/v1/parking-zones"))
                .andExpect(status().isOk());
    }

    @Test
    void creatingInfractionUsesJwtInspectorAndReturnsLocation() throws Exception {
        String token = jwtService.generateToken(
                new AuthenticatedUser("u-1001", "30111222", Role.INSPECTOR));

        mockMvc.perform(post("/api/v1/infractions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "licensePlate": "AB123CD",
                                  "infractionType": "ILLEGAL_PARKING",
                                  "infractionDateTime": "2026-10-06T14:30:00",
                                  "location": {"street": "Av. Cabildo"},
                                  "evidenceUrl": [],
                                  "offenderNationalId": "40123456",
                                  "debtorId": "40123456",
                                  "debtorIdType": "DNI",
                                  "baseAmount": 50000,
                                  "aggravatingFactors": [],
                                  "finalAmount": 50000
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.matchesPattern("/api/v1/infractions/[0-9a-f-]+")))
                .andExpect(jsonPath("$.inspectorId").value("u-1001"))
                .andExpect(jsonPath("$.status").value("PENDING_APPEAL"));
    }

    @Test
    void creatingAccidentIgnoresSpoofedInspectorAndUsesJwtIdentity() throws Exception {
        String token = jwtService.generateToken(
                new AuthenticatedUser("inspector-jwt-2", "30222333", Role.INSPECTOR));

        mockMvc.perform(post("/api/v1/accidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accidentDateTime": "2026-10-06T15:30:00",
                                  "location": {"street": "Av. Rivadavia"},
                                  "involvesInfrastructureDamage": false,
                                  "vehicles": [],
                                  "persons": [],
                                  "inspectorId": "usuario-falsificado"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inspectorId").value("inspector-jwt-2"))
                .andExpect(jsonPath("$.status").value("PRELIMINARY"));
    }
}
