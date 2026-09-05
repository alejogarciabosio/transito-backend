package ar.edu.uade.transito.transito_backend.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;
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
@Table(name = "street_closures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreetClosure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID streetClosureId;

    @Column(nullable = false)
    private String closureRequestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestingModule requestingModule;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String affectedStreet;

    @ElementCollection
    @CollectionTable(name = "street_closure_detours", joinColumns = @JoinColumn(name = "street_closure_id"))
    @Builder.Default
    private List<Detour> detours = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "street_closure_conditions", joinColumns = @JoinColumn(name = "street_closure_id"))
    @Column(name = "condition", nullable = false)
    @Builder.Default
    private List<String> conditions = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StreetClosureStatus status;
}
