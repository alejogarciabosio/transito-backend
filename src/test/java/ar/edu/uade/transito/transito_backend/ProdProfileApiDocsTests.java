package ar.edu.uade.transito.transito_backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Con el perfil prod, la API generada (springdoc) y Swagger UI no se publican; el contrato estático sí. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProdProfileApiDocsTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsNoSePublicanEnProd() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }

    @Test
    void swaggerUiNoSePublicaEnProd() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }

    @Test
    void contratoEstaticoSigueDisponible() throws Exception {
        mockMvc.perform(get("/openapi.yaml")).andExpect(status().isOk());
    }
}
