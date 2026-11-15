package ar.edu.uade.transito.transito_backend.model.state;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;

/** Resolvedor: traduce el enum persistido de la infracción a su objeto estado. */
public final class InfractionStates {

    private static final Map<InfractionStatus, InfractionState> BY_STATUS = new EnumMap<>(InfractionStatus.class);

    static {
        for (InfractionState state : new InfractionState[] {
                InfractionPendingAppealState.INSTANCE, InfractionAppealedState.INSTANCE,
                InfractionConfirmedState.INSTANCE, InfractionAnnulledState.INSTANCE }) {
            BY_STATUS.put(state.status(), state);
        }
    }

    private InfractionStates() {
    }

    public static InfractionState of(InfractionStatus status) {
        return BY_STATUS.get(Objects.requireNonNull(status, "status"));
    }

    public static InfractionState of(Infraction infraction) {
        return of(infraction.getStatus());
    }
}
