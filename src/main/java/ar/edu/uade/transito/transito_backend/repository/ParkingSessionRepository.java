package ar.edu.uade.transito.transito_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.uade.transito.transito_backend.model.ParkingSession;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, UUID> {

    List<ParkingSession> findByZoneId(UUID zoneId);

    List<ParkingSession> findByLicensePlate(String licensePlate);

    List<ParkingSession> findByPaid(boolean paid);

    boolean existsByLicensePlateIgnoreCaseAndEndDateTimeIsNull(String licensePlate);
}
