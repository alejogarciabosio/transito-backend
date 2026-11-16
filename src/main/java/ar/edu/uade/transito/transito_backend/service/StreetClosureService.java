package ar.edu.uade.transito.transito_backend.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureApprovedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureEndedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.StreetClosureRejectedPayload;
import ar.edu.uade.transito.transito_backend.exception.InvalidRequestException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;
import ar.edu.uade.transito.transito_backend.model.state.StreetClosureState;
import ar.edu.uade.transito.transito_backend.model.state.StreetClosureStates;
import ar.edu.uade.transito.transito_backend.repository.StreetClosureRepository;

/**
 * A diferencia del resto de los servicios, {@link #createFromRequest} no se expone vía POST:
 * un StreetClosure nace de consumir el evento streetClosureRequested (Obras/Ambiente), no de un
 * pedido del frontend. Hoy no hay broker/listener configurado, así que este método queda listo
 * para que un futuro consumer de eventos lo invoque.
 */
@Service
@Transactional
public class StreetClosureService {

    private static final ZoneId MUNICIPAL_TIME_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final StreetClosureRepository streetClosureRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    /** Constructor para tests unitarios: reloj del sistema. */
    public StreetClosureService(StreetClosureRepository streetClosureRepository, EventPublisher eventPublisher) {
        this(streetClosureRepository, eventPublisher, Clock.systemUTC());
    }

