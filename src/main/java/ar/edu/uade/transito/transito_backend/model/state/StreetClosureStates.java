package ar.edu.uade.transito.transito_backend.model.state;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;

/** Resolvedor: traduce el enum persistido al objeto estado correspondiente. */
public final class StreetClosureStates {

    private static final Map<StreetClosureStatus, StreetClosureState> BY_STATUS = new EnumMap<>(StreetClosureStatus.class);

    static {
        for (StreetClosureState state : new StreetClosureState[] {
                RequestedState.INSTANCE, ApprovedState.INSTANCE, RejectedState.INSTANCE,
                ActiveState.INSTANCE, FinishedState.INSTANCE, ExpiredState.INSTANCE }) {
            BY_STATUS.put(state.status(), state);
        }
    }

    private StreetClosureStates() {
    }

    public static StreetClosureState of(StreetClosureStatus status) {
        return BY_STATUS.get(Objects.requireNonNull(status, "status"));
    }

    public static StreetClosureState of(StreetClosure closure) {
        return of(closure.getStatus());
    }
}
