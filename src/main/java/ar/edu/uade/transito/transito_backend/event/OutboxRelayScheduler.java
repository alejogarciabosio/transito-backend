package ar.edu.uade.transito.transito_backend.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Corre el relay del outbox cada {@code outbox.relay.interval-ms} (default 2 s). */
@Component
@ConditionalOnProperty(name = "outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private final OutboxRelay relay;

    public OutboxRelayScheduler(OutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:2000}",
            initialDelayString = "${outbox.relay.initial-delay-ms:5000}")
    public void run() {
        try {
            relay.relayPending();
        } catch (RuntimeException ex) {
            // Por ejemplo, la base no responde: se reintenta en la próxima vuelta.
            log.warn("[outbox] el relay no pudo leer pendientes: {}", OutboxRelay.describe(ex));
        }
    }
}
