package ar.edu.uade.transito.transito_backend.event;

import ar.edu.uade.transito.transito_backend.model.OutboxEvent;
import ar.edu.uade.transito.transito_backend.model.enums.OutboxStatus;
import ar.edu.uade.transito.transito_backend.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.AmqpTimeoutException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Relay del outbox (DEF-14): toma los eventos PENDING en orden de creación, los publica con el
 * {@link EventTransport} real (RabbitMQ o log) con el MISMO sobre y los marca PUBLISHED.
 * <ul>
 *   <li>Ante error: suma un intento, guarda el error (sin payload) y reprograma con backoff exponencial;
 *       tras {@code maxAttempts} queda FAILED. Un evento que falla no frena a los demás.</li>
 *   <li>Broker inalcanzable: corta la vuelta (el resto espera sin gastar intentos) y el evento sigue
 *       PENDING sin importar cuántos intentos lleve: se publica apenas vuelve el broker.</li>
 *   <li>Idempotencia entre relays concurrentes: cada evento se toma con un update condicional (lease);
 *       solo un relay lo obtiene, así no se publica dos veces.</li>
 *   <li>Entrega "al menos una vez": si el proceso muere entre el envío y el marcado, se reenvía al vencer
 *       el lease; Core y los módulos deduplican por eventId.</li>
 * </ul>
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final OutboxEventRepository repository;
    private final EventTransport transport;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final int batchSize;
    private final int maxAttempts;
    private final Duration backoffBase;
    private final Duration backoffMax;
    private final Duration lease;

    @Autowired
    public OutboxRelay(OutboxEventRepository repository,
                       // Hay un único EventTransport activo: Rabbit o log, según core.rabbitmq.enabled (mutuamente excluyentes).
                       ObjectProvider<EventTransport> transportProvider,
                       PlatformTransactionManager transactionManager,
                       @Value("${outbox.relay.batch-size:50}") int batchSize,
                       @Value("${outbox.relay.max-attempts:15}") int maxAttempts,
                       @Value("${outbox.relay.backoff-base-ms:2000}") long backoffBaseMs,
                       @Value("${outbox.relay.backoff-max-ms:60000}") long backoffMaxMs,
                       @Value("${outbox.relay.lease-ms:30000}") long leaseMs) {
        this(repository, transportProvider.getObject(), transactionManager, Clock.systemUTC(), batchSize, maxAttempts,
                Duration.ofMillis(backoffBaseMs), Duration.ofMillis(backoffMaxMs), Duration.ofMillis(leaseMs));
    }

    OutboxRelay(OutboxEventRepository repository, EventTransport transport,
                PlatformTransactionManager transactionManager, Clock clock, int batchSize, int maxAttempts,
                Duration backoffBase, Duration backoffMax, Duration lease) {
        this.repository = repository;
        this.transport = transport;
        this.tx = new TransactionTemplate(transactionManager);
        this.clock = clock;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.backoffBase = backoffBase;
        this.backoffMax = backoffMax;
        this.lease = lease;
    }

    /** Publica lo pendiente que ya venció su espera. Devuelve cuántos eventos quedaron PUBLISHED. */
    public int relayPending() {
        Instant now = clock.instant();
        List<UUID> due = repository.findDueIds(OutboxStatus.PENDING, now, PageRequest.of(0, batchSize));
        int published = 0;
        for (UUID id : due) {
            Outcome outcome = relayOne(id);
            if (outcome == Outcome.PUBLISHED) {
                published++;
            } else if (outcome == Outcome.BROKER_UNREACHABLE) {
                // Sin conexión al broker no tiene sentido seguir: los demás esperan a la próxima vuelta
                // (sin sumar intentos) y conservan su orden.
                break;
            }
        }
        return published;
    }

    private enum Outcome { PUBLISHED, SKIPPED, FAILED, BROKER_UNREACHABLE }

    private Outcome relayOne(UUID id) {
        Instant now = clock.instant();
        if (repository.claim(id, OutboxStatus.PENDING, now, now.plus(lease)) == 0) {
            return Outcome.SKIPPED; // lo tomó otro relay, o ya cambió de estado
        }
        OutboxEvent event = repository.findById(id).orElse(null);
        if (event == null) {
            return Outcome.SKIPPED;
        }
        try {
            transport.send(event);
        } catch (RuntimeException ex) {
            boolean brokerUnreachable = isBrokerUnreachable(ex);
            registerFailure(id, ex, brokerUnreachable);
            return brokerUnreachable ? Outcome.BROKER_UNREACHABLE : Outcome.FAILED;
        }
        tx.executeWithoutResult(status -> repository.findById(id).ifPresent(e -> {
            e.setStatus(OutboxStatus.PUBLISHED);
            e.setAttempts(e.getAttempts() + 1);
            e.setPublishedAt(clock.instant());
            e.setLastError(null);
        }));
        return Outcome.PUBLISHED;
    }

    /** Fallas de conexión con el broker (caído, DNS, timeout): no son culpa del evento. */
    static boolean isBrokerUnreachable(Throwable ex) {
        return ex instanceof AmqpConnectException || ex instanceof AmqpIOException || ex instanceof AmqpTimeoutException;
    }

    private void registerFailure(UUID id, RuntimeException ex, boolean brokerUnreachable) {
        String error = describe(ex);
        tx.executeWithoutResult(status -> repository.findById(id).ifPresent(e -> {
            int attempts = e.getAttempts() + 1;
            e.setAttempts(attempts);
            e.setLastError(error);
            // Con el broker caído el evento nunca pasa a FAILED: sigue PENDING (backoff con tope) hasta que
            // vuelva, así no se pierde aunque la caída dure horas. FAILED es solo para errores del propio evento.
            if (attempts >= maxAttempts && !brokerUnreachable) {
                e.setStatus(OutboxStatus.FAILED);
                log.error("[outbox] evento FAILED tras {} intentos type={} eventId={} error={}",
                        attempts, e.getEventType(), e.getEventId(), error);
            } else {
                e.setNextAttemptAt(clock.instant().plus(backoff(attempts)));
                log.warn("[outbox] no se pudo publicar (intento {}/{}) type={} eventId={} error={}",
                        attempts, maxAttempts, e.getEventType(), e.getEventId(), error);
            }
        }));
    }

    /** Backoff exponencial: base, 2·base, 4·base... con tope en backoffMax. */
    Duration backoff(int attempts) {
        int exponent = Math.min(attempts - 1, 20);
        Duration candidate = backoffBase.multipliedBy(1L << exponent);
        return candidate.compareTo(backoffMax) > 0 ? backoffMax : candidate;
    }

    /** Solo tipo de excepción y mensaje técnico de la causa raíz (sin payload, recortado). */
    static String describe(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String text = root == ex
                ? ex.getClass().getSimpleName() + ": " + ex.getMessage()
                : ex.getClass().getSimpleName() + " <- " + root.getClass().getSimpleName() + ": " + root.getMessage();
        return text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) : text;
    }
}
