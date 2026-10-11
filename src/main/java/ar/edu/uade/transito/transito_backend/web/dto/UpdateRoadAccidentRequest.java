package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.LocalDateTime;
import java.util.List;

import ar.edu.uade.transito.transito_backend.model.AccidentPerson;
import ar.edu.uade.transito.transito_backend.model.AccidentVehicle;
import ar.edu.uade.transito.transito_backend.model.Location;
import ar.edu.uade.transito.transito_backend.model.enums.AccidentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PastOrPresent;

public record UpdateRoadAccidentRequest(
        @PastOrPresent(message = "La fecha del accidente no puede estar en el futuro") LocalDateTime accidentDateTime,
        @Valid Location location,
        Boolean involvesInfrastructureDamage,
        String damageDescription,
        List<@Valid AccidentVehicle> vehicles,
        List<@Valid AccidentPerson> persons,
        AccidentStatus status) {

    @AssertTrue(message = "La descripción del daño es obligatoria cuando existe daño de infraestructura")
    public boolean isDamageDescriptionValid() {
        return !Boolean.TRUE.equals(involvesInfrastructureDamage)
                || damageDescription == null
                || !damageDescription.isBlank();
    }
}
