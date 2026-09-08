package ar.edu.uade.transito.transito_backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.ParkingTariff;
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
        return parkingTariffRepository.save(tariff);
    }

    @Transactional(readOnly = true)
    public List<ParkingTariff> findByZoneId(UUID zoneId) {
        return parkingTariffRepository.findByZoneId(zoneId);
    }

    @Transactional(readOnly = true)
    public ParkingTariff findById(UUID tariffId) {
        return parkingTariffRepository.findById(tariffId)
                .orElseThrow(() -> new ResourceNotFoundException("ParkingTariff no encontrada: " + tariffId));
    }

    public ParkingTariff update(UUID tariffId, ParkingTariff changes) {
        ParkingTariff existing = findById(tariffId);

        existing.setPricePerHour(changes.getPricePerHour());
        existing.setValidFrom(changes.getValidFrom());
        existing.setValidTo(changes.getValidTo());

        return parkingTariffRepository.save(existing);
    }

    /**
     * Baja física, solo si no hay sesiones de estacionamiento todavía abiertas en esa zona.
     * Simplificación: no cruza contra el rango de vigencia de la tarifa, solo si hay alguna
     * sesión sin cerrar en la zona.
     */
    public void delete(UUID tariffId) {
        ParkingTariff existing = findById(tariffId);
        boolean hasOpenSessions = parkingSessionRepository.findByZoneId(existing.getZoneId()).stream()
                .anyMatch(session -> session.getEndDateTime() == null);
        if (hasOpenSessions) {
            throw new InvalidStateTransitionException(
                    "No se puede eliminar la tarifa " + tariffId + ": hay sesiones activas dependientes en la zona");
        }
        parkingTariffRepository.delete(existing);
    }
}
