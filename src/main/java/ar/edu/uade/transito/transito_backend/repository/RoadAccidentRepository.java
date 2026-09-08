package ar.edu.uade.transito.transito_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.uade.transito.transito_backend.model.RoadAccident;
import ar.edu.uade.transito.transito_backend.model.enums.AccidentStatus;

public interface RoadAccidentRepository extends JpaRepository<RoadAccident, UUID> {

    List<RoadAccident> findByStatus(AccidentStatus status);

    List<RoadAccident> findByOriginTicketId(String originTicketId);
}
