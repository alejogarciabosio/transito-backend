package ar.edu.uade.transito.transito_backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentOriginType;
import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
@Table(name = "vehicle_impoundments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleImpoundment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID impoundmentId;

    @Column(nullable = false)
    private String licensePlate;

    private String ownerNationalId;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String depotLocation;

    @Column(nullable = false)
    private LocalDateTime entryDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImpoundmentOriginType originType;

    @Column(nullable = false)
    private UUID originId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImpoundmentStatus status;

    private String paymentReference;

    @ElementCollection
    @CollectionTable(name = "vehicle_impoundment_verified_requirements", joinColumns = @JoinColumn(name = "impoundment_id"))
    @Column(name = "requirement", nullable = false)
    @Builder.Default
    private List<String> verifiedRequirements = new ArrayList<>();

    private BigDecimal towingAndStorageFee;

    private String releasedBy;
}
