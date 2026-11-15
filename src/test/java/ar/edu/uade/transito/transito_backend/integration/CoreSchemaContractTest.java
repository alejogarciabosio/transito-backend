package ar.edu.uade.transito.transito_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.payload.UpdateTicketStatusPayload;
import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.model.enums.OperationResolution;
import ar.edu.uade.transito.transito_backend.service.TrafficOperationService;
import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;

/**
 * Contrato con los JSON Schema que Core tiene registrados (owner M2, v1.78) para el {@code data}
 * de updateTicketStatus (publicamos) y ticketUpdated (consumimos). Los payloads se serializan con
 * el mismo converter que usa RabbitMqConfiguration en producción.
 */
@DisplayName("Contrato JSON Schema de Core: updateTicketStatus / ticketUpdated (M2 v1.78)")
class CoreSchemaContractTest extends AbstractIntegrationTest {

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
            new com.fasterxml.jackson.databind.ObjectMapper();
    private static final JsonSchema UPDATE_TICKET_STATUS = schema("updateTicketStatus");
    private static final JsonSchema TICKET_UPDATED = schema("ticketUpdated");

    @Autowired
    private TrafficOperationService trafficOperationService;

    private static JsonSchema schema(String name) {
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().formatAssertionsEnabled(true).build();
        try (InputStream in = CoreSchemaContractTest.class
                .getResourceAsStream("/contracts/core/" + name + ".schema.json")) {
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(in, config);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Serializa el sobre como sale a RabbitMQ y devuelve su {@code data}. */
    private static com.fasterxml.jackson.databind.JsonNode wireData(EventEnvelope<?> envelope) throws Exception {
        byte[] body = new JacksonJsonMessageConverter().toMessage(envelope, new MessageProperties()).getBody();
        return JSON.readTree(body).get("data");
    }

    private static Set<ValidationMessage> validate(JsonSchema schema, com.fasterxml.jackson.databind.JsonNode data) {
        return schema.validate(data);
    }

    private String validTicketUpdatedJson(UUID ticketId, String area, String updateType) {
        return """
                {"ticketId":"%s","publicId":"TK-2026-000123","citizenId":"%s","isAnonymous":false,
                 "responsibleAreaId":"%s","updateType":"%s","currentStatus":"ROUTED","currentPriority":"HIGH",
                 "updatedAt":"2026-11-15T10:00:00-03:00","publicMessage":null,"progress":null,"attachments":[],
                 "details":{"routing":{"requestType":"SEMAFORO_ROTO","ticketType":"COMPLAINT",
                   "summary":"Semáforo apagado","description":"El semáforo no funciona desde ayer",
                   "category":"Tránsito","subcategory":"Semáforos","formData":{},"reassignment":false,
                   "resolutionDueAt":"2026-11-20T10:00:00-03:00","escalation":null,
                   "location":{"street":"Av. Corrientes","streetNumber":"1234","addressLine":"Av. Corrientes 1234",
                     "reference":null,"neighborhoodId":null}}}}
                """.formatted(ticketId, UUID.randomUUID(), area, updateType);
    }

    @Test
    @DisplayName("ticketUpdated válido según el esquema → M7 crea operativo con la calle y el STARTED cumple el esquema")
    void routedTicketUpdatedIsConsumedAndStartedMatchesSchema() throws Exception {
        UUID ticketId = UUID.randomUUID();
        String data = validTicketUpdatedJson(ticketId, "M7", "ROUTED");
        assertThat(validate(TICKET_UPDATED, JSON.readTree(data))).isEmpty();

        dispatchIncoming("ticketUpdated", data);

        var ops = trafficOperationRepository.findAll();
        assertThat(ops).hasSize(1);
        assertThat(ops.get(0).getZone()).isEqualTo("Av. Corrientes");
        EventEnvelope<?> started = events.ofType("updateTicketStatus").get(0);
        var wire = wireData(started);
        assertThat(wire.get("updateType").asText()).isEqualTo("STARTED");
        assertThat(validate(UPDATE_TICKET_STATUS, wire)).isEmpty();
    }

    @Test
    @DisplayName("ticketUpdated de otra área o de otro updateType (válidos) → se ignoran")
    void otherAreasAndUpdateTypesAreIgnored() throws Exception {
        String otherArea = validTicketUpdatedJson(UUID.randomUUID(), "M3", "ROUTED");
        assertThat(validate(TICKET_UPDATED, JSON.readTree(otherArea))).isEmpty();
        dispatchIncoming("ticketUpdated", otherArea);
        dispatchIncoming("ticketUpdated", """
                {"ticketId":"%s","publicId":"TK-2026-000124","citizenId":null,"isAnonymous":true,
                 "responsibleAreaId":"M7","updateType":"PRIORITY_CHANGED","currentStatus":"ROUTED",
                 "currentPriority":"CRITICAL","updatedAt":"2026-11-15T10:00:00Z","details":null}
                """.formatted(UUID.randomUUID()));
        assertThat(trafficOperationRepository.count()).isZero();
        assertThat(events.all()).isEmpty();
    }

    @Test
    @DisplayName("ROUTED sin calle (location null) → zona 'Sin definir'")
    void routedWithoutStreetUsesDefaultZone() throws Exception {
        String data = validTicketUpdatedJson(UUID.randomUUID(), "M7", "ROUTED")
                .replace("\"street\":\"Av. Corrientes\"", "\"street\":null");
        assertThat(validate(TICKET_UPDATED, JSON.readTree(data))).isEmpty();
        dispatchIncoming("ticketUpdated", data);
        assertThat(trafficOperationRepository.findAll().get(0).getZone()).isEqualTo("Sin definir");
    }

    @Test
    @DisplayName("Cierre del operativo → RESOLVED (ACTION_COMPLETED y NO_FURTHER_ACTION_REQUIRED) cumple el esquema")
    void resolvedPayloadsMatchSchema() throws Exception {
        Map<OperationResolution, String> expected = Map.of(
                OperationResolution.INFRACTION_CREATED, "ACTION_COMPLETED",
                OperationResolution.ACCIDENT_REGISTERED, "ACTION_COMPLETED",
                OperationResolution.NO_ACTION, "NO_FURTHER_ACTION_REQUIRED");
        for (var entry : expected.entrySet()) {
            events.clear();
            trafficOperationRepository.deleteAll();
            dispatchIncoming("ticketUpdated", validTicketUpdatedJson(UUID.randomUUID(), "M7", "ROUTED"));
            TrafficOperation op = trafficOperationRepository.findAll().get(0);
            events.clear();

            trafficOperationService.close(op.getOperationId(), entry.getKey());

            List<EventEnvelope<?>> published = events.ofType("updateTicketStatus");
            assertThat(published).hasSize(1);
            var wire = wireData(published.get(0));
            assertThat(validate(UPDATE_TICKET_STATUS, wire)).as(entry.getKey().name()).isEmpty();
            assertThat(wire.get("updateType").asText()).isEqualTo("RESOLVED");
            assertThat(wire.at("/details/resolution/type").asText()).isEqualTo(entry.getValue());
            assertThat(wire.get("internalMessage").asText())
                    .isEqualTo("Operativo " + op.getOperationId() + "; resolución " + entry.getKey().name());
            assertThat(wire.get("progress").asInt()).isEqualTo(100);
            assertThat(wire.get("updateOccurredAt").asText()).matches(".*(Z|[+-]\\d{2}:\\d{2})$");
        }
    }

    @Test
    @DisplayName("El esquema rechaza el formato viejo (operationId/resolution en details)")
    void legacyDetailsAreRejectedBySchema() throws Exception {
        var legacy = new UpdateTicketStatusPayload(UUID.randomUUID(), "RESOLVED", "Listo", null, 100,
                Map.of("operationId", UUID.randomUUID().toString(), "resolution", "INFRACTION_CREATED"),
                List.of(), new UpdateTicketStatusPayload.UpdatedBy("SYSTEM", null),
                java.time.OffsetDateTime.now());
        assertThat(validate(UPDATE_TICKET_STATUS, wireData(EventEnvelope.of("updateTicketStatus", legacy))))
                .isNotEmpty();
    }
}
