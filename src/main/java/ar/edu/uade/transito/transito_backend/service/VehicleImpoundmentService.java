package ar.edu.uade.transito.transito_backend.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.config.MunicipalTime;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.payload.VehicleImpoundedPayload;
import ar.edu.uade.transito.transito_backend.event.payload.VehicleReleasedPayload;
import ar.edu.uade.transito.transito_backend.exception.InvalidRequestException;
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
    private final Clock clock;

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
        return withCollections(vehicleImpoundmentRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<VehicleImpoundment> findByOwnerNationalId(String ownerNationalId) {
        return withCollections(vehicleImpoundmentRepository.findByOwnerNationalId(ownerNationalId));
    }

    @Transactional(readOnly = true)
    public VehicleImpoundment findById(UUID impoundmentId) {
        return vehicleImpoundmentRepository.findById(impoundmentId).map(VehicleImpoundmentService::withCollections)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleImpoundment no encontrado: " + impoundmentId));
    }

    public VehicleImpoundment update(UUID impoundmentId, VehicleImpoundment changes) {
        VehicleImpoundment existing = findById(impoundmentId);
        requireImpounded(existing);

        if (changes.getReason() != null) {
            existing.setReason(changes.getReason());
        }
        if (changes.getDepotLocation() != null) {
            existing.setDepotLocation(changes.getDepotLocation());
        }
        if (changes.getOwnerNationalId() != null) {
            existing.setOwnerNationalId(changes.getOwnerNationalId());
        }

        return vehicleImpoundmentRepository.save(existing);
    }

    public VehicleImpoundment release(
            UUID impoundmentId,
            List<String> verifiedRequirements,
            BigDecimal towingAndStorageFee,
            String paymentReference,
            String releasedBy) {
        VehicleImpoundment existing = findById(impoundmentId);
        requireImpounded(existing);
        validateRelease(verifiedRequirements, towingAndStorageFee, paymentReference);

        existing.setStatus(ImpoundmentStatus.RELEASED);
        existing.setVerifiedRequirements(verifiedRequirements);
        existing.setTowingAndStorageFee(towingAndStorageFee);
        existing.setPaymentReference(paymentReference);
        existing.setReleasedBy(releasedBy);
        VehicleImpoundment saved = vehicleImpoundmentRepository.save(existing);

        eventPublisher.publish(EventEnvelope.of("vehicleReleased", new VehicleReleasedPayload(
                saved.getImpoundmentId(),
                MunicipalTime.now(clock),
                saved.getVerifiedRequirements(),
                saved.getTowingAndStorageFee(),
                saved.getPaymentReference(),
                saved.getReleasedBy())));

        return saved;
    }

    private void requireImpounded(VehicleImpoundment impoundment) {
        if (impoundment.getStatus() != ImpoundmentStatus.IMPOUNDED) {
            throw new InvalidStateTransitionException(
                    "El vehículo de la retención " + impoundment.getImpoundmentId() + " ya fue liberado");
        }
    }

    private void validateRelease(List<String> verifiedRequirements, BigDecimal towingAndStorageFee,
            String paymentReference) {
        if (verifiedRequirements == null || verifiedRequirements.isEmpty()) {
            throw new InvalidRequestException("Debe verificarse al menos un requisito antes de liberar el vehículo");
        }
        if (towingAndStorageFee == null || towingAndStorageFee.signum() < 0) {
            throw new InvalidRequestException("El costo de acarreo y estadía no puede ser negativo");
        }
        if (towingAndStorageFee.signum() > 0 && (paymentReference == null || paymentReference.isBlank())) {
            throw new InvalidRequestException(
                    "La referencia de pago es obligatoria cuando el costo es mayor que cero");
        }
    }

    /**
     * Inicializa las colecciones lazy DENTRO de la transacción (spring.jpa.open-in-view=false): el controller
     * mapea la entidad después de que el servicio liberó la conexión. Con hibernate.default_batch_fetch_size
     * cada colección se carga en lotes (una consulta cada 500 entidades), no una por fila (N+1).
     */
    private static VehicleImpoundment withCollections(VehicleImpoundment e) {
        Hibernate.initialize(e.getVerifiedRequirements());
        return e;
    }

    private static List<VehicleImpoundment> withCollections(List<VehicleImpoundment> list) {
        list.forEach(VehicleImpoundmentService::withCollections);
        return list;
    }
}
