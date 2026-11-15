package ar.edu.uade.transito.transito_backend.model.state;

import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;

/** Corte solicitado por Obras/Ambiente: puede aprobarse o rechazarse. */
public final class RequestedState implements StreetClosureState {

    static final RequestedState INSTANCE = new RequestedState();

    private RequestedState() {
    }

    @Override
    public StreetClosureStatus status() {
        return StreetClosureStatus.REQUESTED;
    }

    @Override
    public StreetClosureState approve(StreetClosure closure) {
        return ApprovedState.INSTANCE;
    }

    @Override
    public StreetClosureState reject(StreetClosure closure) {
        return RejectedState.INSTANCE;
    }
}
