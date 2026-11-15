package ar.edu.uade.transito.transito_backend.model.state;

import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;

/** Infracción apelada: puede confirmarse, anularse o editarse (no apelarse de nuevo). */
public final class InfractionAppealedState extends OpenInfractionState {

    static final InfractionAppealedState INSTANCE = new InfractionAppealedState();

    private InfractionAppealedState() {
    }

    @Override
    public InfractionStatus status() {
        return InfractionStatus.APPEALED;
    }
}
