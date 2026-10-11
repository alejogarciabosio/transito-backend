package ar.edu.uade.transito.transito_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.model.ParkingSession;
import ar.edu.uade.transito.transito_backend.model.RoadAccident;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingSessionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingZoneRepository;
import ar.edu.uade.transito.transito_backend.repository.RoadAccidentRepository;
import ar.edu.uade.transito.transito_backend.service.InfractionService;
import ar.edu.uade.transito.transito_backend.service.ParkingSessionService;
import ar.edu.uade.transito.transito_backend.service.RoadAccidentService;

class SecurityIdentityTests {

    @Test
    void accidentRegistrationUsesAuthenticatedInspectorInsteadOfEntityValue() {
        RoadAccidentRepository repository = mock(RoadAccidentRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> {
            RoadAccident accident = invocation.getArgument(0);
            accident.setAccidentId(UUID.randomUUID());
            return accident;
        });
        RoadAccidentService service = new RoadAccidentService(repository, mock(EventPublisher.class));
        RoadAccident accident = RoadAccident.builder()
                .accidentDateTime(LocalDateTime.now().minusMinutes(10))
                .inspectorId("usuario-falsificado")
                .build();

        RoadAccident saved = service.register(accident, "inspector-jwt-1");

        assertEquals("inspector-jwt-1", saved.getInspectorId());
        assertNotNull(saved.getAccidentId());
    }

    @Test
    void citizenCannotCloseAnotherUsersParkingSession() {
        UUID id = UUID.randomUUID();
        ParkingSession session = ParkingSession.builder()
                .sessionId(id)
                .startedByUserId("ciudadano-1")
                .build();
        ParkingSessionRepository repository = mock(ParkingSessionRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(session));
        ParkingSessionService service = parkingService(repository);

        assertThrows(AccessDeniedException.class,
                () -> service.close(id, "ciudadano-2", false));
    }

    @Test
    void controlOperatorCanCloseAnyParkingSession() {
        UUID id = UUID.randomUUID();
        ParkingSession session = ParkingSession.builder()
                .sessionId(id)
                .startedByUserId("ciudadano-1")
                .build();
        ParkingSessionRepository repository = mock(ParkingSessionRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(session));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ParkingSessionService service = parkingService(repository);

        ParkingSession closed = service.close(id, "operador-1", true);

        assertNotNull(closed.getEndDateTime());
    }

    private ParkingSessionService parkingService(ParkingSessionRepository repository) {
        return new ParkingSessionService(
                repository,
                mock(ParkingZoneRepository.class),
                mock(InfractionRepository.class),
                mock(InfractionService.class));
    }
}
