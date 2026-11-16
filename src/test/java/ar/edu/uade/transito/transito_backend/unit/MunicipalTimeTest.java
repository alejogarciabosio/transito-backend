package ar.edu.uade.transito.transito_backend.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ar.edu.uade.transito.transito_backend.config.MunicipalTime;
import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.payload.VehicleReleasedPayload;
import ar.edu.uade.transito.transito_backend.listener.IncomingEventDispatcher;
import ar.edu.uade.transito.transito_backend.listener.dto.TicketUpdatedPayload;
import ar.edu.uade.transito.transito_backend.listener.handler.TicketUpdatedHandler;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.ParkingSession;
import ar.edu.uade.transito.transito_backend.model.ProcessedEvent;
import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.model.VehicleImpoundment;
import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentStatus;
import ar.edu.uade.transito.transito_backend.model.enums.OperationResolution;
import ar.edu.uade.transito.transito_backend.model.enums.OperationStatus;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingSessionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingZoneRepository;
import ar.edu.uade.transito.transito_backend.repository.ProcessedEventRepository;
import ar.edu.uade.transito.transito_backend.repository.TrafficOperationRepository;
import ar.edu.uade.transito.transito_backend.repository.VehicleImpoundmentRepository;
import ar.edu.uade.transito.transito_backend.service.InfractionService;
import ar.edu.uade.transito.transito_backend.service.ParkingSessionService;
import ar.edu.uade.transito.transito_backend.service.TrafficOperationService;
import ar.edu.uade.transito.transito_backend.service.VehicleImpoundmentService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Defecto de la prueba M2 (15/11): los registros automáticos se guardaban con la hora UTC del servidor.
 * Con un reloj fijo en UTC, las LocalDateTime de negocio deben quedar en hora argentina (UTC-3).
 */
class MunicipalTimeTest {

    /** 15/11/2026 15:27 hora argentina. */
    private static final Clock TICKET_CLOCK = Clock.fixed(Instant.parse("2026-11-15T18:27:00Z"), ZoneOffset.UTC);
    /** 15/11/2026 21:59 hora argentina (ya es 16/11 en UTC). */
    private static final Clock CLOSE_CLOCK = Clock.fixed(Instant.parse("2026-11-16T00:59:00Z"), ZoneOffset.UTC);

    @Test
    void nowUsesMunicipalZoneRegardlessOfClockZone() {
        assertThat(MunicipalTime.now(TICKET_CLOCK)).isEqualTo(LocalDateTime.of(2026, 11, 15, 15, 27));
        assertThat(MunicipalTime.now(CLOSE_CLOCK)).isEqualTo(LocalDateTime.of(2026, 11, 15, 21, 59));
    }

    @Test
    void operationCreatedFromTicketStartsAtArgentineTime() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        TrafficOperationService operations = mock(TrafficOperationService.class);
        JsonNode data = mock(JsonNode.class);
        when(mapper.convertValue(data, TicketUpdatedPayload.class))
                .thenReturn(new TicketUpdatedPayload(UUID.randomUUID(), "M7", "ROUTED", null));

        new TicketUpdatedHandler(mapper, operations, mock(EventPublisher.class), TICKET_CLOCK)
                .handle(data, UUID.randomUUID(), null);

        ArgumentCaptor<TrafficOperation> operation = ArgumentCaptor.forClass(TrafficOperation.class);
        verify(operations).create(operation.capture());
        assertThat(operation.getValue().getStartDateTime()).isEqualTo(LocalDateTime.of(2026, 11, 15, 15, 27));
    }

    @Test
    void operationClosedWithoutEndDateEndsAtArgentineTime() {
        UUID id = UUID.randomUUID();
        TrafficOperationRepository repo = mock(TrafficOperationRepository.class);
        when(repo.findById(id)).thenReturn(Optional.of(TrafficOperation.builder().operationId(id)
                .status(OperationStatus.OPEN).startDateTime(LocalDateTime.of(2026, 11, 15, 15, 27)).build()));
        when(repo.save(any(TrafficOperation.class))).thenAnswer(i -> i.getArgument(0));

        TrafficOperation closed = new TrafficOperationService(repo, mock(EventPublisher.class), CLOSE_CLOCK)
                .close(id, OperationResolution.NO_ACTION);

        assertThat(closed.getEndDateTime()).isEqualTo(LocalDateTime.of(2026, 11, 15, 21, 59));
        assertThat(closed.getEndDateTime()).isAfter(closed.getStartDateTime());
    }

    @Test
    void parkingSessionCloseAndUnpaidInfractionUseArgentineTime() {
        UUID id = UUID.randomUUID();
        ParkingSessionRepository sessions = mock(ParkingSessionRepository.class);
        when(sessions.findById(id)).thenReturn(Optional.of(ParkingSession.builder().sessionId(id)
                .licensePlate("AB123CD").paid(false).build()));
        when(sessions.save(any(ParkingSession.class))).thenAnswer(i -> i.getArgument(0));
        InfractionService infractions = mock(InfractionService.class);
        when(infractions.register(any(Infraction.class))).thenAnswer(i -> i.getArgument(0));
        ParkingSessionService service = new ParkingSessionService(sessions, mock(InfractionRepository.class),
                mock(ParkingZoneRepository.class), infractions, CLOSE_CLOCK);

        assertThat(service.close(id).getEndDateTime()).isEqualTo(LocalDateTime.of(2026, 11, 15, 21, 59));
        assertThat(service.flagUnpaid(id, "INS-1", BigDecimal.TEN).getInfractionDateTime())
                .isEqualTo(LocalDateTime.of(2026, 11, 15, 21, 59));
    }

    @Test
    void vehicleReleaseEventCarriesArgentineTime() {
        UUID id = UUID.randomUUID();
        VehicleImpoundmentRepository repo = mock(VehicleImpoundmentRepository.class);
        when(repo.findById(id)).thenReturn(Optional.of(VehicleImpoundment.builder().impoundmentId(id)
                .status(ImpoundmentStatus.IMPOUNDED).build()));
        when(repo.save(any(VehicleImpoundment.class))).thenAnswer(i -> i.getArgument(0));
        EventPublisher publisher = mock(EventPublisher.class);

        new VehicleImpoundmentService(repo, publisher, TICKET_CLOCK)
                .release(id, List.of("DNI"), BigDecimal.ZERO, null, "inspector");

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<EventEnvelope> env = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(publisher).publish(env.capture());
        assertThat(((VehicleReleasedPayload) env.getValue().getData()).releaseDateTime())
                .isEqualTo(LocalDateTime.of(2026, 11, 15, 15, 27));
    }

    @Test
    void processedEventIsRecordedAtArgentineTime() {
        ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
        JsonNode message = new ObjectMapper().readTree(
                "{\"eventId\":\"83a759ab-95b1-4c31-9dee-2537f960245d\",\"eventType\":\"x\",\"data\":{}}");
        var handler = mock(ar.edu.uade.transito.transito_backend.listener.IncomingEventHandler.class);
        when(handler.eventType()).thenReturn("x");

        new IncomingEventDispatcher(List.of(handler), repository, TICKET_CLOCK).dispatch(message);

        ArgumentCaptor<ProcessedEvent> saved = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getProcessedAt()).isEqualTo(LocalDateTime.of(2026, 11, 15, 15, 27));
    }
}
