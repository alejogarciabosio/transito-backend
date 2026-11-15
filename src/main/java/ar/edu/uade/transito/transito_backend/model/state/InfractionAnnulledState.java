package ar.edu.uade.transito.transito_backend.model.state;

import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;

/** Estado firme (anulada): no admite transiciones ni edición. */
public final class InfractionAnnulledState implements InfractionState {

    static final InfractionAnnulledState INSTANCE = new InfractionAnnulledState();

    private InfractionAnnulledState() {
    }

    @Override
    public InfractionStatus status() {
        return InfractionStatus.ANNULLED;
    }
}
