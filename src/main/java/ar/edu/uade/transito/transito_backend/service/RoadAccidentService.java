package ar.edu.uade.transito.transito_backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.payload.RoadAccidentRegisteredPayload;
import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.RoadAccident;
import ar.edu.uade.transito.transito_backend.model.enums.AccidentStatus;
import ar.edu.uade.transito.transito_backend.repository.RoadAccidentRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RoadAccidentService {

    private static final ZoneId MUNICIPAL_TIME_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final RoadAccidentRepository roadAccidentRepository;
    private final EventPublisher eventPublisher;

    public RoadAccident register(RoadAccident accident) {
        accident.setStatus(AccidentStatus.PRELIMINARY);
        RoadAccident saved = roadAccidentRepository.save(accident);

        eventPublisher.publish(EventEnvelope.of("roadAccidentRegistered", new RoadAccidentRegisteredPayload(
                saved.getAccidentId(),
                saved.getAccidentDateTime().atZone(MUNICIPAL_TIME_ZONE).toOffsetDateTime(),
                saved.getLocation(),
                saved.isInvolvesInfrastructureDamage(),
                saved.getDamageDescription(),
                saved.getVehicles(),
                saved.getPersons(),
                saved.getInspectorId())));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<RoadAccident> findAll() {
        return roadAccidentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<RoadAccident> findInvolving(String nationalId) {
        return roadAccidentRepository.findInvolving(nationalId).stream()
                .sorted(Comparator.comparing(RoadAccident::getAccidentDateTime).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public RoadAccident findById(UUID accidentId) {
        return roadAccidentRepository.findById(accidentId)
                .orElseThrow(() -> new ResourceNotFoundException("RoadAccident no encontrado: " + accidentId));
    }

    public RoadAccident update(UUID accidentId, RoadAccident changes) {
        RoadAccident existing = findById(accidentId);
        if (existing.getStatus() == AccidentStatus.VERIFIED) {
            throw new InvalidStateTransitionException(
                    "El accidente " + accidentId + " ya fue verificado y no se puede modificar");
        }

        existing.setAccidentDateTime(changes.getAccidentDateTime());
        existing.setLocation(changes.getLocation());
        existing.setInvolvesInfrastructureDamage(changes.isInvolvesInfrastructureDamage());
        existing.setDamageDescription(changes.getDamageDescription());
        existing.setVehicles(changes.getVehicles());
        existing.setPersons(changes.getPersons());
        if (changes.getStatus() == AccidentStatus.VERIFIED) {
            existing.setStatus(AccidentStatus.VERIFIED);
        }

        return roadAccidentRepository.save(existing);
    }
}
