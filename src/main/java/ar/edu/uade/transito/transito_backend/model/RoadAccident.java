package ar.edu.uade.transito.transito_backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.AccidentStatus;
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
@Table(name = "road_accidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadAccident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID accidentId;

    @Column(nullable = false)
    private LocalDateTime accidentDateTime;

    @Embedded
    private Location location;

    @Column(nullable = false)
    private boolean involvesInfrastructureDamage;

    private String damageDescription;

    @ElementCollection
    @CollectionTable(name = "road_accident_vehicles", joinColumns = @JoinColumn(name = "accident_id"))
    @Builder.Default
    private List<AccidentVehicle> vehicles = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "road_accident_persons", joinColumns = @JoinColumn(name = "accident_id"))
    @Builder.Default
    private List<AccidentPerson> persons = new ArrayList<>();

    @Column(nullable = false)
    private String inspectorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccidentStatus status;

    private String originTicketId;
}
