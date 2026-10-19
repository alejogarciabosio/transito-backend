package ar.edu.uade.transito.transito_backend.web.mapper;

import ar.edu.uade.transito.transito_backend.model.RoadAccident;
import ar.edu.uade.transito.transito_backend.service.command.UpdateRoadAccidentCommand;
import ar.edu.uade.transito.transito_backend.web.dto.CreateRoadAccidentRequest;
import ar.edu.uade.transito.transito_backend.web.dto.RoadAccidentResponse;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateRoadAccidentRequest;

public final class RoadAccidentMapper {

    private RoadAccidentMapper() {
    }

    public static RoadAccident toEntity(CreateRoadAccidentRequest request) {
        return RoadAccident.builder()
                .accidentDateTime(request.accidentDateTime())
                .location(request.location())
                .involvesInfrastructureDamage(request.involvesInfrastructureDamage())
                .damageDescription(request.damageDescription())
                .vehicles(request.vehicles())
                .persons(request.persons())
                .originTicketId(request.originTicketId())
                .build();
    }

    public static UpdateRoadAccidentCommand toCommand(UpdateRoadAccidentRequest request) {
        return new UpdateRoadAccidentCommand(
                request.accidentDateTime(),
                request.location(),
                request.involvesInfrastructureDamage(),
                request.damageDescription(),
                request.vehicles(),
                request.persons(),
                request.status());
    }

    public static RoadAccidentResponse toResponse(RoadAccident accident) {
        return new RoadAccidentResponse(
                accident.getAccidentId(),
                accident.getAccidentDateTime(),
                accident.getLocation(),
                accident.isInvolvesInfrastructureDamage(),
                accident.getDamageDescription(),
                accident.getVehicles(),
                accident.getPersons(),
                accident.getInspectorId(),
                accident.getStatus(),
                accident.getOriginTicketId());
    }
}
