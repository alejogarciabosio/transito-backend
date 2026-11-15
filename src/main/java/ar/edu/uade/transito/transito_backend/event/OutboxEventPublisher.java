package ar.edu.uade.transito.transito_backend.event;

import ar.edu.uade.transito.transito_backend.model.OutboxEvent;
import ar.edu.uade.transito.transito_backend.model.enums.OutboxStatus;
import ar.edu.uade.transito.transito_backend.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Implementación del puerto {@link EventPublisher} que usan los servicios (DEF-14): en vez de enviar
 * al broker dentro de la transacción, guarda el sobre en {@code outbox_events} en la MISMA transacción.
 * Si la transacción hace rollback, el evento desaparece con ella; si hace commit, el {@link OutboxRelay}
 * lo publica aunque el broker esté caído en ese momento.
 */
@Component
public class OutboxEventPublisher implements EventPublisher {

    private final OutboxEventRepository repository;
    private final Clock clock;

    @Autowired
    public OutboxEventPublisher(OutboxEventRepository repository) {
        this(repository, Clock.systemUTC());
    }

    OutboxEventPublisher(OutboxEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public <T> void publish(EventEnvelope<T> event) {
        Instant now = clock.instant();
        repository.save(OutboxEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .correlationId(event.getCorrelationId())
                .causationId(event.getCausationId())
                .payload(CoreMessageFormat.toJson(event))
                .status(OutboxStatus.PENDING)
                .attempts(0)
                .createdAt(now)
                .nextAttemptAt(now)
                .build());
    }
}
