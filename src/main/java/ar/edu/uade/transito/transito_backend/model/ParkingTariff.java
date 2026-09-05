package ar.edu.uade.transito.transito_backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import lombok.Setter;

@Entity
@Table(name = "parking_tariffs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingTariff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tariffId;

    @Column(nullable = false)
    private UUID zoneId;

    @Column(nullable = false)
    private BigDecimal pricePerHour;

    @Column(nullable = false)
    private LocalDate validFrom;

    private LocalDate validTo;
}
