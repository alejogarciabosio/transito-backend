package ar.edu.uade.transito.transito_backend.listener.handler;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import ar.edu.uade.transito.transito_backend.listener.IncomingEventHandler;
import ar.edu.uade.transito.transito_backend.listener.dto.TicketUpdatedPayload;
import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.model.enums.OperationType;
import ar.edu.uade.transito.transito_backend.service.TrafficOperationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TicketUpdatedHandler implements IncomingEventHandler {

    private static final String TRANSITO_AREA_ID = "M7";
    private static final String ROUTED = "ROUTED";

    private final ObjectMapper objectMapper;
    private final TrafficOperationService trafficOperationService;

    @Override
    public String eventType() {
        return "ticketUpdated";
    }

    @Override
    public void handle(JsonNode data) {
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
                .startDateTime(LocalDateTime.now())
                .ticketId(String.valueOf(payload.ticketId()))
                .build());
    }
}
