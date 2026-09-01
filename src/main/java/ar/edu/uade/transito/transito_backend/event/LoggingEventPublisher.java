package ar.edu.uade.transito.transito_backend.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public <T> void publish(EventEnvelope<T> event) {
        log.info("[event] type={} eventId={} occurredAt={} data={}",
                event.getEventType(), event.getEventId(), event.getOccurredAt(), event.getData());
    }
}
