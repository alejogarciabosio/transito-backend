package ar.edu.uade.transito.transito_backend.event.payload;

import java.math.BigDecimal;
import java.util.UUID;

public record InfractionConfirmedPayload(
        UUID infractionId,
        String debtorId,
        String debtorIdType,
        BigDecimal finalAmount) {
}
