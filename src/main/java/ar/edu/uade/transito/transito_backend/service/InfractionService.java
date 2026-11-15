package ar.edu.uade.transito.transito_backend.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.payload.InfractionAnulledPayload;
import ar.edu.uade.transito.transito_backend.event.payload.InfractionAppealedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.InfractionConfirmedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.InfractionRegisteredPayload;
import ar.edu.uade.transito.transito_backend.exception.InvalidRequestException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;
import ar.edu.uade.transito.transito_backend.model.state.InfractionStates;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class InfractionService {

    private static final ZoneId MUNICIPAL_TIME_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final InfractionRepository infractionRepository;
    private final EventPublisher eventPublisher;

    public Infraction register(Infraction infraction) {
        InfractionValidationRule.verify(infraction);
        infraction.setStatus(InfractionStatus.PENDING_APPEAL);
        Infraction saved = infractionRepository.save(infraction);

        eventPublisher.publish(EventEnvelope.of("infractionRegistered", new InfractionRegisteredPayload(
                saved.getInfractionId(),
                saved.getLicensePlate(),
                saved.getInfractionType(),
                saved.getInfractionDateTime(),
                saved.getBaseAmount(),
                saved.getAggravatingFactors(),
                saved.getFinalAmount(),
                saved.getOriginOperationId(),
                saved.getOriginTicketId(),
                saved.getOriginParkingSessionId())));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Infraction> findAll() {
        // Dos consultas con JOIN (no N+1 ni lotes con IN de cientos de ids, que en Postgres recorren la tabla por lote).
        List<Infraction> all = infractionRepository.findAllFetchingEvidenceUrl();
        infractionRepository.fetchAggravatingFactorsOfAll();
        return all;
    }

    @Transactional(readOnly = true)
    public List<Infraction> findByOffenderNationalId(String offenderNationalId) {
        return withCollections(infractionRepository.findByOffenderNationalId(offenderNationalId));
    }

    @Transactional(readOnly = true)
    public Infraction findById(UUID infractionId) {
        return infractionRepository.findById(infractionId).map(InfractionService::withCollections)
                .orElseThrow(() -> new ResourceNotFoundException("Infraction no encontrada: " + infractionId));
    }

    public Infraction update(UUID infractionId, Infraction changes) {
        Infraction existing = findById(infractionId);
        InfractionStates.of(existing).requireEditable(existing);

        if (changes.getLicensePlate() != null) {
            existing.setLicensePlate(changes.getLicensePlate());
        }
        if (changes.getLocation() != null) {
            existing.setLocation(changes.getLocation());
        }
        if (changes.getEvidenceUrl() != null) {
            existing.setEvidenceUrl(changes.getEvidenceUrl());
        }
        if (changes.getOffenderNationalId() != null) {
            existing.setOffenderNationalId(changes.getOffenderNationalId());
        }
        if (changes.getVehicleOwnerTaxId() != null) {
            existing.setVehicleOwnerTaxId(changes.getVehicleOwnerTaxId());
        }
        if (changes.getDebtorId() != null) {
            existing.setDebtorId(changes.getDebtorId());
            existing.setDebtorIdType(changes.getDebtorIdType());
        }
        if (changes.getBaseAmount() != null) {
            existing.setBaseAmount(changes.getBaseAmount());
        }
        if (changes.getAggravatingFactors() != null) {
            existing.setAggravatingFactors(changes.getAggravatingFactors());
        }
        if (changes.getFinalAmount() != null) {
            existing.setFinalAmount(changes.getFinalAmount());
        }

        if (existing.getFinalAmount().compareTo(existing.getBaseAmount()) < 0) {
            throw new InvalidRequestException("El importe final no puede ser menor que el importe base");
        }

        return infractionRepository.save(existing);
    }

    public Infraction appeal(UUID infractionId, String reason) {
        Infraction existing = findById(infractionId);
        existing.setStatus(InfractionStates.of(existing).appeal(existing).status());
        existing.setAppealReason(reason);
        Infraction saved = infractionRepository.save(existing);

        eventPublisher.publish(EventEnvelope.of("infractionAppealed",
                new InfractionAppealedPayload(saved.getInfractionId(), reason)));

        return saved;
    }

    public Infraction confirm(UUID infractionId) {
        Infraction existing = findById(infractionId);
        existing.setStatus(InfractionStates.of(existing).confirm(existing).status());
        Infraction saved = infractionRepository.save(existing);

        eventPublisher.publish(EventEnvelope.of("infractionConfirmed", new InfractionConfirmedPayload(
                saved.getInfractionId(),
                saved.getDebtorId(),
                saved.getDebtorIdType(),
                saved.getLicensePlate(),
                saved.getInfractionType(),
                toMunicipalOffset(saved.getInfractionDateTime()),
                saved.getBaseAmount(),
                saved.getAggravatingFactors(),
                saved.getFinalAmount(),
                saved.getInspectorId(),
                saved.getLocation())));

        return saved;
    }

    public Infraction annul(UUID infractionId, String annulmentReason, String authorizedBy) {
        Infraction existing = findById(infractionId);
        existing.setStatus(InfractionStates.of(existing).annul(existing).status());
        Infraction saved = infractionRepository.save(existing);

        eventPublisher.publish(EventEnvelope.of("infractionAnulled", new InfractionAnulledPayload(
                saved.getInfractionId(),
                annulmentReason,
                authorizedBy,
                OffsetDateTime.now(MUNICIPAL_TIME_ZONE))));

        return saved;
    }

    /** La hora guardada es local argentina; el contrato v4 exige ISO 8601 con offset (-03:00). */
    static OffsetDateTime toMunicipalOffset(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(MUNICIPAL_TIME_ZONE).toOffsetDateTime();
    }

    /**
     * Inicializa las colecciones lazy DENTRO de la transacción (spring.jpa.open-in-view=false): el controller
     * mapea la entidad después de que el servicio liberó la conexión. Con hibernate.default_batch_fetch_size
     * cada colección se carga en lotes (una consulta cada 500 entidades), no una por fila (N+1).
     */
    private static Infraction withCollections(Infraction e) {
        Hibernate.initialize(e.getEvidenceUrl());
        Hibernate.initialize(e.getAggravatingFactors());
        return e;
    }

    private static List<Infraction> withCollections(List<Infraction> list) {
        list.forEach(InfractionService::withCollections);
        return list;
    }
}
