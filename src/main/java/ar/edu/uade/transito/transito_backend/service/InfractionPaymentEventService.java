package ar.edu.uade.transito.transito_backend.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.listener.dto.RentasPaymentPayload;
import ar.edu.uade.transito.transito_backend.model.InfractionPaymentEvent;
import ar.edu.uade.transito.transito_backend.repository.InfractionPaymentEventRepository;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class InfractionPaymentEventService {

    private static final String TRAFFIC_FINE = "TRAFFIC_FINE";

    private final InfractionRepository infractionRepository;
    private final InfractionPaymentEventRepository paymentEventRepository;

    public void record(String eventType, RentasPaymentPayload payload) {
        if (!TRAFFIC_FINE.equals(payload.originType())) {
            return;
        }

        validateRequiredFields(eventType, payload);
        if (payload.originId() == null || payload.originId().isBlank()) {
            throw new IllegalArgumentException("originId es obligatorio para eventos TRAFFIC_FINE");
        }
        UUID infractionId = requiredInfractionId(payload);
        if (!infractionRepository.existsById(infractionId)) {
            throw new ResourceNotFoundException("Infracción no encontrada para evento de Rentas: " + infractionId);
        }

        paymentEventRepository.save(InfractionPaymentEvent.builder()
                .originId(payload.originId())
                .infractionId(infractionId)
                .eventType(eventType)
                .debtId(payload.debtId())
                .paymentId(payload.paymentId())
                .amount("paymentReversed".equals(eventType) ? payload.reversedAmount() : payload.amountApplied())
                .remainingBalance(payload.remainingBalance())
                .outstandingBalance(payload.outstandingBalance())
                .conceptCode(payload.conceptCode())
                .citizenId(payload.citizenId())
                .reason(payload.reason())
                .occurredAt(occurredAt(eventType, payload))
                .build());
    }

    private void validateRequiredFields(String eventType, RentasPaymentPayload payload) {
        requireText(payload.debtId(), "debtId");
        switch (eventType) {
            case "paymentRegistered" -> {
                requireText(payload.paymentId(), "paymentId");
                requireAmount(payload.amountApplied(), "amountApplied");
                requireDate(payload.paidAt(), "paidAt");
                requireAmount(payload.remainingBalance(), "remainingBalance");
            }
            case "debtSettled" -> {
                requireText(payload.conceptCode(), "conceptCode");
                requireDate(payload.settledAt(), "settledAt");
                requireAmount(payload.remainingBalance(), "remainingBalance");
                requireAmount(payload.outstandingBalance(), "outstandingBalance");
            }
            case "paymentReversed" -> {
                requireText(payload.paymentId(), "paymentId");
                requireAmount(payload.reversedAmount(), "reversedAmount");
                requireDate(payload.reversedAt(), "reversedAt");
                requireAmount(payload.remainingBalance(), "remainingBalance");
                requireText(payload.reason(), "reason");
            }
            default -> throw new IllegalArgumentException("Evento de Rentas no soportado: " + eventType);
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
    }

    private void requireAmount(java.math.BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(field + " debe ser un importe no negativo");
        }
    }

    private void requireDate(OffsetDateTime value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
    }

    private UUID requiredInfractionId(RentasPaymentPayload payload) {
        if (payload.infractionId() == null || payload.infractionId().isBlank()) {
            throw new IllegalArgumentException("infractionId es obligatorio para TRAFFIC_FINE");
        }
        try {
            UUID originId = UUID.fromString(payload.originId());
            UUID infractionId = UUID.fromString(payload.infractionId());
            if (!originId.equals(infractionId)) {
                throw new IllegalArgumentException("originId e infractionId deben coincidir para TRAFFIC_FINE");
            }
            return infractionId;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("originId e infractionId deben ser UUID válidos de la misma infracción", ex);
        }
    }

    private OffsetDateTime occurredAt(String eventType, RentasPaymentPayload payload) {
        return switch (eventType) {
            case "paymentRegistered" -> payload.paidAt();
            case "debtSettled" -> payload.settledAt();
            case "paymentReversed" -> payload.reversedAt();
            default -> throw new IllegalArgumentException("Evento de Rentas no soportado: " + eventType);
        };
    }
}
