package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentOriginType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record CreateVehicleImpoundmentRequest(
        @NotBlank(message = "La patente es obligatoria")
        @Size(max = 15, message = "La patente no puede superar 15 caracteres") String licensePlate,
        @NotBlank(message = "El documento del titular es obligatorio") String ownerNationalId,
        @NotBlank(message = "El motivo de retención es obligatorio") String reason,
        @NotBlank(message = "El depósito es obligatorio") String depotLocation,
        @NotNull(message = "La fecha de ingreso es obligatoria")
        @PastOrPresent(message = "La fecha de ingreso no puede estar en el futuro") LocalDateTime entryDateTime,
        @NotNull(message = "El tipo de origen es obligatorio") ImpoundmentOriginType originType,
        @NotNull(message = "El identificador de origen es obligatorio") UUID originId) {
}
