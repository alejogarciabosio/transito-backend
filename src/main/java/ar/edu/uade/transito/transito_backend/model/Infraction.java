package ar.edu.uade.transito.transito_backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.DebtorIdType;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "infractions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Infraction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID infractionId;

    @Column(nullable = false)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InfractionType infractionType;

    @Column(nullable = false)
    private LocalDateTime infractionDateTime;

    @Embedded
    private Location location;

    @ElementCollection
    @CollectionTable(name = "infraction_evidence_urls", joinColumns = @JoinColumn(name = "infraction_id"))
    @Column(name = "evidence_url", nullable = false)
    @Builder.Default
    private List<String> evidenceUrl = new ArrayList<>();

    @Column(nullable = false)
    private String inspectorId;

    private String offenderNationalId;

    private String vehicleOwnerTaxId;

    private String debtorId;

    @Enumerated(EnumType.STRING)
    private DebtorIdType debtorIdType;

    @Column(nullable = false)
    private BigDecimal baseAmount;

    @ElementCollection
    @CollectionTable(name = "infraction_aggravating_factors", joinColumns = @JoinColumn(name = "infraction_id"))
    @Column(name = "aggravating_factor", nullable = false)
    @Builder.Default
    private List<String> aggravatingFactors = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InfractionStatus status;

    private UUID originOperationId;

    private String originTicketId;

    private UUID originParkingSessionId;

    /**
     * Texto del descargo del ciudadano (DEF-38). Nullable y sin default: las filas previas quedan en null.
     * Largo alineado con {@code AppealRequest.reason} (@Size max = 2000). No se publica a Core ni se loguea.
     */
    @Column(name = "appeal_reason", length = 2000)
    private String appealReason;
}
