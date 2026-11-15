package ar.edu.uade.transito.transito_backend.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateVehicleImpoundmentRequest(
        @Size(min = 1, max = 255, message = "El motivo debe tener entre 1 y 255 caracteres") String reason,
        @Size(min = 1, max = 200, message = "El depósito debe tener entre 1 y 200 caracteres") String depotLocation,
        @Size(min = 1, max = 30, message = "El documento del titular debe tener entre 1 y 30 caracteres") String ownerNationalId) {
}
