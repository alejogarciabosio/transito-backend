package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.LocalDateTime;
import java.util.List;

import ar.edu.uade.transito.transito_backend.model.enums.OperationType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTrafficOperationRequest(
        @NotNull(message = "El tipo de operativo es obligatorio") OperationType type,
        @NotBlank(message = "La zona es obligatoria")
        @Size(max = 200, message = "La zona no puede superar 200 caracteres") String zone,
        @NotNull(message = "La fecha de inicio es obligatoria") LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        @NotNull(message = "La lista de inspectores es obligatoria")
        List<@NotBlank(message = "Los identificadores de inspector no pueden estar vacíos") @Size(max = 255, message = "No puede superar 255 caracteres") String> assignedInspectors,
        @NotNull(message = "La lista de móviles es obligatoria")
        List<@NotBlank(message = "Los identificadores de móvil no pueden estar vacíos") @Size(max = 255, message = "No puede superar 255 caracteres") String> assignedUnits,
        @Size(max = 255, message = "No puede superar 255 caracteres") String ticketId) {

    @AssertTrue(message = "La fecha de finalización no puede ser anterior a la fecha de inicio")
    public boolean isDateRangeValid() {
        return startDateTime == null || endDateTime == null || !endDateTime.isBefore(startDateTime);
    }
}
