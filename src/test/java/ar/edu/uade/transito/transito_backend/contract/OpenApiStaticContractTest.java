package ar.edu.uade.transito.transito_backend.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;

/**
 * El contrato estático ({@code /openapi.yaml}, servido desde {@code static/}) y el {@code openapi.yaml} de la raíz
 * deben coincidir con lo que genera springdoc y con las rutas reales de los controladores.
 * Si falla: regenerar el YAML desde {@code /v3/api-docs} (ver README, sección OpenAPI).
 */
@DisplayName("Contrato OpenAPI estático alineado con springdoc y los controladores")
class OpenApiStaticContractTest extends AbstractIntegrationTest {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete", "head", "options");

    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping;

    @SuppressWarnings("unchecked")
    private static Map<String, Set<String>> operations(Map<String, Object> spec) {
        Map<String, Set<String>> ops = new TreeMap<>();
        ((Map<String, Object>) spec.get("paths")).forEach((path, item) -> {
            Set<String> methods = new TreeSet<>();
            ((Map<String, Object>) item).keySet().stream().filter(HTTP_METHODS::contains).forEach(methods::add);
            ops.put(path, methods);
        });
        return ops;
    }

    private static Map<String, Object> staticSpec() throws Exception {
        try (InputStream in = OpenApiStaticContractTest.class.getResourceAsStream("/static/openapi.yaml")) {
            assertThat(in).as("static/openapi.yaml en el classpath").isNotNull();
            return new Yaml().load(in);
        }
    }

    @Test
    @DisplayName("static/openapi.yaml tiene exactamente las mismas rutas y métodos que /v3/api-docs")
    void staticMatchesSpringdoc() throws Exception {
        String json = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> springdoc = new Yaml().load(json); // JSON es YAML válido
        assertThat(operations(staticSpec())).isEqualTo(operations(springdoc));
        assertThat(operations(staticSpec())).containsKeys("/api/v1/auth/me", "/api/v1/admin/outbox");
    }

    @Test
    @DisplayName("cada ruta /api/v1 de los controladores figura en el contrato estático con su método")
    void staticContainsEveryControllerRoute() throws Exception {
        Map<String, Set<String>> documented = operations(staticSpec());
        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
            for (String pattern : info.getPatternValues()) {
                if (!pattern.startsWith("/api/v1")) {
                    continue;
                }
                assertThat(documented).as("ruta %s (%s)", pattern, handler).containsKey(pattern);
                for (RequestMethod m : info.getMethodsCondition().getMethods()) {
                    assertThat(documented.get(pattern)).as("%s %s", m, pattern).contains(m.name().toLowerCase());
                }
            }
        });
    }

    @Test
    @DisplayName("openapi.yaml de la raíz es idéntico al que se sirve en /openapi.yaml")
    void rootFileEqualsStaticFile() throws Exception {
        assertThat(Files.readString(Path.of("openapi.yaml")))
                .isEqualTo(Files.readString(Path.of("src/main/resources/static/openapi.yaml")));
    }
}
