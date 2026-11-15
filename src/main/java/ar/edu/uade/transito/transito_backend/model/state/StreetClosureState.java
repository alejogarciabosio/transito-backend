package ar.edu.uade.transito.transito_backend.model.state;

import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;

/**
 * Patrón State (GoF) del ciclo de vida de {@link StreetClosure}.
 *
 * <p>Cada estado es una clase que sabe qué transiciones admite. Por defecto toda operación es
 * inválida y lanza {@link InvalidStateTransitionException} (HTTP 409, Problem Details); cada
 * estado concreto sobrescribe solo las transiciones que le corresponden y devuelve el estado
 * siguiente. El estado persistido sigue siendo {@link StreetClosureStatus} (sin cambio de esquema);
 * {@link StreetClosureStates#of(StreetClosureStatus)} resuelve el objeto estado desde el enum.
 */
public interface StreetClosureState {

    /** Valor persistido en la columna {@code status} que representa este estado. */
    StreetClosureStatus status();

    /** REQUESTED → APPROVED. */
    default StreetClosureState approve(StreetClosure closure) {
        throw invalid(closure, StreetClosureStatus.REQUESTED.name());
    }

    /** REQUESTED → REJECTED. */
    default StreetClosureState reject(StreetClosure closure) {
        throw invalid(closure, StreetClosureStatus.REQUESTED.name());
    }

    /** APPROVED → ACTIVE. */
    default StreetClosureState activate(StreetClosure closure) {
        throw invalid(closure, StreetClosureStatus.APPROVED.name());
    }

    /** ACTIVE → FINISHED (cierre manual). */
    default StreetClosureState finish(StreetClosure closure) {
        throw invalid(closure, StreetClosureStatus.ACTIVE.name());
    }

    /** Vencimiento automático del horario: ACTIVE → FINISHED, APPROVED → EXPIRED. */
    default StreetClosureState expire(StreetClosure closure) {
        throw invalid(closure, StreetClosureStatus.ACTIVE + " o " + StreetClosureStatus.APPROVED);
    }

    /** Indica si el estado es terminal (no admite ninguna transición). */
    default boolean isFinal() {
        return false;
    }

    private InvalidStateTransitionException invalid(StreetClosure closure, String expected) {
        return new InvalidStateTransitionException(
                "El corte de calle " + closure.getStreetClosureId() + " debe estar en estado " + expected
                        + " (actual: " + status() + ")");
    }
}
