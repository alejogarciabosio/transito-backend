package ar.edu.uade.transito.transito_backend.ai;

import java.time.Clock;

/** Circuit breaker mínimo: abre tras N fallas consecutivas y permite un intento de prueba pasado un tiempo. */
public class SimpleCircuitBreaker {

    private final int failureThreshold;
    private final long openMillis;
    private final Clock clock;
    private int consecutiveFailures;
    private long openedAt = -1;
    private boolean trialInFlight;

    public SimpleCircuitBreaker(int failureThreshold, long openMillis, Clock clock) {
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openMillis = openMillis;
        this.clock = clock;
    }

    /** @return true si se puede llamar al proveedor. */
    public synchronized boolean allowRequest() {
        if (openedAt < 0) {
            return true;
        }
        if (clock.millis() - openedAt >= openMillis && !trialInFlight) {
            trialInFlight = true; // semiabierto: un solo intento
            return true;
        }
        return false;
    }

    public synchronized void recordSuccess() {
        consecutiveFailures = 0;
        openedAt = -1;
        trialInFlight = false;
    }

    public synchronized void recordFailure() {
        consecutiveFailures++;
        if (trialInFlight || consecutiveFailures >= failureThreshold) {
            openedAt = clock.millis();
        }
        trialInFlight = false;
    }

    public synchronized boolean isOpen() {
        return openedAt >= 0;
    }

    public synchronized void reset() {
        recordSuccess();
    }
}
