package ar.edu.uade.transito.transito_backend.listener.handler;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import ar.edu.uade.transito.transito_backend.config.MunicipalTime;

import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.payload.UpdateTicketStatusPayload;
import ar.edu.uade.transito.transito_backend.listener.ContextualIncomingEventHandler;
import ar.edu.uade.transito.transito_backend.listener.dto.TicketUpdatedPayload;
import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.model.enums.OperationType;
import ar.edu.uade.transito.transito_backend.service.TrafficOperationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TicketUpdatedHandler implements ContextualIncomingEventHandler {

    private static final String TRANSITO_AREA_ID = "M7";
    private static final String ROUTED = "ROUTED";

    private final ObjectMapper objectMapper;
    private final TrafficOperationService trafficOperationService;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public String eventType() {
        return "ticketUpdated";
    }

    @Override
    public void handle(JsonNode data) {
        handle(data, UUID.randomUUID(), null);
    }

    @Override
    public void handle(JsonNode data, UUID eventId, UUID correlationId) {
        TicketUpdatedPayload payload = objectMapper.convertValue(data, TicketUpdatedPayload.class);
        if (!TRANSITO_AREA_ID.equals(payload.responsibleAreaId()) || !ROUTED.equals(payload.updateType())) {
            return;
        }

        String zone = Optional.ofNullable(payload.details())
                .map(details -> details.path("routing").path("location").path("street").asText(null))
                .orElse("Sin definir");

        trafficOperationService.create(TrafficOperation.builder()
                .type(OperationType.COMPLAINT_VERIFICATION)
                .zone(zone)
                .startDateTime(MunicipalTime.now(clock))
                .ticketId(payload.ticketId().toString())
                .sourceEventId(eventId)
                .sourceCorrelationId(correlationId)
                .build());

        eventPublisher.publish(EventEnvelope.of("updateTicketStatus",
                UpdateTicketStatusPayload.started(payload.ticketId()), correlationId, eventId));
    }
}
