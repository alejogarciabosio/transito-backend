package ar.edu.uade.transito.transito_backend.listener.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import ar.edu.uade.transito.transito_backend.listener.IncomingEventHandler;
import ar.edu.uade.transito.transito_backend.listener.dto.StreetClosureRequestedPayload;
import ar.edu.uade.transito.transito_backend.model.AffectedSection;
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
        List<AffectedSection> affectedSections = payload.affectedSections() == null
                ? new ArrayList<>()
                : payload.affectedSections().stream()
                        .map(section -> AffectedSection.builder()
                                .street(section.street())
                                .from(section.from())
                                .to(section.to())
                                .direction(section.direction())
                                .build())
                        .collect(Collectors.toCollection(ArrayList::new));
        List<String> affectedStreets = affectedSections.stream()
                .map(AffectedSection::getStreet)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        streetClosureService.createFromRequest(StreetClosure.builder()
                .closureRequestId(payload.closureRequestId())
                .previousClosureRequestId(payload.previousClosureRequestId())
                .sourceRef(payload.sourceRef())
                .reason(payload.reason())
                .closureType(payload.closureType())
                .requestedAt(payload.requestedAt())
                .requestingModule(RequestingModule.fromCode(payload.sourceModule()))
                .startDate(payload.requestedFrom())
                .endDate(payload.requestedTo())
                .affectedStreet(affectedStreets)
                .affectedSections(affectedSections)
                .build());
    }
}
