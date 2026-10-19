package ar.edu.uade.transito.transito_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.uade.transito.transito_backend.model.InfractionPaymentEvent;

public interface InfractionPaymentEventRepository extends JpaRepository<InfractionPaymentEvent, UUID> {
    List<InfractionPaymentEvent> findByOriginIdOrderByOccurredAtAsc(String originId);
}
