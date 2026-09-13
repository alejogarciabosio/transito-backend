package ar.edu.uade.transito.transito_backend.event.payload;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VehicleReleasedPayload(
        UUID impoundmentId,
        LocalDateTime releaseDateTime,
        List<String> verifiedRequirements,
        BigDecimal towingAndStorageFee,
        String paymentReference,
        String releasedBy) {
}
