package ar.edu.uade.transito.transito_backend.integration;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;

/**
 * KI-2: el límite de intentos de login no debe evadirse rotando X-Forwarded-For.
 * El proxy de Railway agrega la IP real al final del header; lo de la izquierda lo controla el cliente.
 * Defaults: 10 intentos / 60 s, 1 proxy de confianza.
 */
class LoginRateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final int MAX = 10;

    private ResultActions login(String username, String password, String xff) throws Exception {
        var builder = MockMvcRequestBuilders.post(API_PREFIX + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        if (xff != null) {
            builder.header("X-Forwarded-For", xff);
        }
        return mockMvc.perform(builder);
    }

    @Test
    @DisplayName("KI-2: XFF falso inyectado a la izquierda no evade el límite por IP (se usa la entrada del proxy)")
    void spoofedLeftmostXffStillLimited() throws Exception {
        // Usuarios distintos en cada intento para aislar el límite por IP del límite por username.
        for (int i = 0; i < MAX; i++) {
            login("noexiste" + i, "mala", "10.0.0." + i + ", 200.1.1.1").andExpect(status().isUnauthorized());
        }
        login("noexiste-final", "mala", "10.9.9.9, 200.1.1.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too Many Requests"));
    }

    @Test
    @DisplayName("KI-2: mismo username desde IPs reales distintas → 429 (límite por cuenta)")
    void sameUsernameFromDifferentIpsLimited() throws Exception {
        for (int i = 0; i < MAX; i++) {
            login("ciudadano1", "mala" + i, "181.0.0." + i).andExpect(status().isUnauthorized());
        }
        // Ni siquiera la contraseña correcta pasa mientras la cuenta está bloqueada.
        login("ciudadano1", "prueba-ciudadano1", "181.0.0.200")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    @DisplayName("KI-2: login correcto de otro usuario no queda bloqueado por la fuerza bruta contra una cuenta")
    void otherUserNotBlocked() throws Exception {
        for (int i = 0; i < MAX; i++) {
            login("ciudadano1", "mala" + i, "181.0.0." + i).andExpect(status().isUnauthorized());
        }
        login("ciudadano1", "mala", "181.0.0.250").andExpect(status().isTooManyRequests());
        login("inspector1", "prueba-inspector1", "190.0.0.1").andExpect(status().isOk());
    }

    @Test
    @DisplayName("KI-2: sin X-Forwarded-For se usa la IP remota")
    void withoutXffUsesRemoteAddr() throws Exception {
        for (int i = 0; i < MAX; i++) {
            login("anon" + i, "mala", null).andExpect(status().isUnauthorized());
        }
        login("anon-final", "mala", null).andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("KI-2: login correcto limpia el contador de la cuenta")
    void successResetsUsernameCounter() throws Exception {
        for (int i = 0; i < MAX - 1; i++) {
            login("ciudadano1", "mala" + i, "181.0.1." + i).andExpect(status().isUnauthorized());
        }
        login("ciudadano1", "prueba-ciudadano1", "181.0.1.100").andExpect(status().isOk());
        for (int i = 0; i < MAX - 1; i++) {
            login("ciudadano1", "mala" + i, "181.0.2." + i).andExpect(status().isUnauthorized());
        }
    }
}
