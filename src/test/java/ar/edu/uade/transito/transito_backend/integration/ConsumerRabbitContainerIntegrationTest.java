package ar.edu.uade.transito.transito_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ar.edu.uade.transito.transito_backend.repository.ProcessedEventRepository;
import ar.edu.uade.transito.transito_backend.repository.StreetClosureRepository;
import ar.edu.uade.transito.transito_backend.support.AbstractBrokerIntegrationTest;
import com.rabbitmq.client.GetResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Consumidor real ({@code RabbitMqIncomingEventListener}) leyendo de {@code q.transito} en RabbitMQ:
 * idempotencia por eventId (tabla processed_events) y rechazo sin requeue hacia la DLQ.
 *
 * <p><b>DLQ:</b> en producción la cola {@code q.transito}, su dead-letter exchange y la DLQ los declara y
 * gestiona <b>Core</b> (Tránsito no redeclara colas de Core). Este test declara {@code q.transito} con
 * {@code x-dead-letter-exchange} solo para demostrar que el rechazo de la app (basic.reject requeue=false)
 * termina en la DLQ cuando la cola tiene DLX, como en Core.
 */
@DisplayName("Testcontainers: consumidor idempotente y DLQ con RabbitMQ real")
@ExtendWith(OutputCaptureExtension.class)
class ConsumerRabbitContainerIntegrationTest extends AbstractBrokerIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Autowired ProcessedEventRepository processedEvents;
    @Autowired StreetClosureRepository closures;

    @BeforeEach
    void limpiarDlq() {
        purge(DLQ);
    }

    private static String closureRequested(UUID eventId, String closureRequestId, String sourceModule) {
        return "{\"eventId\":\"" + eventId + "\",\"correlationId\":\"" + UUID.randomUUID() + "\","
                + "\"eventType\":\"streetClosureRequested\",\"eventVersion\":\"1.0\","
                + "\"occurredAt\":\"2026-09-01T15:58:00-03:00\",\"sourceModule\":\"" + sourceModule + "\","
                + "\"data\":{\"closureRequestId\":\"" + closureRequestId + "\","
                + "\"sourceModule\":\"" + sourceModule + "\",\"sourceRef\":\"INT-025\","
                + "\"reason\":\"Intervención planificada\","
                + "\"affectedSections\":[{\"street\":\"Av. Rivadavia\",\"from\":\"Av. La Plata\",\"to\":\"Muñiz\",\"direction\":\"BOTH\"}],"
                + "\"requestedFrom\":\"2026-10-01T00:00:00-03:00\",\"requestedTo\":\"2026-10-02T00:00:00-03:00\","
                + "\"requestedAt\":\"2026-09-01T15:58:00-03:00\"}}";
    }

    /**
     * Mensaje válido que se publica al final: el listener consume en orden con un único consumidor, así que
     * cuando este quedó procesado también se procesaron (o rechazaron) todos los anteriores.
     */
    private UUID publicarCentinelaYEsperar() {
        UUID centinela = UUID.randomUUID();
        publishToTransitoQueue(closureRequested(centinela, "REQ-CENT-" + centinela, "obras"));
        await().atMost(TIMEOUT).pollInterval(Duration.ofMillis(200)).until(() -> processedEvents.existsById(centinela));
        return centinela;
    }

    @Test
    @DisplayName("TC-BE-TC-03: el mismo streetClosureRequested (mismo eventId) publicado dos veces crea un solo corte")
    void consumoIdempotente() {
        UUID eventId = UUID.randomUUID();
        String closureRequestId = "REQ-DUP-" + eventId;
        String mensaje = closureRequested(eventId, closureRequestId, "obras");

        publishToTransitoQueue(mensaje);
        publishToTransitoQueue(mensaje);
        publicarCentinelaYEsperar();

        assertThat(processedEvents.existsById(eventId)).isTrue();
        assertThat(closures.findByClosureRequestId(closureRequestId)).hasSize(1);
        assertThat(drain(DLQ)).as("el duplicado se descarta con ack, no va a la DLQ").isEmpty();
    }

    @Test
    @DisplayName("TC-BE-TC-04: JSON roto y payload inválido se rechazan sin requeue, terminan en la DLQ y se sigue consumiendo")
    void mensajeInvalidoVaALaDlq() {
        String jsonRoto = "{\"eventId\": esto no es JSON";
        UUID invalidoId = UUID.randomUUID();
        // sourceModule desconocido: el handler lo rechaza por validación (IllegalArgumentException).
        String payloadInvalido = closureRequested(invalidoId, "REQ-INV-" + invalidoId, "M9");

        // El JSON roto lo rechaza el conversor del contenedor antes del listener (MessageConversionException,
        // fatal para ConditionalRejectingErrorHandler); el payload inválido lo rechaza el listener
        // (AmqpRejectAndDontRequeueException). En ambos casos: reject sin requeue → DLX → DLQ.
        publishToTransitoQueue(jsonRoto);
        publishToTransitoQueue(payloadInvalido);
        publicarCentinelaYEsperar(); // la app no quedó trabada: consumió el siguiente válido

        await().atMost(TIMEOUT).pollInterval(Duration.ofMillis(200))
                .until(() -> withChannel(ch -> ch.messageCount(DLQ)) >= 2);
        List<GetResponse> muertos = drain(DLQ);
        assertThat(muertos).extracting(AbstractBrokerIntegrationTest::body)
                .containsExactly(jsonRoto, payloadInvalido);
        for (GetResponse muerto : muertos) {
            @SuppressWarnings("unchecked")
            Map<String, Object> death = ((List<Map<String, Object>>) muerto.getProps().getHeaders().get("x-death")).get(0);
            assertThat(death.get("reason")).hasToString("rejected");
            assertThat(death.get("queue")).hasToString(TRANSITO_QUEUE);
        }
        assertThat(processedEvents.existsById(invalidoId)).isFalse();
        assertThat(closures.findByClosureRequestId("REQ-INV-" + invalidoId)).isEmpty();
        long pendientes = withChannel(ch -> ch.messageCount(TRANSITO_QUEUE));
        assertThat(pendientes).as("nada reencolado").isZero();
    }

    @Test
    @DisplayName("TC-BE-TC-05: un JSON roto con DNI y patente va a la DLQ y el log no muestra el cuerpo (DEF-05)")
    void jsonRotoNoFiltraDatosPersonalesAlLog(CapturedOutput output) {
        String dni = "27999111";
        String patente = "AC987ZX";
        String messageId = "msg-" + UUID.randomUUID();
        // Valor sin comillas: el mensaje de Jackson ("Unrecognized token '...'") citaría patente y DNI.
        String jsonRoto = "{\"eventType\":\"streetClosureRequested\",\"data\":{\"offenderNationalId\":\"" + dni
                + "\",\"vehiculo\": " + patente + "_" + dni + "}}";

        publishToTransitoQueue(jsonRoto, messageId);
        publicarCentinelaYEsperar();

        await().atMost(TIMEOUT).pollInterval(Duration.ofMillis(200))
                .until(() -> withChannel(ch -> ch.messageCount(DLQ)) >= 1);
        assertThat(drain(DLQ)).extracting(AbstractBrokerIntegrationTest::body).containsExactly(jsonRoto);

        await().atMost(TIMEOUT).until(() -> output.getAll().contains(messageId));
        assertThat(output.getAll())
                .as("solo metadatos: messageId, cola y tipo de error")
                .contains("[event-rejected]", "messageId=" + messageId, "queue=" + TRANSITO_QUEUE)
                .doesNotContain(dni)
                .doesNotContain(patente);
    }
}
