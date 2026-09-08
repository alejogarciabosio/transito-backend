package ar.edu.uade.transito.transito_backend.web.mapper;

import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.web.dto.CreateTrafficOperationRequest;
import ar.edu.uade.transito.transito_backend.web.dto.TrafficOperationResponse;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateTrafficOperationRequest;

public final class TrafficOperationMapper {

    private TrafficOperationMapper() {
    }

    public static TrafficOperation toEntity(CreateTrafficOperationRequest request) {
        return TrafficOperation.builder()
                .type(request.type())
                .zone(request.zone())
                .startDateTime(request.startDateTime())
                .endDateTime(request.endDateTime())
                .assignedInspectors(request.assignedInspectors())
                .assignedUnits(request.assignedUnits())
                .ticketId(request.ticketId())
                .build();
    }

    public static TrafficOperation toEntity(UpdateTrafficOperationRequest request) {
        return TrafficOperation.builder()
                .zone(request.zone())
                .endDateTime(request.endDateTime())
                .assignedInspectors(request.assignedInspectors())
                .assignedUnits(request.assignedUnits())
                .build();
    }

    public static TrafficOperationResponse toResponse(TrafficOperation operation) {
        return new TrafficOperationResponse(
                operation.getOperationId(),
                operation.getType(),
                operation.getZone(),
                operation.getStartDateTime(),
                operation.getEndDateTime(),
                operation.getAssignedInspectors(),
                operation.getAssignedUnits(),
                operation.getTicketId(),
                operation.getStatus(),
                operation.getResolution());
    }
}
