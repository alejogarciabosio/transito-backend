package ar.edu.uade.transito.transito_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.model.enums.OperationStatus;
import ar.edu.uade.transito.transito_backend.model.enums.OperationType;

public interface TrafficOperationRepository extends JpaRepository<TrafficOperation, UUID> {

    List<TrafficOperation> findByStatus(OperationStatus status);

    List<TrafficOperation> findByZone(String zone);

    List<TrafficOperation> findByType(OperationType type);

    List<TrafficOperation> findByTicketId(String ticketId);
}
