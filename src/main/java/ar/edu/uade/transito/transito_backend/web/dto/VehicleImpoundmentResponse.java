package ar.edu.uade.transito.transito_backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentOriginType;
import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentStatus;

public record VehicleImpoundmentResponse(
        UUID impoundmentId,
        String licensePlate,
        String ownerNationalId,
        String reason,
        String depotLocation,
        LocalDateTime entryDateTime,
        ImpoundmentOriginType originType,
        UUID originId,
        ImpoundmentStatus status,
        String paymentReference,
        List<String> verifiedRequirements,
        BigDecimal towingAndStorageFee,
        String releasedBy) {
}
