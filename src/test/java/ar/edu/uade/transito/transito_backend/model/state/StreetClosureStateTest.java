package ar.edu.uade.transito.transito_backend.model.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;

import static ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus.*;

/** DEF-13: matriz completa (estado × operación) del patrón State de StreetClosure. */
class StreetClosureStateTest {

    enum Operation {
        APPROVE(StreetClosureState::approve, "REQUESTED"),
        REJECT(StreetClosureState::reject, "REQUESTED"),
        ACTIVATE(StreetClosureState::activate, "APPROVED"),
        FINISH(StreetClosureState::finish, "ACTIVE"),
        EXPIRE(StreetClosureState::expire, "ACTIVE o APPROVED");

        final BiFunction<StreetClosureState, StreetClosure, StreetClosureState> call;
        final String expectedInMessage;

        Operation(BiFunction<StreetClosureState, StreetClosure, StreetClosureState> call, String expected) {
            this.call = call;
            this.expectedInMessage = expected;
        }
    }

    /** Única fuente de verdad de las transiciones válidas. */
    private static final Map<StreetClosureStatus, Map<Operation, StreetClosureStatus>> VALID = Map.of(
            REQUESTED, Map.of(Operation.APPROVE, APPROVED, Operation.REJECT, REJECTED),
            APPROVED, Map.of(Operation.ACTIVATE, ACTIVE, Operation.EXPIRE, EXPIRED),
            ACTIVE, Map.of(Operation.FINISH, FINISHED, Operation.EXPIRE, FINISHED),
            REJECTED, Map.of(),
            FINISHED, Map.of(),
            EXPIRED, Map.of());

    static Stream<Arguments> validTransitions() {
        List<Arguments> args = new ArrayList<>();
        VALID.forEach((from, ops) -> ops.forEach((op, to) -> args.add(Arguments.of(from, op, to))));
        return args.stream();
    }

    static Stream<Arguments> invalidTransitions() {
        List<Arguments> args = new ArrayList<>();
        for (StreetClosureStatus from : StreetClosureStatus.values()) {
            for (Operation op : Operation.values()) {
                if (!VALID.get(from).containsKey(op)) {
                    args.add(Arguments.of(from, op));
                }
            }
        }
        return args.stream();
    }

    private static StreetClosure closure(StreetClosureStatus status) {
        StreetClosure closure = new StreetClosure();
        closure.setStreetClosureId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
        closure.setStatus(status);
        return closure;
    }

    @ParameterizedTest(name = "{0} --{1}--> {2}")
    @MethodSource("validTransitions")
    void validTransitionReturnsNextState(StreetClosureStatus from, Operation op, StreetClosureStatus to) {
        StreetClosure closure = closure(from);
        StreetClosureState next = op.call.apply(StreetClosureStates.of(from), closure);
        assertThat(next.status()).isEqualTo(to);
        assertThat(next).isSameAs(StreetClosureStates.of(to));
        assertThat(closure.getStatus()).as("el estado no muta la entidad; lo hace el servicio").isEqualTo(from);
    }

    @ParameterizedTest(name = "{0} --{1}--> 409")
    @MethodSource("invalidTransitions")
    void invalidTransitionThrows409Exception(StreetClosureStatus from, Operation op) {
        StreetClosure closure = closure(from);
        assertThatThrownBy(() -> op.call.apply(StreetClosureStates.of(from), closure))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("El corte de calle " + closure.getStreetClosureId() + " debe estar en estado "
                        + op.expectedInMessage + " (actual: " + from + ")");
    }

    @Test
    @DisplayName("La matriz cubre todos los pares estado × operación (6 × 5 = 30)")
    void matrixIsComplete() {
        assertThat(validTransitions().count() + invalidTransitions().count())
                .isEqualTo((long) StreetClosureStatus.values().length * Operation.values().length);
        assertThat(validTransitions().count()).isEqualTo(6);
    }

    @ParameterizedTest
    @EnumSource(StreetClosureStatus.class)
    void resolverMapsEveryPersistedStatusToItsStateClass(StreetClosureStatus status) {
        StreetClosureState state = StreetClosureStates.of(status);
        assertThat(state.status()).isEqualTo(status);
        assertThat(StreetClosureStates.of(closure(status))).isSameAs(state);
        assertThat(state.isFinal()).isEqualTo(EnumSet.of(REJECTED, FINISHED, EXPIRED).contains(status));
        assertThat(state.getClass().getSimpleName()).isNotEqualTo("StreetClosureState");
    }

    @Test
    void resolverRejectsNullStatus() {
        assertThatNullPointerException().isThrownBy(() -> StreetClosureStates.of((StreetClosureStatus) null));
    }
}
