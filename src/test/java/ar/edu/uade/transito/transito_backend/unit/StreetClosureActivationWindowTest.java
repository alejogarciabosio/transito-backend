package ar.edu.uade.transito.transito_backend.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.exception.InvalidRequestException;
import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;
import ar.edu.uade.transito.transito_backend.repository.StreetClosureRepository;
import ar.edu.uade.transito.transito_backend.service.StreetClosureService;

/** DEF-42: el servicio toma la hora de un Clock inyectable y respeta la ventana autorizada. */
class StreetClosureActivationWindowTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-15T19:46:41-03:00");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-11-16T06:00:00-03:00");

    private final UUID id = UUID.randomUUID();
    private final StreetClosureRepository repo = mock(StreetClosureRepository.class);
    private final EventPublisher publisher = mock(EventPublisher.class);

    private StreetClosureService serviceAt(String instant, StreetClosure closure) {
        when(repo.findById(id)).thenReturn(Optional.of(closure));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return new StreetClosureService(repo, publisher,
                Clock.fixed(OffsetDateTime.parse(instant).toInstant(), ZoneOffset.UTC));
    }

    private StreetClosure closure(StreetClosureStatus status) {
        return StreetClosure.builder().streetClosureId(id).closureRequestId("REQ-1")
                .requestingModule(RequestingModule.OBRAS).startDate(START).endDate(END)
                .detours(new ArrayList<>()).conditions(new ArrayList<>()).status(status).build();
    }

    @Test
    @DisplayName("Activar antes del inicio → 409, el corte sigue APPROVED, no se guarda ni se publica nada")
    void activateBeforeStart() {
        StreetClosure closure = closure(StreetClosureStatus.APPROVED);
        var service = serviceAt("2026-11-15T19:45:51-03:00", closure);
        assertThatThrownBy(() -> service.activate(id, "USR-1"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("dentro de la ventana autorizada");
        assertThat(closure.getStatus()).isEqualTo(StreetClosureStatus.APPROVED);
        verify(repo, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    @DisplayName("Activar dentro de la ventana → ACTIVE")
    void activateInsideWindow() {
        StreetClosure closure = closure(StreetClosureStatus.APPROVED);
        assertThat(serviceAt("2026-11-15T19:46:41-03:00", closure).activate(id, "USR-1").getStatus())
                .isEqualTo(StreetClosureStatus.ACTIVE);
    }

    @Test
    @DisplayName("Activar después del fin → 409")
    void activateAfterEnd() {
        var service = serviceAt("2026-11-16T06:00:00-03:00", closure(StreetClosureStatus.APPROVED));
        assertThatThrownBy(() -> service.activate(id, "USR-1")).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("Finalizar un ACTIVE antes del endDate está permitido (cierre anticipado)")
    void finishBeforeEndIsAllowed() {
        var service = serviceAt("2026-11-15T21:00:00-03:00", closure(StreetClosureStatus.ACTIVE));
        assertThat(service.finish(id, "Obra terminada antes").getStatus()).isEqualTo(StreetClosureStatus.FINISHED);
    }

    @Test
    @DisplayName("Aprobar con ventana invertida o vacía (start >= end) → 400 sin cambiar estado ni publicar")
    void approveWithInvertedWindow() {
        StreetClosure closure = closure(StreetClosureStatus.REQUESTED);
        var service = serviceAt("2026-11-15T10:00:00-03:00", closure);
        for (OffsetDateTime to : List.of(START, START.minusHours(1))) {
            assertThatThrownBy(() -> service.approve(id, StreetClosure.builder().detours(List.of())
                    .conditions(List.of()).startDate(START).endDate(to).build()))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("anterior");
        }
        assertThat(closure.getStatus()).isEqualTo(StreetClosureStatus.REQUESTED);
        verifyNoInteractions(publisher);
    }

    @Test
    @DisplayName("Aprobar fijando un fin ya pasado → 400")
    void approveWithEndInThePast() {
        StreetClosure closure = closure(StreetClosureStatus.REQUESTED);
        var service = serviceAt("2026-11-16T07:00:00-03:00", closure);
        assertThatThrownBy(() -> service.approve(id, StreetClosure.builder().detours(List.of())
                .conditions(List.of()).endDate(END).build()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("pasado");
        assertThat(closure.getStatus()).isEqualTo(StreetClosureStatus.REQUESTED);
    }

    @Test
    @DisplayName("Aprobar fijando inicio en el pasado y fin futuro (corte inmediato) → OK")
    void approveWithStartInThePastIsAllowed() {
        StreetClosure closure = closure(StreetClosureStatus.REQUESTED);
        var service = serviceAt("2026-11-15T20:00:00-03:00", closure);
        assertThat(service.approve(id, StreetClosure.builder().detours(List.of()).conditions(List.of())
                .startDate(START).endDate(END).build()).getStatus()).isEqualTo(StreetClosureStatus.APPROVED);
    }
}
