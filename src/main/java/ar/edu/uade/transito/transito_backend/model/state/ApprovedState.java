package ar.edu.uade.transito.transito_backend.model.state;

import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;

/** Corte aprobado: puede activarse o vencer sin haberse activado. */
public final class ApprovedState implements StreetClosureState {

    static final ApprovedState INSTANCE = new ApprovedState();

    private ApprovedState() {
    }

    @Override
    public StreetClosureStatus status() {
        return StreetClosureStatus.APPROVED;
    }

    @Override
    public StreetClosureState activate(StreetClosure closure) {
        return ActiveState.INSTANCE;
    }

    @Override
    public StreetClosureState expire(StreetClosure closure) {
        return ExpiredState.INSTANCE;
    }
}
