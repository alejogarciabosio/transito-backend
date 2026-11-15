package ar.edu.uade.transito.transito_backend.model.state;

import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;

/** Infracción labrada, en plazo de apelación: puede apelarse, confirmarse, anularse o editarse. */
public final class InfractionPendingAppealState extends OpenInfractionState {

    static final InfractionPendingAppealState INSTANCE = new InfractionPendingAppealState();

    private InfractionPendingAppealState() {
    }

    @Override
    public InfractionStatus status() {
        return InfractionStatus.PENDING_APPEAL;
    }

    @Override
    public InfractionState appeal(Infraction infraction) {
        return InfractionAppealedState.INSTANCE;
    }
}
