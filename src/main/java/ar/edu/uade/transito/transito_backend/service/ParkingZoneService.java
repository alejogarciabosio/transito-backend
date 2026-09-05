package ar.edu.uade.transito.transito_backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.ParkingZone;
import ar.edu.uade.transito.transito_backend.model.enums.ParkingZoneStatus;
import ar.edu.uade.transito.transito_backend.repository.ParkingZoneRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ParkingZoneService {

    private final ParkingZoneRepository parkingZoneRepository;

    public ParkingZone create(ParkingZone zone) {
        zone.setStatus(ParkingZoneStatus.ACTIVE);
        return parkingZoneRepository.save(zone);
    }

    @Transactional(readOnly = true)
    public List<ParkingZone> findAll() {
        return parkingZoneRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ParkingZone findById(UUID zoneId) {
        return parkingZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("ParkingZone no encontrada: " + zoneId));
    }

    public ParkingZone update(UUID zoneId, ParkingZone changes) {
        ParkingZone existing = findById(zoneId);

        existing.setName(changes.getName());
        existing.setBoundaries(changes.getBoundaries());
        existing.setAllowedHours(changes.getAllowedHours());
        existing.setStatus(changes.getStatus());

        return parkingZoneRepository.save(existing);
    }

    /**
     * Baja lógica (no física): las sesiones históricas quedan intactas referenciando la zona.
     */
    public void delete(UUID zoneId) {
        ParkingZone existing = findById(zoneId);
        existing.setStatus(ParkingZoneStatus.INACTIVE);
        parkingZoneRepository.save(existing);
    }
}
