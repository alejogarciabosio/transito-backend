package ar.edu.uade.transito.transito_backend.repository;

import java.util.Collection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;

public interface StreetClosureRepository extends JpaRepository<StreetClosure, UUID> {

    List<StreetClosure> findByStatus(StreetClosureStatus status);

    List<StreetClosure> findByStatusInOrderByStartDateAsc(Collection<StreetClosureStatus> statuses);

    List<StreetClosure> findByStatusAndEndDateLessThanEqual(StreetClosureStatus status, OffsetDateTime endDate);

    List<StreetClosure> findByRequestingModule(RequestingModule requestingModule);

    List<StreetClosure> findByClosureRequestId(String closureRequestId);
}
