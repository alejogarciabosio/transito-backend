// Autor: revisado por el equipo (asistido por IA)
package ar.edu.uade.transito.transito_backend.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.exception.InvalidRequestException;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.model.enums.OperationResolution;
import ar.edu.uade.transito.transito_backend.model.enums.OperationStatus;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;
import ar.edu.uade.transito.transito_backend.repository.StreetClosureRepository;
import ar.edu.uade.transito.transito_backend.repository.TrafficOperationRepository;
import ar.edu.uade.transito.transito_backend.service.StreetClosureService;
import ar.edu.uade.transito.transito_backend.service.TrafficOperationService;

/** Tests dirigidos a mutantes sobrevivientes de PIT en operativos y cortes de calle. */
@DisplayName("Operativos y cortes: estado, eventos y límites")
class MutationKillingOperationsClosuresTest {

    @SuppressWarnings("rawtypes")
    private static EventEnvelope published(EventPublisher publisher) {
        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(publisher).publish(captor.capture());
        return captor.getValue();
    }

    @Test
    void operationCreateForcesOpenStatusClearsResolutionAndPublishesCreated() {
        var repo = mock(TrafficOperationRepository.class);
        var publisher = mock(EventPublisher.class);
        var service = new TrafficOperationService(repo, publisher);
        when(repo.save(any(TrafficOperation.class))).thenAnswer(i -> i.getArgument(0));
        var input = TrafficOperation.builder().zone("Centro").status(OperationStatus.CLOSED)
                .resolution(OperationResolution.NO_ACTION).startDateTime(LocalDateTime.now()).build();

        TrafficOperation saved = service.create(input);

        assertThat(saved.getStatus()).isEqualTo(OperationStatus.OPEN);
        assertThat(saved.getResolution()).isNull();
        assertThat(published(publisher).getEventType()).isEqualTo("trafficOperationCreated");
    }

