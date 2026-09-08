package ar.edu.uade.transito.transito_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.uade.transito.transito_backend.model.ParkingTariff;

public interface ParkingTariffRepository extends JpaRepository<ParkingTariff, UUID> {

    List<ParkingTariff> findByZoneId(UUID zoneId);
}
