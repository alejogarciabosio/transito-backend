package ar.edu.uade.transito.transito_backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.ParkingSession;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingSessionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingZoneRepository;
import ar.edu.uade.transito.transito_backend.model.Location;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ParkingSessionService {

    private final ParkingSessionRepository parkingSessionRepository;
    private final InfractionRepository infractionRepository;
    private final ParkingZoneRepository parkingZoneRepository;
    private final InfractionService infractionService;

    public ParkingSession start(ParkingSession session) {
        session.setPaid(false);
        session.setEndDateTime(null);
        session.setPaymentReference(null);
        return parkingSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<ParkingSession> findAll() {
        return parkingSessionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ParkingSession findById(UUID sessionId) {
        return parkingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ParkingSession no encontrada: " + sessionId));
    }

    public ParkingSession close(UUID sessionId) {
        ParkingSession existing = findById(sessionId);
        if (existing.getEndDateTime() != null) {
            throw new InvalidStateTransitionException("La sesión " + sessionId + " ya está cerrada");
        }
        existing.setEndDateTime(LocalDateTime.now());
        return parkingSessionRepository.save(existing);
    }

    /**
     * Reutiliza el circuito de Infracciones: crea una Infraction con infractionType = UNPAID_PARKING
     * referenciando la sesión, en vez de emitir un evento nuevo hacia otros módulos.
     */
    public Infraction flagUnpaid(UUID sessionId, String inspectorId, BigDecimal baseAmount) {
        ParkingSession session = findById(sessionId);
        if (session.isPaid()) {
            throw new InvalidStateTransitionException("La sesión " + sessionId + " ya está paga");
        }
        if (infractionRepository.existsByOriginParkingSessionId(sessionId)) {
            throw new InvalidStateTransitionException(
                    "La sesión " + sessionId + " ya tiene una infracción por falta de pago");
        }

        // La infracción exige ubicación: se toma de la zona de estacionamiento de la sesión.
        Location location = java.util.Optional.ofNullable(session.getZoneId())
                .flatMap(parkingZoneRepository::findById)
                .map(zone -> Location.builder().street(zone.getName()).crossStreet(zone.getBoundaries()).build())
                .orElseGet(() -> Location.builder().street("Zona de estacionamiento " + session.getZoneId()).build());

        Infraction infraction = Infraction.builder()
                .licensePlate(session.getLicensePlate())
                .infractionType(InfractionType.UNPAID_PARKING)
                .location(location)
                .infractionDateTime(LocalDateTime.now())
                .inspectorId(inspectorId)
                .baseAmount(baseAmount)
                .finalAmount(baseAmount)
                .originParkingSessionId(session.getSessionId())
                .build();

        return infractionService.register(infraction);
    }
}
