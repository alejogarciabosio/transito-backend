package ar.edu.uade.transito.transito_backend.repository;

import ar.edu.uade.transito.transito_backend.model.OutboxEvent;
import ar.edu.uade.transito.transito_backend.model.enums.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /** Candidatos a publicar, en orden de creación (los que esperan backoff o están tomados quedan afuera). */
    @Query("select e.eventId from OutboxEvent e where e.status = :status and e.nextAttemptAt <= :now "
            + "order by e.createdAt asc, e.eventId asc")
    List<UUID> findDueIds(@Param("status") OutboxStatus status, @Param("now") Instant now, Pageable page);

    /**
     * Toma un evento con un update condicional (atómico en cualquier base): solo uno de varios relays
     * concurrentes obtiene 1; los demás obtienen 0 y lo saltean. Así un evento no se publica dos veces.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update OutboxEvent e set e.nextAttemptAt = :leaseUntil "
            + "where e.eventId = :id and e.status = :status and e.nextAttemptAt <= :now")
    int claim(@Param("id") UUID id, @Param("status") OutboxStatus status, @Param("now") Instant now,
              @Param("leaseUntil") Instant leaseUntil);

    List<OutboxEvent> findByStatusOrderByCreatedAtDesc(OutboxStatus status, Pageable page);

    List<OutboxEvent> findAllByOrderByCreatedAtDesc(Pageable page);

    long countByStatus(OutboxStatus status);
}
