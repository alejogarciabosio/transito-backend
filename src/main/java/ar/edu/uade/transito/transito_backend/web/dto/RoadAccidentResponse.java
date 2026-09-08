package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.AccidentPerson;
import ar.edu.uade.transito.transito_backend.model.AccidentVehicle;
import ar.edu.uade.transito.transito_backend.model.Location;
import ar.edu.uade.transito.transito_backend.model.enums.AccidentStatus;

public record RoadAccidentResponse(
        UUID accidentId,
        LocalDateTime accidentDateTime,
        Location location,
        boolean involvesInfrastructureDamage,
        String damageDescription,
        List<AccidentVehicle> vehicles,
        List<AccidentPerson> persons,
        String inspectorId,
        AccidentStatus status,
        String originTicketId) {
}
