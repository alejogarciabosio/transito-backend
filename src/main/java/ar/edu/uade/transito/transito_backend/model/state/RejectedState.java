package ar.edu.uade.transito.transito_backend.model.state;

import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;

/** Estado terminal (rechazado): no admite transiciones; toda operación responde 409. */
public final class RejectedState implements StreetClosureState {

    static final RejectedState INSTANCE = new RejectedState();

    private RejectedState() {
    }

    @Override
    public StreetClosureStatus status() {
        return StreetClosureStatus.REJECTED;
    }

    @Override
    public boolean isFinal() {
        return true;
    }
}
