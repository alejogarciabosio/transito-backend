package ar.edu.uade.transito.transito_backend.model.state;

import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;

/**
 * Patrón State del ciclo de vida de {@link Infraction}. Las operaciones inválidas lanzan
 * {@link InvalidStateTransitionException} (HTTP 409). El enum persistido no cambia;
 * {@link InfractionStates#of(InfractionStatus)} resuelve el objeto estado.
 */
public interface InfractionState {

    InfractionStatus status();

    /** PENDING_APPEAL → APPEALED. */
    default InfractionState appeal(Infraction infraction) {
        throw new InvalidStateTransitionException(
                "Solo se puede apelar una infracción en estado " + InfractionStatus.PENDING_APPEAL);
    }

    /** PENDING_APPEAL | APPEALED → CONFIRMED. */
    default InfractionState confirm(Infraction infraction) {
        throw new InvalidStateTransitionException("No se puede confirmar una infracción en estado " + status());
    }

    /** PENDING_APPEAL | APPEALED → ANNULLED. */
    default InfractionState annul(Infraction infraction) {
        throw alreadyFinal(infraction);
    }

    /** Edición de datos: solo mientras la infracción no esté firme. */
    default void requireEditable(Infraction infraction) {
        throw alreadyFinal(infraction);
    }

    default boolean isFinal() {
        return true;
    }

    private InvalidStateTransitionException alreadyFinal(Infraction infraction) {
        return new InvalidStateTransitionException(
                "La infracción " + infraction.getInfractionId() + " ya está firme (" + status() + ")");
    }
}
