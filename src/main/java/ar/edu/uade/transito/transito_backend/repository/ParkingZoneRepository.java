package ar.edu.uade.transito.transito_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.uade.transito.transito_backend.model.ParkingZone;
import ar.edu.uade.transito.transito_backend.model.enums.ParkingZoneStatus;

public interface ParkingZoneRepository extends JpaRepository<ParkingZone, UUID> {

    List<ParkingZone> findByStatus(ParkingZoneStatus status);
}
