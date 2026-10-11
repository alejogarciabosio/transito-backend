package ar.edu.uade.transito.transito_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.ParkingSession;
import ar.edu.uade.transito.transito_backend.model.ParkingTariff;
import ar.edu.uade.transito.transito_backend.model.ParkingZone;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.model.VehicleImpoundment;
import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentStatus;
import ar.edu.uade.transito.transito_backend.model.enums.OperationResolution;
import ar.edu.uade.transito.transito_backend.model.enums.OperationStatus;
import ar.edu.uade.transito.transito_backend.model.enums.OperationType;
import ar.edu.uade.transito.transito_backend.model.enums.ParkingTariffStatus;
import ar.edu.uade.transito.transito_backend.model.enums.ParkingZoneStatus;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingSessionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingTariffRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingZoneRepository;
import ar.edu.uade.transito.transito_backend.repository.StreetClosureRepository;
import ar.edu.uade.transito.transito_backend.repository.TrafficOperationRepository;
import ar.edu.uade.transito.transito_backend.repository.VehicleImpoundmentRepository;
import ar.edu.uade.transito.transito_backend.service.InfractionService;
import ar.edu.uade.transito.transito_backend.service.ParkingSessionService;
import ar.edu.uade.transito.transito_backend.service.ParkingTariffService;
import ar.edu.uade.transito.transito_backend.service.ParkingZoneService;
import ar.edu.uade.transito.transito_backend.service.StreetClosureService;
import ar.edu.uade.transito.transito_backend.service.TrafficOperationService;
import ar.edu.uade.transito.transito_backend.service.VehicleImpoundmentService;

class AdditionalServiceCoverageTests {

    @Test
    void streetClosureCompletesTheApprovedLifecycle() {
        UUID id = UUID.randomUUID();
        StreetClosureRepository repository = mock(StreetClosureRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        StreetClosureService service = new StreetClosureService(repository, mock(EventPublisher.class));
        StreetClosure closure = closure(id, StreetClosureStatus.REQUESTED);
        when(repository.findById(id)).thenReturn(Optional.of(closure));

        service.approve(id, StreetClosure.builder()
                .detours(List.of())
                .conditions(List.of("Mantener acceso peatonal"))
                .build());
        assertEquals(StreetClosureStatus.APPROVED, closure.getStatus());

        service.activate(id, "operador-1");
        assertEquals(StreetClosureStatus.ACTIVE, closure.getStatus());

        service.finish(id, "Calle liberada");
        assertEquals(StreetClosureStatus.FINISHED, closure.getStatus());
    }

    @Test
    void streetClosureCanBeRejectedOnlyWhileRequested() {
        UUID id = UUID.randomUUID();
        StreetClosureRepository repository = mock(StreetClosureRepository.class);
        StreetClosure closure = closure(id, StreetClosureStatus.REQUESTED);
        when(repository.findById(id)).thenReturn(Optional.of(closure));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        StreetClosureService service = new StreetClosureService(repository, mock(EventPublisher.class));

        service.reject(id, "Fechas incompatibles");

        assertEquals(StreetClosureStatus.REJECTED, closure.getStatus());
        assertThrows(InvalidStateTransitionException.class,
                () -> service.reject(id, "Segundo rechazo"));
    }

    @Test
    void vehicleImpoundmentCanBeRegisteredUpdatedAndReleased() {
        UUID id = UUID.randomUUID();
        VehicleImpoundmentRepository repository = mock(VehicleImpoundmentRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> {
            VehicleImpoundment value = invocation.getArgument(0);
            if (value.getImpoundmentId() == null) {
                value.setImpoundmentId(id);
            }
            return value;
        });
        VehicleImpoundmentService service = new VehicleImpoundmentService(repository, mock(EventPublisher.class));
        VehicleImpoundment impoundment = VehicleImpoundment.builder()
                .licensePlate("AB123CD")
                .reason("Falta grave")
                .depotLocation("Depósito Norte")
                .entryDateTime(LocalDateTime.now().minusHours(2))
                .originId(UUID.randomUUID())
                .build();

        VehicleImpoundment saved = service.register(impoundment);
        when(repository.findById(id)).thenReturn(Optional.of(saved));
        assertEquals(ImpoundmentStatus.IMPOUNDED, saved.getStatus());
        assertNull(saved.getPaymentReference());

        service.update(id, VehicleImpoundment.builder().depotLocation("Depósito Sur").build());
        VehicleImpoundment released = service.release(
                id, List.of("DOCUMENTATION_OK"), new BigDecimal("5000"), "PAY-1", "deposito-1");

        assertEquals(ImpoundmentStatus.RELEASED, released.getStatus());
        assertEquals("PAY-1", released.getPaymentReference());
        assertEquals("deposito-1", released.getReleasedBy());
    }

    @Test
    void activeParkingSessionStartsAndClosesForItsOwner() {
        UUID zoneId = UUID.randomUUID();
        ParkingZoneRepository zoneRepository = mock(ParkingZoneRepository.class);
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(ParkingZone.builder()
                .zoneId(zoneId)
                .status(ParkingZoneStatus.ACTIVE)
                .build()));
        ParkingSessionRepository repository = mock(ParkingSessionRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> {
            ParkingSession session = invocation.getArgument(0);
            if (session.getSessionId() == null) {
                session.setSessionId(UUID.randomUUID());
            }
            return session;
        });
        ParkingSessionService service = new ParkingSessionService(
                repository, zoneRepository, mock(InfractionRepository.class), mock(InfractionService.class));
        ParkingSession session = ParkingSession.builder()
                .licensePlate("AB123CD")
                .zoneId(zoneId)
                .startDateTime(LocalDateTime.now().minusMinutes(5))
                .paid(true)
                .build();

        ParkingSession started = service.start(session, "ciudadano-1");
        when(repository.findById(started.getSessionId())).thenReturn(Optional.of(started));
        ParkingSession closed = service.close(started.getSessionId(), "ciudadano-1", false);

        assertFalse(started.isPaid());
        assertEquals("ciudadano-1", started.getStartedByUserId());
        assertNotNull(closed.getEndDateTime());
    }

