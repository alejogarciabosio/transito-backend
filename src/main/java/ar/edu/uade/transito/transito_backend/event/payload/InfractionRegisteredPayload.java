package ar.edu.uade.transito.transito_backend.event.payload;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;

public record InfractionRegisteredPayload(
        UUID infractionId,
        String licensePlate,
        InfractionType infractionType,
        LocalDateTime infractionDateTime,
        BigDecimal baseAmount,
        List<String> aggravatingFactors,
        BigDecimal finalAmount,
        UUID originOperationId,
        String originTicketId,
        UUID originParkingSessionId) {
}
