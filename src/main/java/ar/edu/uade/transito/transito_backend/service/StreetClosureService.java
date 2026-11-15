package ar.edu.uade.transito.transito_backend.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureApprovedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureEndedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureRejectedPayload;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;
import ar.edu.uade.transito.transito_backend.model.state.StreetClosureState;
import ar.edu.uade.transito.transito_backend.model.state.StreetClosureStates;
import ar.edu.uade.transito.transito_backend.repository.StreetClosureRepository;
import lombok.RequiredArgsConstructor;

/**
 * A diferencia del resto de los servicios, {@link #createFromRequest} no se expone vía POST:
 * un StreetClosure nace de consumir el evento streetClosureRequested (Obras/Ambiente), no de un
 * pedido del frontend. Hoy no hay broker/listener configurado, así que este método queda listo
 * para que un futuro consumer de eventos lo invoque.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StreetClosureService {

    private static final ZoneId MUNICIPAL_TIME_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final StreetClosureRepository streetClosureRepository;
    private final EventPublisher eventPublisher;

    public StreetClosure createFromRequest(StreetClosure closure) {
        closure.setStatus(StreetClosureStatus.REQUESTED);
        return streetClosureRepository.save(closure);
    }

    @Transactional(readOnly = true)
    public List<StreetClosure> findAll() {
        return streetClosureRepository.findAll();
    }

    /**
     * Cortes vigentes para la consulta ciudadana: los ya aprobados (próximos a activarse)
     * y los activos. Los solicitados, rechazados y finalizados no se exponen al vecino.
     */
    @Transactional(readOnly = true)
    public List<StreetClosure> findCurrent() {
        return streetClosureRepository.findByStatusInOrderByStartDateAsc(
                List.of(StreetClosureStatus.APPROVED, StreetClosureStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public StreetClosure findById(UUID streetClosureId) {
        return streetClosureRepository.findById(streetClosureId)
                .orElseThrow(() -> new ResourceNotFoundException("StreetClosure no encontrado: " + streetClosureId));
    }

    public StreetClosure approve(UUID streetClosureId, StreetClosure changes) {
        StreetClosure existing = findById(streetClosureId);
        transition(existing, stateOf(existing).approve(existing));
        existing.setDetours(changes.getDetours());
        existing.setConditions(changes.getConditions());
        if (changes.getStartDate() != null) {
            existing.setStartDate(changes.getStartDate());
        }
        if (changes.getEndDate() != null) {
            existing.setEndDate(changes.getEndDate());
        }
        StreetClosure saved = streetClosureRepository.save(existing);

        eventPublisher.publish(EventEnvelope.of("streetClosureApproved", new StreetClosureApprovedPayload(
                saved.getStreetClosureId(),
                saved.getClosureRequestId(),
                saved.getRequestingModule().technicalName(),
                saved.getStartDate(),
                saved.getEndDate(),
                List.copyOf(saved.getAffectedSections()),
                List.copyOf(saved.getDetours()),
                List.copyOf(saved.getConditions())), saved.getSourceCorrelationId(), saved.getSourceEventId()));

        return saved;
    }

    public StreetClosure reject(UUID streetClosureId, String rejectionReason) {
        StreetClosure existing = findById(streetClosureId);
        transition(existing, stateOf(existing).reject(existing));
        StreetClosure saved = streetClosureRepository.save(existing);

        eventPublisher.publish(EventEnvelope.of("streetClosureRejected", new StreetClosureRejectedPayload(
                saved.getClosureRequestId(),
                rejectionReason,
                saved.getRequestingModule().technicalName()), saved.getSourceCorrelationId(), saved.getSourceEventId()));

        return saved;
    }

    public StreetClosure activate(UUID streetClosureId, String activatedBy) {
        StreetClosure existing = findById(streetClosureId);
        transition(existing, stateOf(existing).activate(existing));
        StreetClosure saved = streetClosureRepository.save(existing);

        return saved;
    }

    public StreetClosure finish(UUID streetClosureId, String notes) {
        StreetClosure existing = findById(streetClosureId);
        transition(existing, stateOf(existing).finish(existing));
        StreetClosure saved = streetClosureRepository.save(existing);

        eventPublisher.publish(EventEnvelope.of("streetClosureEnded", new StreetClosureEndedPayload(
                saved.getStreetClosureId(),
                saved.getClosureRequestId(),
                OffsetDateTime.now(MUNICIPAL_TIME_ZONE),
                notes), saved.getSourceCorrelationId(), saved.getSourceEventId()));

        return saved;
    }

    /**
     * Applies the operational window agreed with Obras. ACTIVE closures are ended and
     * APPROVED closures which never activated become EXPIRED, so they cannot activate later.
     */
    public void expireDueClosures(OffsetDateTime now) {
        for (StreetClosure closure : streetClosureRepository
                .findByStatusAndEndDateLessThanEqual(StreetClosureStatus.ACTIVE, now)) {
            transition(closure, stateOf(closure).expire(closure));
            StreetClosure saved = streetClosureRepository.save(closure);
            eventPublisher.publish(EventEnvelope.of("streetClosureEnded", new StreetClosureEndedPayload(
                    saved.getStreetClosureId(), saved.getClosureRequestId(), OffsetDateTime.now(MUNICIPAL_TIME_ZONE),
                    "Cierre automático al vencer el horario autorizado."), saved.getSourceCorrelationId(), saved.getSourceEventId()));
        }

        for (StreetClosure closure : streetClosureRepository
                .findByStatusAndEndDateLessThanEqual(StreetClosureStatus.APPROVED, now)) {
            transition(closure, stateOf(closure).expire(closure));
            streetClosureRepository.save(closure);
        }
    }

    /** Patrón State: el objeto estado decide si la operación es válida (si no, 409). */
    private static StreetClosureState stateOf(StreetClosure closure) {
        return StreetClosureStates.of(closure);
    }

    private static void transition(StreetClosure closure, StreetClosureState next) {
        closure.setStatus(next.status());
    }
}
