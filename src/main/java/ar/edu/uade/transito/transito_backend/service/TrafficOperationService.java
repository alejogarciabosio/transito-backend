package ar.edu.uade.transito.transito_backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.payload.TrafficOperationCreatedPayload;
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
        return trafficOperationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public TrafficOperation findById(UUID operationId) {
        return trafficOperationRepository.findById(operationId)
                .orElseThrow(() -> new ResourceNotFoundException("TrafficOperation no encontrado: " + operationId));
    }

    public TrafficOperation update(UUID operationId, TrafficOperation changes) {
        TrafficOperation existing = findById(operationId);
        requireOpen(existing);

        existing.setZone(changes.getZone());
        existing.setAssignedInspectors(changes.getAssignedInspectors());
        existing.setAssignedUnits(changes.getAssignedUnits());
        existing.setEndDateTime(changes.getEndDateTime());

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

        return trafficOperationRepository.save(existing);
    }

    private void requireOpen(TrafficOperation operation) {
        if (operation.getStatus() != OperationStatus.OPEN) {
            throw new InvalidStateTransitionException(
                    "El operativo " + operation.getOperationId() + " ya está cerrado");
        }
    }
}
