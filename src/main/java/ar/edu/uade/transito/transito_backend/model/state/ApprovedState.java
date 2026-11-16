package ar.edu.uade.transito.transito_backend.model.state;

import java.time.OffsetDateTime;

import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;

/** Corte aprobado: puede activarse (solo dentro de su ventana autorizada, DEF-42) o vencer sin haberse activado. */
public final class ApprovedState implements StreetClosureState {

    static final ApprovedState INSTANCE = new ApprovedState();

    private ApprovedState() {
    }

    @Override
    public StreetClosureStatus status() {
        return StreetClosureStatus.APPROVED;
    }

    @Override
    public StreetClosureState activate(StreetClosure closure, OffsetDateTime now) {
        OffsetDateTime start = closure.getStartDate();
        OffsetDateTime end = closure.getEndDate();
        // Ventana semiabierta [startDate, endDate). Un extremo nulo no acota (los cortes reales siempre traen ambos).
        boolean beforeStart = start != null && now.isBefore(start);
        boolean atOrAfterEnd = end != null && !now.isBefore(end);
        if (beforeStart || atOrAfterEnd) {
            throw new InvalidStateTransitionException(
                    "El corte solo puede activarse dentro de la ventana autorizada (desde " + start + " hasta " + end
                            + "; ahora: " + now + ")");
        }
        return ActiveState.INSTANCE;
    }

    @Override
    public StreetClosureState expire(StreetClosure closure) {
        return ExpiredState.INSTANCE;
    }
}
