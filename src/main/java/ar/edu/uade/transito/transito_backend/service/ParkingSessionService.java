package ar.edu.uade.transito.transito_backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.exception.InvalidRequestException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.ParkingSession;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;
import ar.edu.uade.transito.transito_backend.model.enums.ParkingZoneStatus;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingSessionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingZoneRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ParkingSessionService {

    private final ParkingSessionRepository parkingSessionRepository;
    private final ParkingZoneRepository parkingZoneRepository;
    private final InfractionRepository infractionRepository;
    private final InfractionService infractionService;

    public ParkingSession start(ParkingSession session, String startedByUserId) {
        if (startedByUserId == null || startedByUserId.isBlank()) {
            throw new InvalidRequestException("No se pudo identificar al usuario que inicia el estacionamiento");
        }
        var zone = parkingZoneRepository.findById(session.getZoneId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ParkingZone no encontrada: " + session.getZoneId()));
        if (zone.getStatus() != ParkingZoneStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "No se puede iniciar una sesión en una zona de estacionamiento inactiva");
        }
        if (parkingSessionRepository.existsByLicensePlateIgnoreCaseAndEndDateTimeIsNull(session.getLicensePlate())) {
            throw new InvalidStateTransitionException(
                    "El vehículo " + session.getLicensePlate() + " ya tiene una sesión de estacionamiento activa");
        }
        session.setPaid(false);
        session.setEndDateTime(null);
        session.setPaymentReference(null);
        session.setStartedByUserId(startedByUserId);
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

    public ParkingSession close(UUID sessionId, String requesterUserId, boolean canManageAll) {
        ParkingSession existing = findById(sessionId);
        if (!canManageAll && !Objects.equals(requesterUserId, existing.getStartedByUserId())) {
            throw new AccessDeniedException("La sesión no pertenece al usuario autenticado");
        }
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

        Infraction infraction = Infraction.builder()
                .licensePlate(session.getLicensePlate())
                .infractionType(InfractionType.UNPAID_PARKING)
                .infractionDateTime(LocalDateTime.now())
                .inspectorId(inspectorId)
                .baseAmount(baseAmount)
                .finalAmount(baseAmount)
                .originParkingSessionId(session.getSessionId())
                .build();

        return infractionService.register(infraction);
    }
}
