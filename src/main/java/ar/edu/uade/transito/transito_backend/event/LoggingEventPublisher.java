package ar.edu.uade.transito.transito_backend.event;

import ar.edu.uade.transito.transito_backend.model.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "core.rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
//Esta clase corresponde al patrón de diseño Strategy: es una de las formas concretas de enviar eventos
//(EventTransport). Sin broker (perfil local) el relay del outbox "publica" dejando solo el log.
public class LoggingEventPublisher implements EventTransport {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void send(OutboxEvent event) {
        logPublished(event.getEventType(), event.getEventId(), event.getCorrelationId());
    }

    public <T> void publish(EventEnvelope<T> event) {
        logPublished(event.getEventType(), event.getEventId(), event.getCorrelationId());
    }

    private static void logPublished(String eventType, UUID eventId, UUID correlationId) {
        // DEF-05: solo metadatos del sobre, igual que RabbitMqEventPublisher; nunca el payload
        // (data puede traer DNI, patente, inspector, etc.).
        log.info("[event-published] type={} eventId={} correlationId={}", eventType, eventId, correlationId);
    }

}
