package ar.edu.uade.transito.transito_backend.model.state;

import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;

/** Estado firme (confirmada): no admite transiciones ni edición. */
public final class InfractionConfirmedState implements InfractionState {

    static final InfractionConfirmedState INSTANCE = new InfractionConfirmedState();

    private InfractionConfirmedState() {
    }

    @Override
    public InfractionStatus status() {
        return InfractionStatus.CONFIRMED;
    }
}
