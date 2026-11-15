package ar.edu.uade.transito.transito_backend.model;

import ar.edu.uade.transito.transito_backend.model.enums.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional Outbox (DEF-14) y registro de eventos publicados (DEF-04).
 * <p>
 * Cada evento hacia Core se guarda acá en la MISMA transacción que el cambio de negocio; un relay
 * programado lo publica después. {@code payload} es el sobre Core completo, serializado exactamente
 * como se enviaba antes al broker, así el contrato no cambia. Tabla nueva (solo aditiva).
 */
@Entity
@Table(name = "outbox_events", indexes = @Index(name = "idx_outbox_status_next", columnList = "status, nextAttemptAt"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    /** Es el mismo eventId del sobre: también sirve de clave de idempotencia para los consumidores. */
    @Id
    private UUID eventId;

    @Column(nullable = false, length = 100)
    private String eventType;

    private UUID correlationId;

    private UUID causationId;

    /** Sobre completo en JSON (mismo contenido que recibe Core). Puede tener datos personales: nunca se loguea ni se expone. */
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    /** Último error de publicación: solo tipo de excepción y mensaje técnico recortado, sin payload. */
    @Column(length = 500)
    private String lastError;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    /** Cuándo puede volver a intentarse (backoff) o hasta cuándo lo tiene tomado un relay (lease). */
    @Column(nullable = false)
    private Instant nextAttemptAt;
}
