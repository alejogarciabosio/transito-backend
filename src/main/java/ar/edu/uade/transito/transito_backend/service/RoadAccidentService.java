package ar.edu.uade.transito.transito_backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.payload.RoadAccidentRegisteredPayload;
import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.exception.InvalidRequestException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.RoadAccident;
import ar.edu.uade.transito.transito_backend.model.enums.AccidentStatus;
import ar.edu.uade.transito.transito_backend.repository.RoadAccidentRepository;
import ar.edu.uade.transito.transito_backend.service.command.UpdateRoadAccidentCommand;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RoadAccidentService {

    private final RoadAccidentRepository roadAccidentRepository;
    private final EventPublisher eventPublisher;

    public RoadAccident register(RoadAccident accident, String inspectorId) {
        if (inspectorId == null || inspectorId.isBlank()) {
            throw new InvalidRequestException("No se pudo identificar al inspector autenticado");
        }
        accident.setInspectorId(inspectorId);
        accident.setStatus(AccidentStatus.PRELIMINARY);
        RoadAccident saved = roadAccidentRepository.save(accident);

        eventPublisher.publish(EventEnvelope.of("roadAccidentRegistered", new RoadAccidentRegisteredPayload(
                saved.getAccidentId(),
                saved.getAccidentDateTime(),
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

    public RoadAccident update(UUID accidentId, UpdateRoadAccidentCommand changes) {
        RoadAccident existing = findById(accidentId);
        if (existing.getStatus() == AccidentStatus.VERIFIED) {
            throw new InvalidStateTransitionException(
                    "El accidente " + accidentId + " ya fue verificado y no se puede modificar");
        }

        if (changes.accidentDateTime() != null) {
            existing.setAccidentDateTime(changes.accidentDateTime());
        }
        if (changes.location() != null) {
            existing.setLocation(changes.location());
        }
        if (changes.involvesInfrastructureDamage() != null) {
            existing.setInvolvesInfrastructureDamage(changes.involvesInfrastructureDamage());
            if (!changes.involvesInfrastructureDamage()) {
                existing.setDamageDescription(null);
            }
        }
        if (changes.damageDescription() != null) {
            existing.setDamageDescription(changes.damageDescription());
        }
        if (changes.vehicles() != null) {
            existing.setVehicles(changes.vehicles());
        }
        if (changes.persons() != null) {
            existing.setPersons(changes.persons());
        }
        if (existing.isInvolvesInfrastructureDamage()
                && (existing.getDamageDescription() == null || existing.getDamageDescription().isBlank())) {
            throw new InvalidRequestException(
                    "La descripción del daño es obligatoria cuando existe daño de infraestructura");
        }
        if (changes.status() != null && changes.status() != AccidentStatus.VERIFIED) {
            throw new InvalidStateTransitionException(
                    "Un accidente preliminar solo puede pasar al estado " + AccidentStatus.VERIFIED);
        }
        if (changes.status() == AccidentStatus.VERIFIED) {
            existing.setStatus(AccidentStatus.VERIFIED);
        }

        return roadAccidentRepository.save(existing);
    }
}
