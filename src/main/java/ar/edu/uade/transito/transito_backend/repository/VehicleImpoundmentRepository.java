package ar.edu.uade.transito.transito_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.uade.transito.transito_backend.model.VehicleImpoundment;
import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentStatus;

public interface VehicleImpoundmentRepository extends JpaRepository<VehicleImpoundment, UUID> {

    List<VehicleImpoundment> findByStatus(ImpoundmentStatus status);

    List<VehicleImpoundment> findByLicensePlate(String licensePlate);

    List<VehicleImpoundment> findByOwnerNationalId(String ownerNationalId);
}
