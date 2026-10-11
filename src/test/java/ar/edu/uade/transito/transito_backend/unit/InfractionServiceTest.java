package ar.edu.uade.transito.transito_backend.unit;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import ar.edu.uade.transito.transito_backend.service.InfractionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Matriz de transiciones de estado de Infraction, aislada de la base y de la web. */
@ExtendWith(MockitoExtension.class)
@DisplayName("InfractionService — máquina de estados (unit)")
class InfractionServiceTest {

    @Mock InfractionRepository repository;
    @Mock EventPublisher publisher;
    @InjectMocks InfractionService service;

    private final UUID id = UUID.randomUUID();

    private void givenStatus(InfractionStatus status) {
        Infraction i = Infraction.builder().infractionId(id).licensePlate("AA").status(status).build();
        when(repository.findById(id)).thenReturn(Optional.of(i));
        org.mockito.Mockito.lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("TC-BE-INF-U01: findById inexistente → ResourceNotFoundException")
    void notFound() {
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @ParameterizedTest(name = "TC-BE-INF-U02: apelar desde {0} → 409 salvo PENDING_APPEAL")
    @EnumSource(InfractionStatus.class)
    void appealMatrix(InfractionStatus from) {
        givenStatus(from);
        if (from == InfractionStatus.PENDING_APPEAL) {
            assertThat(service.appeal(id, "r").getStatus()).isEqualTo(InfractionStatus.APPEALED);
        } else {
            assertThatThrownBy(() -> service.appeal(id, "r")).isInstanceOf(InvalidStateTransitionException.class);
            verify(publisher, never()).publish(any());
        }
    }

    @ParameterizedTest(name = "TC-BE-INF-U03: confirmar desde {0}")
    @EnumSource(InfractionStatus.class)
    void confirmMatrix(InfractionStatus from) {
        givenStatus(from);
        boolean allowed = from == InfractionStatus.PENDING_APPEAL || from == InfractionStatus.APPEALED;
        if (allowed) {
            assertThat(service.confirm(id).getStatus()).isEqualTo(InfractionStatus.CONFIRMED);
            ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
            verify(publisher).publish(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo("infractionConfirmed");
        } else {
            assertThatThrownBy(() -> service.confirm(id)).isInstanceOf(InvalidStateTransitionException.class);
        }
    }

    @ParameterizedTest(name = "TC-BE-INF-U04: anular / editar desde {0}")
    @EnumSource(InfractionStatus.class)
    void annulAndUpdateMatrix(InfractionStatus from) {
        givenStatus(from);
        boolean isFinal = from == InfractionStatus.CONFIRMED || from == InfractionStatus.ANNULLED;
        if (isFinal) {
            assertThatThrownBy(() -> service.annul(id, "r", "a")).isInstanceOf(InvalidStateTransitionException.class);
            assertThatThrownBy(() -> service.update(id, new Infraction())).isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("ya está firme");
        } else {
            assertThat(service.annul(id, "r", "a").getStatus()).isEqualTo(InfractionStatus.ANNULLED);
        }
    }
}
