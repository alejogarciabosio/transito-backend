package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record CreateParkingSessionRequest(
        @NotBlank(message = "La patente es obligatoria")
        @Size(max = 15, message = "La patente no puede superar 15 caracteres") String licensePlate,
        @NotNull(message = "La zona es obligatoria") UUID zoneId,
        @NotNull(message = "La fecha de inicio es obligatoria")
        @PastOrPresent(message = "La fecha de inicio no puede estar en el futuro") LocalDateTime startDateTime) {
}
