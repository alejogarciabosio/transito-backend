package ar.edu.uade.transito.transito_backend.model.state;

import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;

/** Corte en curso: finaliza manualmente o al vencer el horario autorizado. */
public final class ActiveState implements StreetClosureState {

    static final ActiveState INSTANCE = new ActiveState();

    private ActiveState() {
    }

    @Override
    public StreetClosureStatus status() {
        return StreetClosureStatus.ACTIVE;
    }

    @Override
    public StreetClosureState finish(StreetClosure closure) {
        return FinishedState.INSTANCE;
    }

    @Override
    public StreetClosureState expire(StreetClosure closure) {
        return FinishedState.INSTANCE;
    }
}
