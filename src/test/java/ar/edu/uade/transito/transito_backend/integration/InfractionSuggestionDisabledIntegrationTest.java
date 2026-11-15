package ar.edu.uade.transito.transito_backend.integration;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.INSPECTOR;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ar.edu.uade.transito.transito_backend.ai.AiGateway;
import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;

/** Configuración por defecto (AI_ENABLED sin definir → false): no se llama a nadie. */
@DisplayName("Sugerencias con la IA apagada (por defecto)")
class InfractionSuggestionDisabledIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean AiGateway gateway;

    @Test
    void disabledByDefaultReturnsManualWithoutCalling() throws Exception {
        post("/infractions/suggestions", INSPECTOR, "{\"description\":\"auto en doble fila\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.infractionType").doesNotExist());
        verify(gateway, never()).complete(anyString(), anyString());
    }
}
