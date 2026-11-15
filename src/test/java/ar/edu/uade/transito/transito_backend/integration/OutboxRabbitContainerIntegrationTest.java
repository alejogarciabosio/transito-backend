package ar.edu.uade.transito.transito_backend.integration;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.ADMIN;
import static ar.edu.uade.transito.transito_backend.support.TestUsers.INSPECTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.JwtService;
import ar.edu.uade.transito.transito_backend.event.OutboxRelay;
import ar.edu.uade.transito.transito_backend.model.OutboxEvent;
import ar.edu.uade.transito.transito_backend.model.enums.OutboxStatus;
import ar.edu.uade.transito.transito_backend.repository.OutboxEventRepository;
import ar.edu.uade.transito.transito_backend.support.AbstractBrokerIntegrationTest;
import ar.edu.uade.transito.transito_backend.support.Payloads;
import com.jayway.jsonpath.JsonPath;
import com.rabbitmq.client.GetResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Outbox → RabbitMQ real (Eje B/C): el relay publica en el exchange de Core con el mismo sobre que quedó en
 * la tabla, y con el broker caído nada se pierde: la fila queda PENDING y se publica una sola vez al volver.
 */
@DisplayName("Testcontainers: outbox publicando en RabbitMQ real")
class OutboxRabbitContainerIntegrationTest extends AbstractBrokerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired OutboxRelay relay;
    @Autowired OutboxEventRepository outbox;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void limpiarCaptura() {
        relay.relayPending(); // lo que hayan dejado otros tests no ensucia la cola de captura
        purge(CAPTURE_QUEUE);
    }

    private String bearer(AuthenticatedUser user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    /** Registra y confirma una infracción por la API real; devuelve la fila infractionConfirmed del outbox. */
    private OutboxEvent confirmarInfraccion() throws Exception {
        String body = mockMvc.perform(post("/api/v1/infractions").header("Authorization", bearer(INSPECTOR))
                        .contentType("application/json").content(Payloads.infraction("1")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.infractionId");
        mockMvc.perform(post("/api/v1/infractions/" + id + "/confirmation").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isOk());
        return outbox.findAll().stream()
                .filter(e -> e.getEventType().equals("infractionConfirmed") && e.getPayload().contains(id))
                .findFirst().orElseThrow();
    }

    private List<GetResponse> capturados(UUID eventId) {
        return drain(CAPTURE_QUEUE).stream()
                .filter(m -> objectMapper.readTree(body(m)).path("eventId").asText().equals(eventId.toString()))
                .toList();
    }

    @Test
    @DisplayName("TC-BE-TC-01: el relay publica infractionConfirmed en muni.inbox con el mismo sobre y la fila queda PUBLISHED")
    void publicaViaOutbox() throws Exception {
        OutboxEvent fila = confirmarInfraccion();
        assertThat(fila.getStatus()).isEqualTo(OutboxStatus.PENDING);

        relay.relayPending();

        OutboxEvent publicada = outbox.findById(fila.getEventId()).orElseThrow();
        assertThat(publicada.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(publicada.getPublishedAt()).isNotNull();

        List<GetResponse> mensajes = capturados(fila.getEventId());
        assertThat(mensajes).hasSize(1);
        GetResponse mensaje = mensajes.get(0);
        JsonNode sobre = objectMapper.readTree(body(mensaje));
        assertThat(body(mensaje)).isEqualTo(fila.getPayload()); // mismos bytes que se guardaron en el outbox
        assertThat(sobre.path("eventType").asText()).isEqualTo("infractionConfirmed");
        assertThat(sobre.path("correlationId").asText()).isEqualTo(fila.getCorrelationId().toString());
        assertThat(sobre.has("causationId")).isTrue();
        assertThat(sobre.path("data").isObject()).isTrue();
        assertThat(mensaje.getProps().getContentType()).isEqualTo("application/json");
        assertThat(mensaje.getProps().getDeliveryMode()).isEqualTo(2);
        assertThat(mensaje.getProps().getUserId()).isEqualTo(RABBIT.getAdminUsername());

        assertThat(relay.relayPending()).isZero(); // no se republica
        assertThat(capturados(fila.getEventId())).isEmpty();
    }

    @Test
    @DisplayName("TC-BE-TC-02: con RabbitMQ caído la confirmación responde y el evento queda PENDING; al volver se publica una sola vez")
    void brokerCaidoSinPerdida() throws Exception {
        OutboxEvent fila;
        stopBroker();
        try {
            fila = confirmarInfraccion(); // la API no depende del broker: el evento queda en el outbox
            relay.relayPending();
            relay.relayPending();
            OutboxEvent pendiente = outbox.findById(fila.getEventId()).orElseThrow();
            assertThat(pendiente.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(pendiente.getAttempts()).isGreaterThanOrEqualTo(1);
            assertThat(pendiente.getLastError()).isNotBlank();
            assertThat(pendiente.getPublishedAt()).isNull();
        } finally {
            startBroker();
        }

        UUID id = fila.getEventId();
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(300)).until(() -> {
            relay.relayPending();
            return outbox.findById(id).orElseThrow().getStatus() == OutboxStatus.PUBLISHED;
        });
        relay.relayPending();
        assertThat(capturados(fila.getEventId())).as("recibido exactamente una vez").hasSize(1);
    }
}
