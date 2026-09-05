package ar.edu.uade.transito.transito_backend.model;

import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.ParkingZoneStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parking_zones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingZone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID zoneId;

    @Column(nullable = false)
    private String name;

    private String boundaries;

    @Column(nullable = false)
    private String allowedHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParkingZoneStatus status;
}
