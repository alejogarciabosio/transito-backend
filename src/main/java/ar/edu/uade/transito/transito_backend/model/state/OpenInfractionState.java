package ar.edu.uade.transito.transito_backend.model.state;

import ar.edu.uade.transito.transito_backend.model.Infraction;

/** Base de los estados no firmes (PENDING_APPEAL, APPEALED): admiten confirmar, anular y editar. */
abstract class OpenInfractionState implements InfractionState {

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
