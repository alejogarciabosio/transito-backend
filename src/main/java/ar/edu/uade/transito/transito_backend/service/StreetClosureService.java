package ar.edu.uade.transito.transito_backend.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureActivatedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureApprovedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureEndedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureRejectedPayload;
import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;
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
        requireStatus(existing, StreetClosureStatus.REQUESTED);

        existing.setStatus(StreetClosureStatus.APPROVED);
        existing.setDetours(changes.getDetours());
        existing.setConditions(changes.getConditions());
        StreetClosure saved = streetClosureRepository.save(existing);

        eventPublisher.publish(EventEnvelope.of("streetClosureApproved", new StreetClosureApprovedPayload(
                saved.getStreetClosureId(),
                saved.getClosureRequestId(),
                saved.getRequestingModule(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getAffectedStreet(),
                saved.getDetours(),
                saved.getConditions())));

        return saved;
    }

    public StreetClosure reject(UUID streetClosureId, String rejectionReason) {
        StreetClosure existing = findById(streetClosureId);
        requireStatus(existing, StreetClosureStatus.REQUESTED);

        existing.setStatus(StreetClosureStatus.REJECTED);
        StreetClosure saved = streetClosureRepository.save(existing);

        eventPublisher.publish(EventEnvelope.of("streetClosureRejected", new StreetClosureRejectedPayload(
                saved.getClosureRequestId(),
                rejectionReason,
                saved.getRequestingModule())));

        return saved;
    }

    public StreetClosure activate(UUID streetClosureId, String activatedBy) {
        StreetClosure existing = findById(streetClosureId);
        requireStatus(existing, StreetClosureStatus.APPROVED);

        existing.setStatus(StreetClosureStatus.ACTIVE);
        StreetClosure saved = streetClosureRepository.save(existing);

        eventPublisher.publish(EventEnvelope.of("streetClosureActivated", new StreetClosureActivatedPayload(
                saved.getStreetClosureId(),
                saved.getClosureRequestId(),
                saved.getRequestingModule().code(),
                LocalDateTime.now(),
                activatedBy)));

        return saved;
    }

    public StreetClosure finish(UUID streetClosureId, String notes) {
        StreetClosure existing = findById(streetClosureId);
        requireStatus(existing, StreetClosureStatus.ACTIVE);

        existing.setStatus(StreetClosureStatus.FINISHED);
        StreetClosure saved = streetClosureRepository.save(existing);

        eventPublisher.publish(EventEnvelope.of("streetClosureEnded", new StreetClosureEndedPayload(
                saved.getStreetClosureId(),
                saved.getClosureRequestId(),
                LocalDateTime.now(),
                notes)));

        return saved;
    }

    /** Finaliza únicamente cortes activos cuya vigencia solicitada ya terminó. */
    public int finishExpired(OffsetDateTime now) {
        List<StreetClosure> expired = streetClosureRepository
                .findByStatusAndEndDateLessThanEqual(StreetClosureStatus.ACTIVE, now);
        expired.forEach(closure -> finish(closure.getStreetClosureId(), "Cierre automático por vencimiento"));
        return expired.size();
    }

    private void requireStatus(StreetClosure closure, StreetClosureStatus expected) {
        if (closure.getStatus() != expected) {
            throw new InvalidStateTransitionException(
                    "El corte de calle " + closure.getStreetClosureId() + " debe estar en estado " + expected
                            + " (actual: " + closure.getStatus() + ")");
        }
    }
}
