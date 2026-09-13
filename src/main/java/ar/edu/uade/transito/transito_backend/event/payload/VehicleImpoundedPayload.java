package ar.edu.uade.transito.transito_backend.event.payload;

import java.time.LocalDateTime;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentOriginType;

public record VehicleImpoundedPayload(
        UUID impoundmentId,
        String licensePlate,
        String ownerNationalId,
        String reason,
        String depotLocation,
        LocalDateTime entryDateTime,
        ImpoundmentOriginType originType,
        UUID originId) {
}
