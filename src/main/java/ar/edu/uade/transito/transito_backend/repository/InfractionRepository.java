package ar.edu.uade.transito.transito_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;

public interface InfractionRepository extends JpaRepository<Infraction, UUID> {

    List<Infraction> findByStatus(InfractionStatus status);

    List<Infraction> findByInfractionType(InfractionType infractionType);

    List<Infraction> findByLicensePlate(String licensePlate);

    List<Infraction> findByOffenderNationalId(String offenderNationalId);

    List<Infraction> findByOriginOperationId(UUID originOperationId);
}
