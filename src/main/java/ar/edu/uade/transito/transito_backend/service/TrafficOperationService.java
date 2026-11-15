package ar.edu.uade.transito.transito_backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.payload.TrafficOperationCreatedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.UpdateTicketStatusPayload;
import ar.edu.uade.transito.transito_backend.exception.InvalidRequestException;
import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.model.enums.OperationResolution;
import ar.edu.uade.transito.transito_backend.model.enums.OperationStatus;
import ar.edu.uade.transito.transito_backend.repository.TrafficOperationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TrafficOperationService {

    private final TrafficOperationRepository trafficOperationRepository;
    private final EventPublisher eventPublisher;

    public TrafficOperation create(TrafficOperation operation) {
        operation.setStatus(OperationStatus.OPEN);
        operation.setResolution(null);
        TrafficOperation saved = trafficOperationRepository.save(operation);

        eventPublisher.publish(EventEnvelope.of("trafficOperationCreated", new TrafficOperationCreatedPayload(
                saved.getOperationId(),
                saved.getType(),
                saved.getZone(),
                saved.getStartDateTime(),
                saved.getAssignedInspectors(),
                saved.getAssignedUnits(),
                saved.getTicketId())));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<TrafficOperation> findAll() {
        return withCollections(trafficOperationRepository.findAll());
    }

    @Transactional(readOnly = true)
    public TrafficOperation findById(UUID operationId) {
        return trafficOperationRepository.findById(operationId).map(TrafficOperationService::withCollections)
                .orElseThrow(() -> new ResourceNotFoundException("TrafficOperation no encontrado: " + operationId));
    }

    public TrafficOperation update(UUID operationId, TrafficOperation changes) {
        TrafficOperation existing = findById(operationId);
        requireOpen(existing);

        if (changes.getZone() != null) {
            existing.setZone(changes.getZone());
        }
        if (changes.getAssignedInspectors() != null) {
            existing.setAssignedInspectors(changes.getAssignedInspectors());
        }
        if (changes.getAssignedUnits() != null) {
            existing.setAssignedUnits(changes.getAssignedUnits());
        }
        if (changes.getEndDateTime() != null) {
            if (changes.getEndDateTime().isBefore(existing.getStartDateTime())) {
                throw new InvalidRequestException(
                        "La fecha de finalización no puede ser anterior a la fecha de inicio");
            }
            existing.setEndDateTime(changes.getEndDateTime());
        }

        return trafficOperationRepository.save(existing);
    }

    public TrafficOperation close(UUID operationId, OperationResolution resolution) {
        TrafficOperation existing = findById(operationId);
        requireOpen(existing);

        existing.setStatus(OperationStatus.CLOSED);
        existing.setResolution(resolution);
        if (existing.getEndDateTime() == null) {
            existing.setEndDateTime(LocalDateTime.now());
        }

        TrafficOperation closed = trafficOperationRepository.save(existing);
        publishTicketCompletion(closed);
        return closed;
    }

    private void publishTicketCompletion(TrafficOperation operation) {
        if (operation.getTicketId() == null || operation.getTicketId().isBlank()) {
            return;
        }
        try {
            UUID ticketId = UUID.fromString(operation.getTicketId());
            // NO_ACTION: el reclamo se verificó y no requiere acción; no es un rechazo del reclamo.
            boolean noAction = operation.getResolution() == OperationResolution.NO_ACTION;
            String resolutionType = noAction ? "NO_FURTHER_ACTION_REQUIRED" : "ACTION_COMPLETED";
            String message = noAction
                    ? "Tránsito verificó el reclamo y no fue necesario tomar acciones adicionales."
                    : "La verificación de Tránsito finalizó satisfactoriamente y se tomaron las medidas correspondientes.";
            String internalMessage = "Operativo " + operation.getOperationId()
                    + "; resolución " + operation.getResolution().name();
            eventPublisher.publish(EventEnvelope.of("updateTicketStatus",
                    UpdateTicketStatusPayload.resolved(ticketId, message, internalMessage, resolutionType),
                    operation.getSourceCorrelationId(), operation.getSourceEventId()));
        } catch (IllegalArgumentException ignored) {
            // Only M2-originated operations use UUID ticket identifiers and receive M2 updates.
        }
    }

    private void requireOpen(TrafficOperation operation) {
        if (operation.getStatus() != OperationStatus.OPEN) {
            throw new InvalidStateTransitionException(
                    "El operativo " + operation.getOperationId() + " ya está cerrado");
        }
    }

    /**
     * Inicializa las colecciones lazy DENTRO de la transacción (spring.jpa.open-in-view=false): el controller
     * mapea la entidad después de que el servicio liberó la conexión. Con hibernate.default_batch_fetch_size
     * cada colección se carga en lotes (una consulta cada 500 entidades), no una por fila (N+1).
     */
    private static TrafficOperation withCollections(TrafficOperation e) {
        Hibernate.initialize(e.getAssignedInspectors());
        Hibernate.initialize(e.getAssignedUnits());
        return e;
    }

    private static List<TrafficOperation> withCollections(List<TrafficOperation> list) {
        list.forEach(TrafficOperationService::withCollections);
        return list;
    }
}
