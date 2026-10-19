package ar.edu.uade.transito.transito_backend.web.dto;

import ar.edu.uade.transito.transito_backend.model.enums.ParkingZoneStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateParkingZoneRequest(
        @NotBlank(message = "El nombre de la zona es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres") String name,
        @NotBlank(message = "Los límites de la zona son obligatorios") String boundaries,
        @NotBlank(message = "El horario permitido es obligatorio") String allowedHours,
        @NotNull(message = "El estado de la zona es obligatorio") ParkingZoneStatus status) {
}
