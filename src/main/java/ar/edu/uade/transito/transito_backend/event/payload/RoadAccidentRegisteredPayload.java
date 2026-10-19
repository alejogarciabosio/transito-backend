package ar.edu.uade.transito.transito_backend.event.payload;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.AccidentPerson;
import ar.edu.uade.transito.transito_backend.model.AccidentVehicle;
import ar.edu.uade.transito.transito_backend.model.Location;

public record RoadAccidentRegisteredPayload(
        UUID accidentId,
        OffsetDateTime accidentDateTime,
        Location location,
        boolean involvesInfrastructureDamage,
        String damageDescription,
        List<AccidentVehicle> vehicles,
        List<AccidentPerson> persons,
        String inspectorId) {
}
