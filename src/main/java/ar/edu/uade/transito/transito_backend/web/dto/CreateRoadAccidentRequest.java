package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.LocalDateTime;
import java.util.List;

import ar.edu.uade.transito.transito_backend.model.AccidentPerson;
import ar.edu.uade.transito.transito_backend.model.AccidentVehicle;
import ar.edu.uade.transito.transito_backend.model.Location;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record CreateRoadAccidentRequest(
        @NotNull(message = "La fecha del accidente es obligatoria")
        @PastOrPresent(message = "La fecha del accidente no puede estar en el futuro") LocalDateTime accidentDateTime,
        @NotNull(message = "La ubicación es obligatoria") @Valid Location location,
        boolean involvesInfrastructureDamage,
        @Size(max = 255, message = "No puede superar 255 caracteres") String damageDescription,
        @NotNull(message = "La lista de vehículos es obligatoria") List<@Valid AccidentVehicle> vehicles,
        @NotNull(message = "La lista de personas es obligatoria") List<@Valid AccidentPerson> persons,
        @Size(max = 255, message = "No puede superar 255 caracteres") String originTicketId) {

    @AssertTrue(message = "La descripción del daño es obligatoria cuando existe daño de infraestructura")
    public boolean isDamageDescriptionValid() {
        return !involvesInfrastructureDamage || (damageDescription != null && !damageDescription.isBlank());
    }
}
