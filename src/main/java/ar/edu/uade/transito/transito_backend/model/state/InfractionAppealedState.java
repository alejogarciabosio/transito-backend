package ar.edu.uade.transito.transito_backend.model.state;

import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;

/** Infracción apelada: puede confirmarse, anularse o editarse (no apelarse de nuevo). */
public final class InfractionAppealedState implements InfractionState {

    static final InfractionAppealedState INSTANCE = new InfractionAppealedState();

    private InfractionAppealedState() {
    }

    @Override
    public InfractionStatus status() {
        return InfractionStatus.APPEALED;
    }

    @Override
    public InfractionState confirm(Infraction infraction) {
        return InfractionConfirmedState.INSTANCE;
    }

    @Override
    public InfractionState annul(Infraction infraction) {
        return InfractionAnnulledState.INSTANCE;
    }

    @Override
    public void requireEditable(Infraction infraction) {
        // editable mientras no esté firme
    }

    @Override
    public boolean isFinal() {
        return false;
    }
}
