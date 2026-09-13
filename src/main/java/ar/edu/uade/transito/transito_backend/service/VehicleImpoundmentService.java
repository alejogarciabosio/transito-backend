package ar.edu.uade.transito.transito_backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.payload.VehicleImpoundedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.VehicleReleasedPayload;
import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.VehicleImpoundment;
import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentStatus;
import ar.edu.uade.transito.transito_backend.repository.VehicleImpoundmentRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleImpoundmentService {

    private final VehicleImpoundmentRepository vehicleImpoundmentRepository;
    private final EventPublisher eventPublisher;

    public VehicleImpoundment register(VehicleImpoundment impoundment) {
        impoundment.setStatus(ImpoundmentStatus.IMPOUNDED);
        impoundment.setPaymentReference(null);
        VehicleImpoundment saved = vehicleImpoundmentRepository.save(impoundment);

        eventPublisher.publish(EventEnvelope.of("vehicleImpounded", new VehicleImpoundedPayload(
                saved.getImpoundmentId(),
                saved.getLicensePlate(),
                saved.getOwnerNationalId(),
                saved.getReason(),
                saved.getDepotLocation(),
                saved.getEntryDateTime(),
                saved.getOriginType(),
                saved.getOriginId())));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<VehicleImpoundment> findAll() {
        return vehicleImpoundmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public VehicleImpoundment findById(UUID impoundmentId) {
        return vehicleImpoundmentRepository.findById(impoundmentId)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleImpoundment no encontrado: " + impoundmentId));
    }

    public VehicleImpoundment update(UUID impoundmentId, VehicleImpoundment changes) {
        VehicleImpoundment existing = findById(impoundmentId);
        requireImpounded(existing);

        existing.setReason(changes.getReason());
        existing.setDepotLocation(changes.getDepotLocation());
        existing.setOwnerNationalId(changes.getOwnerNationalId());

        return vehicleImpoundmentRepository.save(existing);
    }

    public VehicleImpoundment release(UUID impoundmentId, String paymentReference) {
        VehicleImpoundment existing = findById(impoundmentId);
        requireImpounded(existing);

        existing.setStatus(ImpoundmentStatus.RELEASED);
        existing.setPaymentReference(paymentReference);
        VehicleImpoundment saved = vehicleImpoundmentRepository.save(existing);

        eventPublisher.publish(EventEnvelope.of("vehicleReleased", new VehicleReleasedPayload(
                saved.getImpoundmentId(),
                saved.getLicensePlate(),
                saved.getPaymentReference(),
                LocalDateTime.now())));

        return saved;
    }

    private void requireImpounded(VehicleImpoundment impoundment) {
        if (impoundment.getStatus() != ImpoundmentStatus.IMPOUNDED) {
            throw new InvalidStateTransitionException(
                    "El vehículo de la retención " + impoundment.getImpoundmentId() + " ya fue liberado");
        }
    }
}
