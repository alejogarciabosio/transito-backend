package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentOriginType;

public record CreateVehicleImpoundmentRequest(
        String licensePlate,
        String ownerNationalId,
        String reason,
        String depotLocation,
        LocalDateTime entryDateTime,
        ImpoundmentOriginType originType,
        UUID originId) {
}
