package ar.edu.uade.transito.transito_backend.web.mapper;

import ar.edu.uade.transito.transito_backend.model.RoadAccident;
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
                .inspectorId(request.inspectorId())
                .originTicketId(request.originTicketId())
                .build();
    }

    public static RoadAccident toEntity(UpdateRoadAccidentRequest request) {
        return RoadAccident.builder()
                .accidentDateTime(request.accidentDateTime())
                .location(request.location())
                .involvesInfrastructureDamage(request.involvesInfrastructureDamage())
                .damageDescription(request.damageDescription())
                .vehicles(request.vehicles())
                .persons(request.persons())
                .status(request.status())
                .build();
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
