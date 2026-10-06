package ar.edu.uade.transito.transito_backend.listener.handler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import ar.edu.uade.transito.transito_backend.listener.IncomingEventHandler;
import ar.edu.uade.transito.transito_backend.listener.dto.StreetClosureRequestedPayload;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.service.StreetClosureService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StreetClosureRequestedHandler implements IncomingEventHandler {

    private final ObjectMapper objectMapper;
    private final StreetClosureService streetClosureService;

    @Override
    public String eventType() {
        return "streetClosureRequested";
    }

    @Override
    public void handle(JsonNode data) {
        StreetClosureRequestedPayload payload = objectMapper.convertValue(data, StreetClosureRequestedPayload.class);
        List<String> affectedStreets = payload.affectedSections() == null
                ? new ArrayList<>()
                : new ArrayList<>(payload.affectedSections());

        streetClosureService.createFromRequest(StreetClosure.builder()
                .closureRequestId(payload.closureRequestId())
                .requestingModule(RequestingModule.fromCode(payload.sourceModule()))
                .startDate(payload.requestedFrom())
                .endDate(payload.requestedTo())
                .affectedStreet(affectedStreets)
                .build());
    }
}
