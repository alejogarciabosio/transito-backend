package ar.edu.uade.transito.transito_backend.web.dto;

public record UpdateVehicleImpoundmentRequest(
        String reason,
        String depotLocation,
        String ownerNationalId) {
}