    @Autowired
    public StreetClosureService(StreetClosureRepository streetClosureRepository, EventPublisher eventPublisher,
            Clock clock) {
        this.streetClosureRepository = streetClosureRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock.withZone(MUNICIPAL_TIME_ZONE));
    }

    public StreetClosure createFromRequest(StreetClosure closure) {
        closure.setStatus(StreetClosureStatus.REQUESTED);
        return streetClosureRepository.save(closure);
    }

    @Transactional(readOnly = true)
    public List<StreetClosure> findAll() {
        return withCollections(streetClosureRepository.findAll());
    }

    /**
     * Cortes vigentes para la consulta ciudadana: los ya aprobados (próximos a activarse)
     * y los activos. Los solicitados, rechazados y finalizados no se exponen al vecino.
     */
    @Transactional(readOnly = true)
    public List<StreetClosure> findCurrent() {
        return withCollections(streetClosureRepository.findByStatusInOrderByStartDateAsc(
                List.of(StreetClosureStatus.APPROVED, StreetClosureStatus.ACTIVE)));
    }

    @Transactional(readOnly = true)
    public StreetClosure findById(UUID streetClosureId) {
        return streetClosureRepository.findById(streetClosureId).map(StreetClosureService::withCollections)
                .orElseThrow(() -> new ResourceNotFoundException("StreetClosure no encontrado: " + streetClosureId));
    }

    public StreetClosure approve(UUID streetClosureId, StreetClosure changes) {
        StreetClosure existing = findById(streetClosureId);
        StreetClosureState next = stateOf(existing).approve(existing);
        validateAuthorizedWindow(existing, changes);
        transition(existing, next);
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
        transition(existing, stateOf(existing).activate(existing, now()));
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
                now(),
                notes), saved.getSourceCorrelationId(), saved.getSourceEventId()));

        return saved;
    }

    /**
     * Applies the operational window agreed with Obras. ACTIVE closures are ended and
     * APPROVED closures which never activated become EXPIRED, so they cannot activate later.
     * (Procesa todo en una sola transacción; el scheduler usa {@link #expireClosure} por corte.)
     */
    public void expireDueClosures(OffsetDateTime now) {
        for (StreetClosure closure : streetClosureRepository
                .findByStatusAndEndDateLessThanEqual(StreetClosureStatus.ACTIVE, now)) {
            expire(closure);
        }

        for (StreetClosure closure : streetClosureRepository
                .findByStatusAndEndDateLessThanEqual(StreetClosureStatus.APPROVED, now)) {
            expire(closure);
        }
    }

    /** Ids de los cortes ACTIVE/APPROVED cuyo horario autorizado ya venció. */
    @Transactional(readOnly = true)
    public List<UUID> findClosuresDueForExpiration(OffsetDateTime now) {
        List<UUID> ids = new ArrayList<>();
        for (StreetClosureStatus status : List.of(StreetClosureStatus.ACTIVE, StreetClosureStatus.APPROVED)) {
            streetClosureRepository.findByStatusAndEndDateLessThanEqual(status, now)
                    .forEach(closure -> ids.add(closure.getStreetClosureId()));
        }
        return ids;
    }

    /**
     * Vence UN corte en su propia transacción (la del proxy de este servicio): si falla, solo se revierte
     * ese corte, y su evento streetClosureEnded (que va al outbox en la misma transacción) tampoco sale.
     * Vuelve a validar el estado y el horario por si el corte cambió desde que se listó.
     */
    public boolean expireClosure(UUID streetClosureId, OffsetDateTime now) {
        // Repositorio directo (no this.findById): no mezclar un @Transactional(readOnly) en una llamada interna.
        StreetClosure closure = streetClosureRepository.findById(streetClosureId)
                .orElseThrow(() -> new ResourceNotFoundException("StreetClosure no encontrado: " + streetClosureId));
        boolean due = (closure.getStatus() == StreetClosureStatus.ACTIVE
                || closure.getStatus() == StreetClosureStatus.APPROVED)
                && closure.getEndDate() != null && !closure.getEndDate().isAfter(now);
        if (due) {
            expire(closure);
        }
        return due;
    }

    private void expire(StreetClosure closure) {
        boolean wasActive = closure.getStatus() == StreetClosureStatus.ACTIVE;
        transition(closure, stateOf(closure).expire(closure));
        StreetClosure saved = streetClosureRepository.save(closure);
        if (wasActive) {
            eventPublisher.publish(EventEnvelope.of("streetClosureEnded", new StreetClosureEndedPayload(
                    saved.getStreetClosureId(), saved.getClosureRequestId(), now(),
                    "Cierre automático al vencer el horario autorizado."), saved.getSourceCorrelationId(), saved.getSourceEventId()));
        }
    }

    /**
     * DEF-42: la ventana autorizada resultante debe tener inicio anterior al fin; y si la aprobación fija un
     * fin nuevo, no puede estar ya vencido. Un inicio en el pasado con fin futuro es válido (corte inmediato).
     */
    private void validateAuthorizedWindow(StreetClosure existing, StreetClosure changes) {
        OffsetDateTime start = changes.getStartDate() != null ? changes.getStartDate() : existing.getStartDate();
        OffsetDateTime end = changes.getEndDate() != null ? changes.getEndDate() : existing.getEndDate();
        if (start != null && end != null && !start.isBefore(end)) {
            throw new InvalidRequestException("El inicio de la ventana autorizada (" + start
                    + ") debe ser anterior al fin (" + end + ")");
        }
        if (changes.getEndDate() != null && !changes.getEndDate().isAfter(now())) {
            throw new InvalidRequestException("El fin de la ventana autorizada (" + changes.getEndDate()
                    + ") no puede estar en el pasado");
        }
    }

    /** Patrón State: el objeto estado decide si la operación es válida (si no, 409). */
    private static StreetClosureState stateOf(StreetClosure closure) {
        return StreetClosureStates.of(closure);
    }

    private static void transition(StreetClosure closure, StreetClosureState next) {
        closure.setStatus(next.status());
    }

    /**
     * Inicializa las colecciones lazy DENTRO de la transacción (spring.jpa.open-in-view=false): el controller
     * mapea la entidad después de que el servicio liberó la conexión. Con hibernate.default_batch_fetch_size
     * cada colección se carga en lotes (una consulta cada 500 entidades), no una por fila (N+1).
     */
    private static StreetClosure withCollections(StreetClosure e) {
        Hibernate.initialize(e.getAffectedStreet());
        Hibernate.initialize(e.getAffectedSections());
        Hibernate.initialize(e.getDetours());
        Hibernate.initialize(e.getConditions());
        return e;
    }

    private static List<StreetClosure> withCollections(List<StreetClosure> list) {
        list.forEach(StreetClosureService::withCollections);
        return list;
    }
}
