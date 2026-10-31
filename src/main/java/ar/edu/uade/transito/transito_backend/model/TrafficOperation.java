package ar.edu.uade.transito.transito_backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.OperationResolution;
import ar.edu.uade.transito.transito_backend.model.enums.OperationStatus;
import ar.edu.uade.transito.transito_backend.model.enums.OperationType;
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
@Table(name = "traffic_operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID operationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationType type;

    @Column(nullable = false)
    private String zone;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    @ElementCollection
    @CollectionTable(name = "traffic_operation_inspectors", joinColumns = @JoinColumn(name = "operation_id"))
    @Column(name = "inspector_id", nullable = false)
    @Builder.Default
    private List<String> assignedInspectors = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "traffic_operation_units", joinColumns = @JoinColumn(name = "operation_id"))
    @Column(name = "unit_id", nullable = false)
    @Builder.Default
    private List<String> assignedUnits = new ArrayList<>();

    private String ticketId;

    private UUID sourceEventId;

    private UUID sourceCorrelationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationStatus status;

    @Enumerated(EnumType.STRING)
    private OperationResolution resolution;
}