    @Test
    void parkingZoneUsesLogicalDeletion() {
        UUID id = UUID.randomUUID();
        ParkingZoneRepository repository = mock(ParkingZoneRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ParkingZoneService service = new ParkingZoneService(repository);
        ParkingZone zone = ParkingZone.builder()
                .zoneId(id)
                .name("Centro")
                .boundaries("A-B-C-D")
                .allowedHours("08:00-20:00")
                .status(ParkingZoneStatus.ACTIVE)
                .build();
        when(repository.findById(id)).thenReturn(Optional.of(zone));

        service.update(id, ParkingZone.builder()
                .name("Microcentro")
                .boundaries("E-F-G-H")
                .allowedHours("07:00-21:00")
                .status(ParkingZoneStatus.ACTIVE)
                .build());
        service.delete(id);

        assertEquals("Microcentro", zone.getName());
        assertEquals(ParkingZoneStatus.INACTIVE, zone.getStatus());
        verify(repository, times(2)).save(zone);
    }

    @Test
    void parkingTariffCreateListAndUpdateKeepActiveState() {
        UUID id = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        ParkingTariffRepository repository = mock(ParkingTariffRepository.class);
        ParkingSessionRepository sessions = mock(ParkingSessionRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> {
            ParkingTariff tariff = invocation.getArgument(0);
            if (tariff.getTariffId() == null) {
                tariff.setTariffId(id);
            }
            return tariff;
        });
        ParkingTariffService service = new ParkingTariffService(repository, sessions);
        ParkingTariff tariff = ParkingTariff.builder()
                .zoneId(zoneId)
                .pricePerHour(new BigDecimal("1000"))
                .validFrom(LocalDate.now())
                .validTo(LocalDate.now().plusMonths(1))
                .build();

        ParkingTariff created = service.create(tariff);
        when(repository.findById(id)).thenReturn(Optional.of(created));
        when(repository.findByZoneIdAndStatusOrderByValidFromDesc(zoneId, ParkingTariffStatus.ACTIVE))
                .thenReturn(List.of(created));
        service.update(id, ParkingTariff.builder()
                .pricePerHour(new BigDecimal("1200"))
                .validFrom(LocalDate.now())
                .validTo(LocalDate.now().plusMonths(2))
                .build());

        assertEquals(ParkingTariffStatus.ACTIVE, created.getStatus());
        assertEquals(new BigDecimal("1200"), created.getPricePerHour());
        assertEquals(List.of(created), service.findByZoneId(zoneId));
    }

    @Test
    void tariffCannotBeDeletedWhileZoneHasOpenSession() {
        UUID id = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        ParkingTariff tariff = ParkingTariff.builder()
                .tariffId(id)
                .zoneId(zoneId)
                .status(ParkingTariffStatus.ACTIVE)
                .build();
        ParkingTariffRepository repository = mock(ParkingTariffRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(tariff));
        ParkingSessionRepository sessions = mock(ParkingSessionRepository.class);
        when(sessions.findByZoneId(zoneId)).thenReturn(List.of(ParkingSession.builder().build()));
        ParkingTariffService service = new ParkingTariffService(repository, sessions);

        assertThrows(InvalidStateTransitionException.class, () -> service.delete(id));
    }

    @Test
    void trafficOperationCreateAndCloseProducesNoActionResolution() {
        UUID id = UUID.randomUUID();
        TrafficOperationRepository repository = mock(TrafficOperationRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> {
            TrafficOperation operation = invocation.getArgument(0);
            if (operation.getOperationId() == null) {
                operation.setOperationId(id);
            }
            return operation;
        });
        TrafficOperationService service = new TrafficOperationService(repository, mock(EventPublisher.class));
        TrafficOperation operation = TrafficOperation.builder()
                .type(OperationType.ROUTINE_CONTROL)
                .zone("Centro")
                .startDateTime(LocalDateTime.now().minusHours(1))
                .assignedInspectors(List.of("inspector-1"))
                .assignedUnits(List.of("movil-1"))
                .build();

        TrafficOperation created = service.create(operation);
        when(repository.findById(id)).thenReturn(Optional.of(created));
        TrafficOperation closed = service.close(id, OperationResolution.NO_ACTION);

        assertEquals(OperationStatus.CLOSED, closed.getStatus());
        assertEquals(OperationResolution.NO_ACTION, closed.getResolution());
        assertNotNull(closed.getEndDateTime());
    }

    @Test
    void missingResourcesReturnNotFound() {
        UUID id = UUID.randomUUID();
        ParkingZoneRepository zones = mock(ParkingZoneRepository.class);
        when(zones.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> new ParkingZoneService(zones).findById(id));
    }

    private StreetClosure closure(UUID id, StreetClosureStatus status) {
        return StreetClosure.builder()
                .streetClosureId(id)
                .closureRequestId("REQ-1")
                .requestingModule(RequestingModule.OBRAS)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .affectedStreet(List.of("Av. Rivadavia"))
                .status(status)
                .build();
    }
}
