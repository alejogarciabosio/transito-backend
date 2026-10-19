package ar.edu.uade.transito.transito_backend.listener.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RentasPaymentPayload(
        String paymentId,
        String debtId,
        String originType,
        String originId,
        String infractionId,
        BigDecimal amountApplied,
        BigDecimal reversedAmount,
        BigDecimal remainingBalance,
        BigDecimal outstandingBalance,
        String conceptCode,
        String citizenId,
        String reason,
        OffsetDateTime paidAt,
        OffsetDateTime settledAt,
        OffsetDateTime reversedAt) {
}
