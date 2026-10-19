package ar.edu.uade.transito.transito_backend.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "infraction_payment_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfractionPaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Identificador de origen enviado por Rentas; el contrato lo define como string. */
    @Column(nullable = false)
    private String originId;

    /** Vínculo local opcional cuando originId corresponde a una infracción UUID de M7. */
    private UUID infractionId;

    @Column(nullable = false)
    private String eventType;

    private String debtId;

    private String paymentId;

    private BigDecimal amount;

    private BigDecimal remainingBalance;

    private BigDecimal outstandingBalance;

    private String conceptCode;

    private String citizenId;

    private String reason;

    @Column(nullable = false)
    private OffsetDateTime occurredAt;
}
