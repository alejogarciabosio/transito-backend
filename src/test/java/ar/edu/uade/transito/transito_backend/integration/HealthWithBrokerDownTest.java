package ar.edu.uade.transito.transito_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Con el broker de Core inalcanzable, /actuator/health debe seguir respondiendo UP y rápido.
 * El valor de {@code management.health.rabbit.enabled} se toma del application.properties de producción
 * (src/main), porque el de test lo reemplaza en el classpath.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("/actuator/health con el broker caído")
class HealthWithBrokerDownTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) throws IOException {
        Properties main = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            main.load(in);
        }
        // Igual que producción: integración con Core activada (antes esto encendía el indicador de RabbitMQ).
        registry.add("CORE_RABBITMQ_ENABLED", () -> "true");
        registry.add("management.health.rabbit.enabled", () -> main.getProperty("management.health.rabbit.enabled"));
        // Broker inalcanzable: puerto local cerrado, sin TLS.
        registry.add("spring.rabbitmq.host", () -> "127.0.0.1");
        registry.add("spring.rabbitmq.port", () -> "1");
        registry.add("spring.rabbitmq.ssl.enabled", () -> "false");
    }

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("responde 200 UP en menos de 2 s aunque RabbitMQ no esté disponible")
    void healthIsUpFastWithBrokerDown() throws Exception {
        long start = System.nanoTime();
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertThat(millis).as("duración de /actuator/health").isLessThan(2000);
    }
}
