package ar.edu.uade.transito.transito_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.edu.uade.transito.transito_backend.model.RoadAccident;
import ar.edu.uade.transito.transito_backend.model.enums.AccidentStatus;

public interface RoadAccidentRepository extends JpaRepository<RoadAccident, UUID> {

    List<RoadAccident> findByStatus(AccidentStatus status);

    List<RoadAccident> findByOriginTicketId(String originTicketId);

    /** Accidentes donde la persona figura como involucrada o como titular de alguno de los vehículos. */
    @Query("""
            SELECT DISTINCT a FROM RoadAccident a
            LEFT JOIN a.persons p
            LEFT JOIN a.vehicles v
            WHERE p.nationalId = :nationalId OR v.ownerNationalId = :nationalId
            """)
    List<RoadAccident> findInvolving(@Param("nationalId") String nationalId);
}
