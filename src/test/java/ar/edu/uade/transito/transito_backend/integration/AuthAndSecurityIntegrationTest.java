package ar.edu.uade.transito.transito_backend.integration;

import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.INSPECTOR;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Autenticación, JWT y seguridad transversal")
class AuthAndSecurityIntegrationTest extends AbstractIntegrationTest {

    @ParameterizedTest(name = "TC-BE-AUTH-01: login {0} → rol {1}")
    @CsvSource({
            "inspector1,inspector123,INSPECTOR",
            "supervisor1,supervisor123,SUPERVISOR",
            "auditor1,auditor123,AUDITOR",
            "operador1,operador123,OPERADOR_CONTROL",
            "deposito1,deposito123,PERSONAL_DEPOSITO",
            "admin1,admin123,ADMIN_MUNICIPAL",
            "ciudadano1,ciudadano123,CIUDADANO"})
    void loginOk(String user, String pass, String role) throws Exception {
        var result = post("/auth/login", null, "{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.role").value(role))
                .andExpect(jsonPath("$.expiresInSeconds").value(7200))
                .andReturn();
        // el token devuelto sirve para llamar endpoints protegidos
        String token = read(result, "$.token");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(API_PREFIX + "/infractions/mine").header("Authorization", "Bearer " + token))
                .andExpect(role.equals("CIUDADANO") ? status().isOk() : status().isForbidden());
    }

    @ParameterizedTest(name = "TC-BE-AUTH-02: credenciales inválidas ({0}/{1}) → 401 en Problem Details")
    @CsvSource({"inspector1,mal", "noexiste,inspector123", "INSPECTOR1,inspector123"})
    void loginFails(String user, String pass) throws Exception {
        post("/auth/login", null, "{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Usuario o contraseña inválidos"));
    }

    @Test
    @DisplayName("TC-BE-SEC-01: /actuator/health es público")
    void health() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("TC-BE-SEC-02: endpoint protegido sin token → 401 (sin credenciales)")
    void noToken() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(API_PREFIX + "/infractions")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-BE-SEC-03: token mal formado / firmado con otra clave / sin 'Bearer' → 401; rol insuficiente → 403")
    void invalidTokens() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(API_PREFIX + "/infractions").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized());
        String valid = token(INSPECTOR);
        String tampered = valid.substring(0, valid.length() - 3) + "abc";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(API_PREFIX + "/infractions").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(API_PREFIX + "/infractions").header("Authorization", valid))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(API_PREFIX + "/infractions").header("Authorization", "Bearer " + valid))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BE-SEC-04: token con rol inexistente → 401 (no 500)")
    void unknownRoleClaim() throws Exception {
        String forged = io.jsonwebtoken.Jwts.builder().subject("u").claim("nationalId", "1").claim("role", "HACKER")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "test-secret-de-al-menos-32-bytes-para-hs256-0123456789".getBytes()))
                .compact();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(API_PREFIX + "/infractions").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-BE-SEC-05: CORS — origen permitido recibe Access-Control-Allow-Origin; otro origen → 403")
    void cors() throws Exception {
        mockMvc.perform(options(API_PREFIX + "/infractions")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mockMvc.perform(options(API_PREFIX + "/infractions")
                        .header("Origin", "http://evil.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
