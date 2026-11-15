package ar.edu.uade.transito.transito_backend.model.state;

import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;

/** Estado terminal (vencido sin activarse): no admite transiciones; toda operación responde 409. */
public final class ExpiredState implements StreetClosureState {

    static final ExpiredState INSTANCE = new ExpiredState();

    private ExpiredState() {
    }

    @Override
    public StreetClosureStatus status() {
        return StreetClosureStatus.EXPIRED;
    }

    @Override
    public boolean isFinal() {
        return true;
    }
}
