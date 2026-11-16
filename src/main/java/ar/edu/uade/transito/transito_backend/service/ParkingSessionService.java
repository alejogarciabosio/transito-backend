package ar.edu.uade.transito.transito_backend.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.transito.transito_backend.config.MunicipalTime;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.ParkingSession;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;
import ar.edu.uade.transito.transito_backend.model.enums.Role;
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
    private final Clock clock;

    /**
     * Inicia la sesión registrando como dueño al ciudadano autenticado (DEF-15). Si la inicia
     * otro rol (inspector), el dueño queda null. El dueño nunca se toma del body.
     */
    public ParkingSession start(ParkingSession session, AuthenticatedUser user) {
        session.setOwnerUsername(user != null && user.role() == Role.CIUDADANO ? user.userId() : null);
        return start(session);
    }

    public ParkingSession start(ParkingSession session) {
        var zone = parkingZoneRepository.findById(session.getZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("ParkingZone no encontrada: " + session.getZoneId()));
        if (zone.getStatus() != ar.edu.uade.transito.transito_backend.model.enums.ParkingZoneStatus.ACTIVE) {
            throw new InvalidStateTransitionException("La zona " + session.getZoneId() + " no está activa");
        }
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

    /**
     * Igual que {@link #findById(UUID)} pero aplicando autorización a nivel de objeto (DEF-15 / KI-1):
     * un CIUDADANO solo ve sus propias sesiones; si no es suya (o es legacy sin dueño) se responde
     * exactamente igual que un id inexistente (404), para no revelar su existencia. Otros roles: sin cambios.
     */
    @Transactional(readOnly = true)
    public ParkingSession findById(UUID sessionId, AuthenticatedUser user) {
        ParkingSession session = findById(sessionId);
        if (user != null && user.role() == Role.CIUDADANO
                && (session.getOwnerUsername() == null || !session.getOwnerUsername().equals(user.userId()))) {
            throw new ResourceNotFoundException("ParkingSession no encontrada: " + sessionId);
        }
        return session;
    }

    /** Cierra la sesión verificando primero la pertenencia (antes de cualquier validación de estado o escritura). */
    public ParkingSession close(UUID sessionId, AuthenticatedUser user) {
        return doClose(findById(sessionId, user));
    }

    public ParkingSession close(UUID sessionId) {
        return doClose(findById(sessionId));
    }

    private ParkingSession doClose(ParkingSession existing) {
        UUID sessionId = existing.getSessionId();
        if (existing.getEndDateTime() != null) {
            throw new InvalidStateTransitionException("La sesión " + sessionId + " ya está cerrada");
        }
        existing.setEndDateTime(MunicipalTime.now(clock));
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
                .infractionDateTime(MunicipalTime.now(clock))
                .inspectorId(inspectorId)
                .baseAmount(baseAmount)
                .finalAmount(baseAmount)
                .originParkingSessionId(session.getSessionId())
                .build();

        return infractionService.register(infraction);
    }
}
