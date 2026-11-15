package ar.edu.uade.transito.transito_backend.model.state;

import static ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;

/** Matriz completa (estado × operación) del patrón State de Infraction. */
class InfractionStateTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    enum Operation {
        APPEAL(InfractionState::appeal),
        CONFIRM(InfractionState::confirm),
        ANNUL(InfractionState::annul);

        final BiFunction<InfractionState, Infraction, InfractionState> call;

        Operation(BiFunction<InfractionState, Infraction, InfractionState> call) {
            this.call = call;
        }

        String expectedMessage(InfractionStatus from) {
            return switch (this) {
                case APPEAL -> "Solo se puede apelar una infracción en estado PENDING_APPEAL";
                case CONFIRM -> "No se puede confirmar una infracción en estado " + from;
                case ANNUL -> "La infracción " + ID + " ya está firme (" + from + ")";
            };
        }
    }

    private static final Map<InfractionStatus, Map<Operation, InfractionStatus>> VALID = Map.of(
            PENDING_APPEAL, Map.of(Operation.APPEAL, APPEALED, Operation.CONFIRM, CONFIRMED, Operation.ANNUL, ANNULLED),
            APPEALED, Map.of(Operation.CONFIRM, CONFIRMED, Operation.ANNUL, ANNULLED),
            CONFIRMED, Map.of(),
            ANNULLED, Map.of());

    static Stream<Arguments> validTransitions() {
        List<Arguments> args = new ArrayList<>();
        VALID.forEach((from, ops) -> ops.forEach((op, to) -> args.add(Arguments.of(from, op, to))));
        return args.stream();
    }

    static Stream<Arguments> invalidTransitions() {
        List<Arguments> args = new ArrayList<>();
        for (InfractionStatus from : InfractionStatus.values()) {
            for (Operation op : Operation.values()) {
                if (!VALID.get(from).containsKey(op)) {
                    args.add(Arguments.of(from, op));
                }
            }
        }
        return args.stream();
    }

    private static Infraction infraction(InfractionStatus status) {
        Infraction infraction = new Infraction();
        infraction.setInfractionId(ID);
        infraction.setStatus(status);
        return infraction;
    }

    @ParameterizedTest(name = "{0} --{1}--> {2}")
    @MethodSource("validTransitions")
    void validTransitionReturnsNextState(InfractionStatus from, Operation op, InfractionStatus to) {
        assertThat(op.call.apply(InfractionStates.of(from), infraction(from))).isSameAs(InfractionStates.of(to));
    }

    @ParameterizedTest(name = "{0} --{1}--> 409")
    @MethodSource("invalidTransitions")
    void invalidTransitionThrows409Exception(InfractionStatus from, Operation op) {
        assertThatThrownBy(() -> op.call.apply(InfractionStates.of(from), infraction(from)))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage(op.expectedMessage(from));
    }

    @Test
    void matrixIsComplete() {
        assertThat(validTransitions().count() + invalidTransitions().count())
                .isEqualTo((long) InfractionStatus.values().length * Operation.values().length);
    }

    @ParameterizedTest
    @EnumSource(InfractionStatus.class)
    void editableOnlyWhileNotFinalAndResolverCoversEveryStatus(InfractionStatus status) {
        InfractionState state = InfractionStates.of(status);
        assertThat(state.status()).isEqualTo(status);
        assertThat(InfractionStates.of(infraction(status))).isSameAs(state);
        boolean isFinal = EnumSet.of(CONFIRMED, ANNULLED).contains(status);
        assertThat(state.isFinal()).isEqualTo(isFinal);
        if (isFinal) {
            assertThatThrownBy(() -> state.requireEditable(infraction(status)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("La infracción " + ID + " ya está firme (" + status + ")");
        } else {
            assertThatCode(() -> state.requireEditable(infraction(status))).doesNotThrowAnyException();
        }
    }

    @Test
    void resolverRejectsNullStatus() {
        assertThatNullPointerException().isThrownBy(() -> InfractionStates.of((InfractionStatus) null));
    }
}
