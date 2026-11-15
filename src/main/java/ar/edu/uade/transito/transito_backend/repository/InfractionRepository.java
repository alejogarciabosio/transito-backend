package ar.edu.uade.transito.transito_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;

public interface InfractionRepository extends JpaRepository<Infraction, UUID> {

    List<Infraction> findByStatus(InfractionStatus status);

    List<Infraction> findByInfractionType(InfractionType infractionType);

    List<Infraction> findByLicensePlate(String licensePlate);

    List<Infraction> findByOffenderNationalId(String offenderNationalId);

    List<Infraction> findByOriginOperationId(UUID originOperationId);

    boolean existsByOriginParkingSessionId(UUID originParkingSessionId);

    /**
     * Listado completo con las evidencias en un solo JOIN (Hibernate 6 descarta las filas duplicadas).
     * Junto con {@link #fetchAggravatingFactorsOfAll()} carga el listado en 2 consultas en vez de 1 + 2·N:
     * no se pueden traer las dos colecciones (List) en el mismo JOIN (MultipleBagFetchException).
     */
    @Query("select i from Infraction i left join fetch i.evidenceUrl")
    List<Infraction> findAllFetchingEvidenceUrl();

    /** Inicializa los agravantes de todas las infracciones ya cargadas en la misma sesión. */
    @Query("select i from Infraction i left join fetch i.aggravatingFactors")
    List<Infraction> fetchAggravatingFactorsOfAll();
}
