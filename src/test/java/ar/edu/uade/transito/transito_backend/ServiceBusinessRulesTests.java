package ar.edu.uade.transito.transito_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.exception.InvalidRequestException;
import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.ParkingSession;
import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.model.VehicleImpoundment;
import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentStatus;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;
import ar.edu.uade.transito.transito_backend.model.enums.OperationStatus;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingSessionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingZoneRepository;
import ar.edu.uade.transito.transito_backend.repository.TrafficOperationRepository;
import ar.edu.uade.transito.transito_backend.repository.VehicleImpoundmentRepository;
import ar.edu.uade.transito.transito_backend.service.InfractionService;
import ar.edu.uade.transito.transito_backend.service.ParkingSessionService;
import ar.edu.uade.transito.transito_backend.service.TrafficOperationService;
import ar.edu.uade.transito.transito_backend.service.VehicleImpoundmentService;

class ServiceBusinessRulesTests {

    @Test
    void infractionPatchKeepsFieldsThatWereNotSent() {
        UUID id = UUID.randomUUID();
        Infraction existing = Infraction.builder()
                .infractionId(id)
                .licensePlate("AB123CD")
                .baseAmount(new BigDecimal("50000"))
                .finalAmount(new BigDecimal("50000"))
                .aggravatingFactors(List.of())
                .status(InfractionStatus.PENDING_APPEAL)
                .build();
        InfractionRepository repository = mock(InfractionRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        InfractionService service = new InfractionService(repository, mock(EventPublisher.class));

        Infraction updated = service.update(id, Infraction.builder().licensePlate("AC456EF").build());

        assertEquals("AC456EF", updated.getLicensePlate());
        assertEquals(new BigDecimal("50000"), updated.getBaseAmount());
        assertEquals(new BigDecimal("50000"), updated.getFinalAmount());
    }

    @Test
    void infractionPatchRejectsAnInvalidFinalAmountAfterMerge() {
        UUID id = UUID.randomUUID();
        Infraction existing = Infraction.builder()
                .infractionId(id)
                .baseAmount(new BigDecimal("50000"))
                .finalAmount(new BigDecimal("50000"))
                .status(InfractionStatus.PENDING_APPEAL)
                .build();
        InfractionRepository repository = mock(InfractionRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        InfractionService service = new InfractionService(repository, mock(EventPublisher.class));

        assertThrows(InvalidRequestException.class,
                () -> service.update(id, Infraction.builder().baseAmount(new BigDecimal("60000")).build()));
    }

    @Test
    void operationPatchKeepsUnspecifiedFields() {
        UUID id = UUID.randomUUID();
        TrafficOperation existing = TrafficOperation.builder()
                .operationId(id)
                .zone("Palermo")
                .startDateTime(LocalDateTime.now().minusHours(1))
                .assignedInspectors(List.of("INS-1"))
                .assignedUnits(List.of("UNIT-1"))
                .status(OperationStatus.OPEN)
                .build();
        TrafficOperationRepository repository = mock(TrafficOperationRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TrafficOperationService service = new TrafficOperationService(repository, mock(EventPublisher.class));

        TrafficOperation updated = service.update(id,
                TrafficOperation.builder()
                        .assignedInspectors(null)
                        .assignedUnits(List.of("UNIT-2"))
                        .build());

        assertEquals("Palermo", updated.getZone());
        assertEquals(List.of("INS-1"), updated.getAssignedInspectors());
        assertEquals(List.of("UNIT-2"), updated.getAssignedUnits());
    }

    @Test
    void vehicleReleaseRequiresPaymentReferenceWhenThereIsAFee() {
        UUID id = UUID.randomUUID();
        VehicleImpoundment existing = VehicleImpoundment.builder()
                .impoundmentId(id)
                .status(ImpoundmentStatus.IMPOUNDED)
                .build();
        VehicleImpoundmentRepository repository = mock(VehicleImpoundmentRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        VehicleImpoundmentService service = new VehicleImpoundmentService(
                repository, mock(EventPublisher.class));

        assertThrows(InvalidRequestException.class, () -> service.release(
                id, List.of("DOCUMENTATION_OK"), new BigDecimal("85000"), null, "USR-1"));
    }

    @Test
    void parkingSessionCannotGenerateMoreThanOneUnpaidInfraction() {
        UUID id = UUID.randomUUID();
        ParkingSession session = ParkingSession.builder()
                .sessionId(id)
                .licensePlate("AB123CD")
                .paid(false)
                .build();
        ParkingSessionRepository sessionRepository = mock(ParkingSessionRepository.class);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));
        InfractionRepository infractionRepository = mock(InfractionRepository.class);
        when(infractionRepository.existsByOriginParkingSessionId(id)).thenReturn(true);
        ParkingSessionService service = new ParkingSessionService(
                sessionRepository,
                infractionRepository,
                mock(InfractionService.class));

        assertThrows(InvalidStateTransitionException.class,
                () -> service.flagUnpaid(id, "INS-1", new BigDecimal("50000")));
    }
}
