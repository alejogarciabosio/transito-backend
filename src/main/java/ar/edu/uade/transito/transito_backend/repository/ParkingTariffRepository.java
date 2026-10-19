package ar.edu.uade.transito.transito_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.uade.transito.transito_backend.model.ParkingTariff;
import ar.edu.uade.transito.transito_backend.model.enums.ParkingTariffStatus;

public interface ParkingTariffRepository extends JpaRepository<ParkingTariff, UUID> {

    List<ParkingTariff> findByZoneIdAndStatusOrderByValidFromDesc(UUID zoneId, ParkingTariffStatus status);
}