    @Test
    void operationUpdateChangesOnlyProvidedFieldsAndRejectsEndBeforeStart() {
        var repo = mock(TrafficOperationRepository.class);
        var service = new TrafficOperationService(repo, mock(EventPublisher.class));
        UUID id = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 10, 0);
        var existing = TrafficOperation.builder().operationId(id).status(OperationStatus.OPEN).zone("Norte")
                .assignedInspectors(List.of("i1")).assignedUnits(List.of("u1")).startDateTime(start).build();
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(TrafficOperation.class))).thenAnswer(i -> i.getArgument(0));

        TrafficOperation onlyZone = new TrafficOperation();
        onlyZone.setZone("Sur");
        onlyZone.setAssignedInspectors(null);
        onlyZone.setAssignedUnits(null);
        TrafficOperation r = service.update(id, onlyZone);
        assertThat(r.getZone()).isEqualTo("Sur");
        assertThat(r.getAssignedInspectors()).containsExactly("i1");
        assertThat(r.getAssignedUnits()).containsExactly("u1");
        assertThat(r.getEndDateTime()).isNull();

        TrafficOperation before = new TrafficOperation();
        before.setEndDateTime(start.minusMinutes(1));
        assertThatThrownBy(() -> service.update(id, before)).isInstanceOf(InvalidRequestException.class);

        // Fin exactamente igual al inicio es válido (límite).
        TrafficOperation same = new TrafficOperation();
        same.setEndDateTime(start);
        same.setAssignedInspectors(List.of("i2"));
        same.setAssignedUnits(List.of("u2"));
        r = service.update(id, same);
        assertThat(r.getEndDateTime()).isEqualTo(start);
        assertThat(r.getAssignedInspectors()).containsExactly("i2");
        assertThat(r.getAssignedUnits()).containsExactly("u2");
    }

    @Test
    void operationCloseWithUuidTicketPublishesResolvedOrRejectedAndSetsEnd() {
        var repo = mock(TrafficOperationRepository.class);
        var publisher = mock(EventPublisher.class);
        var service = new TrafficOperationService(repo, publisher);
        UUID id = UUID.randomUUID();
        UUID ticket = UUID.randomUUID();
        var op = TrafficOperation.builder().operationId(id).status(OperationStatus.OPEN)
                .ticketId(ticket.toString()).startDateTime(LocalDateTime.now().minusHours(1)).build();
        when(repo.findById(id)).thenReturn(Optional.of(op));
        when(repo.save(any(TrafficOperation.class))).thenAnswer(i -> i.getArgument(0));

        TrafficOperation closed = service.close(id, OperationResolution.NO_ACTION);

        assertThat(closed.getStatus()).isEqualTo(OperationStatus.CLOSED);
        assertThat(closed.getResolution()).isEqualTo(OperationResolution.NO_ACTION);
        assertThat(closed.getEndDateTime()).isNotNull();
        var env = published(publisher);
        assertThat(env.getEventType()).isEqualTo("updateTicketStatus");
        assertThat(env.getData().toString()).contains("RESOLVED").contains(ticket.toString())
                .contains("NO_FURTHER_ACTION_REQUIRED").contains("no fue necesario").doesNotContain("REJECTED");
    }

    @Test
    void operationCloseResolvedAndNonUuidOrBlankTicketBehaviour() {
        var repo = mock(TrafficOperationRepository.class);
        var publisher = mock(EventPublisher.class);
        var service = new TrafficOperationService(repo, publisher);
        when(repo.save(any(TrafficOperation.class))).thenAnswer(i -> i.getArgument(0));

        UUID resolvedId = UUID.randomUUID();
        var resolved = TrafficOperation.builder().operationId(resolvedId).status(OperationStatus.OPEN)
                .ticketId(UUID.randomUUID().toString()).startDateTime(LocalDateTime.now()).build();
        when(repo.findById(resolvedId)).thenReturn(Optional.of(resolved));
        service.close(resolvedId, OperationResolution.INFRACTION_CREATED);
        var env = published(publisher);
        assertThat(env.getData().toString()).contains("RESOLVED").contains("satisfactoriamente")
                .contains("ACTION_COMPLETED").contains("Operativo " + resolvedId + "; resolución INFRACTION_CREATED");

        for (String legacyTicket : new String[] {"TCK-123", "", "  ", null}) {
            UUID id = UUID.randomUUID();
            var op = TrafficOperation.builder().operationId(id).status(OperationStatus.OPEN).ticketId(legacyTicket)
                    .startDateTime(LocalDateTime.now()).build();
            when(repo.findById(id)).thenReturn(Optional.of(op));
            service.close(id, OperationResolution.NO_ACTION);
            assertThat(op.getStatus()).isEqualTo(OperationStatus.CLOSED);
        }
        // Solo se publicó el evento del ticket UUID (los tickets legados/ausentes no generan evento).
        verify(publisher).publish(any());
    }

    @Test
    void closureCreateFromRequestForcesRequestedAndApproveCopiesDatesAndPublishes() {
        var repo = mock(StreetClosureRepository.class);
        var publisher = mock(EventPublisher.class);
        var service = new StreetClosureService(repo, publisher);
        when(repo.save(any(StreetClosure.class))).thenAnswer(i -> i.getArgument(0));

        StreetClosure created = service.createFromRequest(StreetClosure.builder().status(StreetClosureStatus.ACTIVE).build());
        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo(StreetClosureStatus.REQUESTED);

        UUID id = UUID.randomUUID();
        OffsetDateTime oldStart = OffsetDateTime.parse("2026-06-01T08:00:00Z");
        OffsetDateTime oldEnd = OffsetDateTime.parse("2026-06-02T08:00:00Z");
        var existing = StreetClosure.builder().streetClosureId(id).closureRequestId("REQ-1").status(StreetClosureStatus.REQUESTED)
                .requestingModule(RequestingModule.OBRAS).startDate(oldStart).endDate(oldEnd).build();
        when(repo.findById(id)).thenReturn(Optional.of(existing));

        // Sin fechas nuevas se conservan las originales.
        StreetClosure keep = service.approve(id, new StreetClosure());
        assertThat(keep.getStatus()).isEqualTo(StreetClosureStatus.APPROVED);
        assertThat(keep.getStartDate()).isEqualTo(oldStart);
        assertThat(keep.getEndDate()).isEqualTo(oldEnd);
        var env = published(publisher);
        assertThat(env.getEventType()).isEqualTo("streetClosureApproved");
        assertThat(env.getData().toString()).contains("REQ-1").contains("obras");
    }

    @Test
    void closureApproveReplacesDatesWhenProvided() {
        var repo = mock(StreetClosureRepository.class);
        // DEF-42: reloj fijo anterior a la ventana nueva (un fin en el pasado se rechaza con 400)
        var service = new StreetClosureService(repo, mock(EventPublisher.class),
                java.time.Clock.fixed(java.time.Instant.parse("2026-05-15T12:00:00Z"), java.time.ZoneOffset.UTC));
        UUID id = UUID.randomUUID();
        var existing = StreetClosure.builder().streetClosureId(id).status(StreetClosureStatus.REQUESTED)
                .requestingModule(RequestingModule.AMBIENTE)
                .startDate(OffsetDateTime.parse("2026-06-01T08:00:00Z")).endDate(OffsetDateTime.parse("2026-06-02T08:00:00Z")).build();
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(StreetClosure.class))).thenAnswer(i -> i.getArgument(0));
        var changes = new StreetClosure();
        changes.setStartDate(OffsetDateTime.parse("2026-07-01T08:00:00Z"));
        changes.setEndDate(OffsetDateTime.parse("2026-07-03T08:00:00Z"));
        changes.setConditions(List.of("solo peatones"));

        StreetClosure r = service.approve(id, changes);

        assertThat(r.getStartDate()).isEqualTo(OffsetDateTime.parse("2026-07-01T08:00:00Z"));
        assertThat(r.getEndDate()).isEqualTo(OffsetDateTime.parse("2026-07-03T08:00:00Z"));
        assertThat(r.getConditions()).containsExactly("solo peatones");
        verify(repo, never()).findAll();
    }
}
