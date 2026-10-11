package ar.edu.uade.transito.transito_backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.ParkingTariff;
import ar.edu.uade.transito.transito_backend.model.enums.ParkingTariffStatus;
import ar.edu.uade.transito.transito_backend.repository.ParkingSessionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingTariffRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ParkingTariffService {

    private final ParkingTariffRepository parkingTariffRepository;
    private final ParkingSessionRepository parkingSessionRepository;

    public ParkingTariff create(ParkingTariff tariff) {
        tariff.setStatus(ParkingTariffStatus.ACTIVE);
        return parkingTariffRepository.save(tariff);
    }

    @Transactional(readOnly = true)
    public List<ParkingTariff> findByZoneId(UUID zoneId) {
        return parkingTariffRepository.findByZoneIdAndStatusOrderByValidFromDesc(
                zoneId, ParkingTariffStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public ParkingTariff findById(UUID tariffId) {
        return parkingTariffRepository.findById(tariffId)
                .orElseThrow(() -> new ResourceNotFoundException("ParkingTariff no encontrada: " + tariffId));
    }

    public ParkingTariff update(UUID tariffId, ParkingTariff changes) {
        ParkingTariff existing = findById(tariffId);
        requireActive(existing);

        existing.setPricePerHour(changes.getPricePerHour());
        existing.setValidFrom(changes.getValidFrom());
        existing.setValidTo(changes.getValidTo());

        return parkingTariffRepository.save(existing);
    }

    /**
     * Baja lógica: conserva la tarifa para auditoría y para las sesiones históricas.
     */
    public void delete(UUID tariffId) {
        ParkingTariff existing = findById(tariffId);
        if (existing.getStatus() == ParkingTariffStatus.INACTIVE) {
            return;
        }
        boolean hasOpenSessions = parkingSessionRepository.findByZoneId(existing.getZoneId()).stream()
                .anyMatch(session -> session.getEndDateTime() == null);
        if (hasOpenSessions) {
            throw new InvalidStateTransitionException(
                    "No se puede eliminar la tarifa " + tariffId + ": hay sesiones activas dependientes en la zona");
        }
        existing.setStatus(ParkingTariffStatus.INACTIVE);
        parkingTariffRepository.save(existing);
    }

    private void requireActive(ParkingTariff tariff) {
        if (tariff.getStatus() != ParkingTariffStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "La tarifa " + tariff.getTariffId() + " está inactiva y no se puede modificar");
        }
    }
}
