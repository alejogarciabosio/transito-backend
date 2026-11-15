package ar.edu.uade.transito.transito_backend.model.state;

import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;

/** Estado terminal (finalizado): no admite transiciones; toda operación responde 409. */
public final class FinishedState implements StreetClosureState {

    static final FinishedState INSTANCE = new FinishedState();

    private FinishedState() {
    }

    @Override
    public StreetClosureStatus status() {
        return StreetClosureStatus.FINISHED;
    }

    @Override
    public boolean isFinal() {
        return true;
    }
}
