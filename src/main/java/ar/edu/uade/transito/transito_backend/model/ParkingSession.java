package ar.edu.uade.transito.transito_backend.model;

import java.time.LocalDateTime;
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
@Table(name = "parking_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID sessionId;

    @Column(nullable = false)
    private String licensePlate;

    @Column(nullable = false)
    private UUID zoneId;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    @Column(nullable = false)
    private boolean paid;

    private String paymentReference;

    /**
     * Identificador (subject del JWT) del ciudadano que inició la sesión; null si la creó un inspector
     * o si es anterior a DEF-15. Interno: no se expone en el DTO de respuesta.
     */
    @Column(name = "owner_username")
    private String ownerUsername;
}
