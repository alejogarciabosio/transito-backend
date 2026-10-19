package ar.edu.uade.transito.transito_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.Location;
import ar.edu.uade.transito.transito_backend.model.ParkingTariff;
import ar.edu.uade.transito.transito_backend.model.RoadAccident;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.model.enums.AccidentStatus;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;
import ar.edu.uade.transito.transito_backend.model.enums.OperationResolution;
import ar.edu.uade.transito.transito_backend.model.enums.OperationStatus;
import ar.edu.uade.transito.transito_backend.model.enums.ParkingTariffStatus;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingSessionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingTariffRepository;
import ar.edu.uade.transito.transito_backend.repository.RoadAccidentRepository;
import ar.edu.uade.transito.transito_backend.repository.StreetClosureRepository;
import ar.edu.uade.transito.transito_backend.repository.TrafficOperationRepository;
import ar.edu.uade.transito.transito_backend.service.InfractionService;
import ar.edu.uade.transito.transito_backend.service.ParkingTariffService;
import ar.edu.uade.transito.transito_backend.service.RoadAccidentService;
import ar.edu.uade.transito.transito_backend.service.StreetClosureService;
import ar.edu.uade.transito.transito_backend.service.TrafficOperationService;
import ar.edu.uade.transito.transito_backend.service.command.UpdateRoadAccidentCommand;

class StateTransitionTests {

    @Test
    void accidentPatchKeepsOmittedFieldsAndCanRemoveDamage() {
        UUID id = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.now().minusDays(1);
        Location location = Location.builder().street("Av. Siempre Viva 742").build();
        RoadAccident existing = RoadAccident.builder()
                .accidentId(id)
                .accidentDateTime(occurredAt)
                .location(location)
                .involvesInfrastructureDamage(true)
                .damageDescription("Semáforo dañado")
                .vehicles(List.of())
                .persons(List.of())
                .status(AccidentStatus.PRELIMINARY)
                .build();
        RoadAccidentRepository repository = mock(RoadAccidentRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RoadAccidentService service = new RoadAccidentService(repository, mock(EventPublisher.class));

        RoadAccident updated = service.update(id,
                new UpdateRoadAccidentCommand(null, null, false, null, null, null, null));

        assertEquals(occurredAt, updated.getAccidentDateTime());
        assertEquals(location, updated.getLocation());
        assertEquals(AccidentStatus.PRELIMINARY, updated.getStatus());
        assertNull(updated.getDamageDescription());
    }

    @Test
    void verifiedAccidentCannotBeModified() {
        UUID id = UUID.randomUUID();
        RoadAccident existing = RoadAccident.builder()
                .accidentId(id)
                .status(AccidentStatus.VERIFIED)
                .build();
        RoadAccidentRepository repository = mock(RoadAccidentRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        RoadAccidentService service = new RoadAccidentService(repository, mock(EventPublisher.class));

        assertThrows(InvalidStateTransitionException.class,
                () -> service.update(id,
                        new UpdateRoadAccidentCommand(null, null, null, null, null, null, null)));
    }

    @Test
    void tariffDeletionIsLogicalAndPreservesTheRecord() {
        UUID id = UUID.randomUUID();
        ParkingTariff tariff = ParkingTariff.builder()
                .tariffId(id)
                .zoneId(UUID.randomUUID())
                .pricePerHour(new BigDecimal("1500"))
                .validFrom(LocalDate.now().minusMonths(1))
                .validTo(LocalDate.now().plusMonths(1))
                .status(ParkingTariffStatus.ACTIVE)
                .build();
        ParkingTariffRepository repository = mock(ParkingTariffRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(tariff));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ParkingSessionRepository sessionRepository = mock(ParkingSessionRepository.class);
        when(sessionRepository.findByZoneId(tariff.getZoneId())).thenReturn(List.of());
        ParkingTariffService service = new ParkingTariffService(repository, sessionRepository);

        service.delete(id);

        assertEquals(ParkingTariffStatus.INACTIVE, tariff.getStatus());
        verify(repository).save(tariff);
        verify(repository, never()).delete(any());
    }

    @Test
    void streetClosureCannotActivateBeforeApproval() {
        UUID id = UUID.randomUUID();
        StreetClosure closure = StreetClosure.builder()
                .streetClosureId(id)
                .status(StreetClosureStatus.REQUESTED)
                .build();
        StreetClosureRepository repository = mock(StreetClosureRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(closure));
        StreetClosureService service = new StreetClosureService(repository, mock(EventPublisher.class));

        assertThrows(InvalidStateTransitionException.class, () -> service.activate(id, "USR-1"));
    }

    @Test
    void finalInfractionCannotBeModified() {
        UUID id = UUID.randomUUID();
        Infraction infraction = Infraction.builder()
                .infractionId(id)
                .status(InfractionStatus.CONFIRMED)
                .build();
        InfractionRepository repository = mock(InfractionRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(infraction));
        InfractionService service = new InfractionService(repository, mock(EventPublisher.class));

        assertThrows(InvalidStateTransitionException.class,
                () -> service.update(id, Infraction.builder().licensePlate("AB123CD").build()));
    }

    @Test
    void closedOperationCannotBeClosedAgain() {
        UUID id = UUID.randomUUID();
        TrafficOperation operation = TrafficOperation.builder()
                .operationId(id)
                .status(OperationStatus.CLOSED)
                .build();
        TrafficOperationRepository repository = mock(TrafficOperationRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(operation));
        TrafficOperationService service = new TrafficOperationService(repository, mock(EventPublisher.class));

        assertThrows(InvalidStateTransitionException.class,
                () -> service.close(id, OperationResolution.NO_ACTION));
    }
}
