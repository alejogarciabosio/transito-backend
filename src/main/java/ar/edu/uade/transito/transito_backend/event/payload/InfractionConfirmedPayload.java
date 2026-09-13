package ar.edu.uade.transito.transito_backend.event.payload;

import java.math.BigDecimal;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.DebtorIdType;

public record InfractionConfirmedPayload(
        UUID infractionId,
        String debtorId,
        DebtorIdType debtorIdType,
        BigDecimal finalAmount) {
}
