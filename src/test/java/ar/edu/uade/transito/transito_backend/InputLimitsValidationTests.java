package ar.edu.uade.transito.transito_backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.JwtService;
import ar.edu.uade.transito.transito_backend.model.enums.Role;

/** Hallazgos de OWASP ZAP: textos de más de 255 caracteres o con NUL respondían 500; deben ser 400 Problem Details. */
@SpringBootTest
@AutoConfigureMockMvc
class InputLimitsValidationTests {

    private static final String LARGO = "x".repeat(256);
    private static final String NUL = "\\u0000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private String token;

    @BeforeEach
    void setUp() {
        token = jwtService.generateToken(new AuthenticatedUser("inspector-limites", "30999888", Role.INSPECTOR));
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String accidente(String street, String crossStreet, String plate, String nationalId, String ticket) {
        return """
                {"accidentDateTime":"2026-10-06T15:30:00","location":{"street":"%s","crossStreet":"%s"},
                 "involvesInfrastructureDamage":false,
                 "vehicles":[{"licensePlate":"%s"}],
                 "persons":[{"nationalId":"%s","injuryLevel":"NONE"}],
                 "originTicketId":"%s"}
                """.formatted(street, crossStreet, plate, nationalId, ticket);
    }

    private static String operativo(String inspector, String ticket) {
        return """
                {"type":"ROUTINE_CONTROL","zone":"Centro","startDateTime":"2026-10-06T10:00:00",
                 "assignedInspectors":["%s"],"assignedUnits":["movil-1"],"ticketId":"%s"}
                """.formatted(inspector, ticket);
    }

    private static void esValidacion(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void accidenteConCampoMayorA255DevuelveValidationError() throws Exception {
        esValidacion(postJson("/api/v1/accidents", accidente(LARGO, "Mitre", "AB123CD", "30111222", "T-1")));
        esValidacion(postJson("/api/v1/accidents", accidente("Rivadavia", LARGO, "AB123CD", "30111222", "T-1")));
        esValidacion(postJson("/api/v1/accidents", accidente("Rivadavia", "Mitre", LARGO, "30111222", "T-1")));
        esValidacion(postJson("/api/v1/accidents", accidente("Rivadavia", "Mitre", "AB123CD", LARGO, "T-1")));
        postJson("/api/v1/accidents", accidente("Rivadavia", "Mitre", "AB123CD", "30111222", LARGO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.originTicketId").exists());
    }

    @Test
    void accidenteCon255CaracteresSeAcepta() throws Exception {
        postJson("/api/v1/accidents", accidente("x".repeat(255), "Mitre", "AB123CD", "30111222", "T-1"))
                .andExpect(status().isCreated());
    }

    @Test
    void operativoConCampoMayorA255DevuelveValidationError() throws Exception {
        esValidacion(postJson("/api/v1/operations", operativo(LARGO, "T-1")));
        esValidacion(postJson("/api/v1/operations", operativo("inspector-1", LARGO)));
    }

    @Test
    void caracterNulEnElCuerpoDevuelve400() throws Exception {
        esValidacion(postJson("/api/v1/accidents", accidente("Riva" + NUL + "davia", "Mitre", "AB123CD", "30111222", "T-1")));
        esValidacion(postJson("/api/v1/operations", operativo("insp" + NUL, "T-1")));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"inspector1" + NUL + "\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
