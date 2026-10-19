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
        validate(payload);
        List<String> affectedStreets = payload.affectedSections() == null
                ? new ArrayList<>()
                : new ArrayList<>(payload.affectedSections());

        streetClosureService.createFromRequest(StreetClosure.builder()
                .closureRequestId(payload.closureRequestId())
                .sourceRef(payload.sourceRef())
                .reason(payload.reason())
                .closureType(payload.closureType())
                .requestedAt(payload.requestedAt())
                .requestingModule(RequestingModule.fromSourceModule(payload.sourceModule()))
                .startDate(payload.requestedFrom())
                .endDate(payload.requestedTo())
                .affectedStreet(affectedStreets)
                .build());
    }

    /** Campos obligatorios del payload acordado por M3 Obras y M6 Ambiente. */
    private void validate(StreetClosureRequestedPayload payload) {
        requireText(payload.closureRequestId(), "closureRequestId");
        requireText(payload.sourceModule(), "sourceModule");
        RequestingModule.fromSourceModule(payload.sourceModule());
        requireText(payload.sourceRef(), "sourceRef");
        requireText(payload.reason(), "reason");
        if (payload.affectedSections() == null || payload.affectedSections().isEmpty()) {
            throw new IllegalArgumentException("affectedSections es obligatorio");
        }
        if (payload.affectedSections().stream().anyMatch(section -> section == null || section.isBlank())) {
            throw new IllegalArgumentException("affectedSections no puede contener valores vacíos");
        }
        if (payload.requestedFrom() == null || payload.requestedTo() == null || payload.requestedAt() == null) {
            throw new IllegalArgumentException("requestedFrom, requestedTo y requestedAt son obligatorios");
        }
        if (!payload.requestedTo().isAfter(payload.requestedFrom())) {
            throw new IllegalArgumentException("requestedTo debe ser posterior a requestedFrom");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
    }
}
