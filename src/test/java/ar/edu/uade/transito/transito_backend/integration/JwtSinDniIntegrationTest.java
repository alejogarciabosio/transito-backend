package ar.edu.uade.transito.transito_backend.integration;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.CIUDADANO;
import static ar.edu.uade.transito.transito_backend.support.TestUsers.INSPECTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.JwtService;
import ar.edu.uade.transito.transito_backend.model.enums.Role;
import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import ar.edu.uade.transito.transito_backend.support.Payloads;

/** DEF-06: el JWT no transporta el DNI; el backend lo resuelve por subject y lo expone solo en /auth/me. */
@DisplayName("JWT sin DNI (DEF-06)")
class JwtSinDniIntegrationTest extends AbstractIntegrationTest {

    @Value("${jwt.secret}")
    private String secret;

    private int statusOf(String path, String bearer) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(API_PREFIX + path).header("Authorization", "Bearer " + bearer))
                .andReturn().getResponse().getStatus();
    }

    @Test
    @DisplayName("TC-BE-DEF06-01: login de ciudadano → token sin DNI y GET /auth/me devuelve su DNI")
    void loginAndMe() throws Exception {
        String token = read(post("/auth/login", null, "{\"username\":\"ciudadano1\",\"password\":\"prueba-ciudadano1\"}")
                .andExpect(status().isOk()).andReturn(), "$.token");
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        assertThat(payload).doesNotContain("30777888").doesNotContain("nationalId");

        mockMvc.perform(MockMvcRequestBuilders.get(API_PREFIX + "/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u-1007"))
                .andExpect(jsonPath("$.role").value("CIUDADANO"))
                .andExpect(jsonPath("$.nationalId").value("30777888"));
    }

    @Test
    @DisplayName("TC-BE-DEF06-02: /auth/me sin token → 401; inspector ve solo sus propios datos")
    void meRequiresAuth() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(API_PREFIX + "/auth/me")).andExpect(status().isUnauthorized());
        get("/auth/me", INSPECTOR).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u-1001"))
                .andExpect(jsonPath("$.role").value("INSPECTOR"));
    }

    @Test
    @DisplayName("TC-BE-DEF06-03: filtros del ciudadano (/mine) siguen usando su DNI resuelto en el servidor")
    void mineUsesServerSideDni() throws Exception {
        post("/infractions", INSPECTOR, Payloads.infraction(CIUDADANO.nationalId())).andExpect(status().isCreated());
        get("/infractions/mine", CIUDADANO).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].offenderNationalId").value(CIUDADANO.nationalId()));
    }

    @Test
    @DisplayName("TC-BE-DEF06-04: tras un reinicio, el stub resuelve el DNI por subject")
    void stubFallbackAfterRestart() throws Exception {
        String token = new JwtService(secret, 10).generateToken(new AuthenticatedUser("u-1007", "30777888", Role.CIUDADANO));
        assertThat(statusOf("/infractions/mine", token)).isEqualTo(200);
    }

    @Test
    @DisplayName("TC-BE-DEF06-05: ciudadano cuyo DNI no se puede resolver → 401 (vuelve a iniciar sesión)")
    void unresolvableCitizen() throws Exception {
        String token = new JwtService(secret, 10).generateToken(new AuthenticatedUser("u-desconocido", "1", Role.CIUDADANO));
        assertThat(statusOf("/infractions/mine", token)).isEqualTo(401);
    }
}
