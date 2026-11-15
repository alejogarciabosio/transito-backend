package ar.edu.uade.transito.transito_backend.event;

import ar.edu.uade.transito.transito_backend.model.OutboxEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Publishes the Core envelope to its shared fanout exchange (lo invoca el relay del outbox). */
@Component
@ConditionalOnProperty(name = "core.rabbitmq.enabled", havingValue = "true")
public class RabbitMqEventPublisher implements EventTransport {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String moduleUserId;

    public RabbitMqEventPublisher(RabbitTemplate rabbitTemplate,
                                  @Value("${core.rabbitmq.exchange}") String exchange,
                                  @Value("${spring.rabbitmq.username}") String moduleUserId) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.moduleUserId = moduleUserId;
    }

    @Override
    public void send(OutboxEvent event) {
        sendJson(event.getEventType(), event.getEventId(), event.getCorrelationId(), event.getPayload());
    }

    /** Envío directo de un sobre (sin outbox); mismo mensaje que produce el relay. */
    public <T> void publish(EventEnvelope<T> event) {
        sendJson(event.getEventType(), event.getEventId(), event.getCorrelationId(), CoreMessageFormat.toJson(event));
    }

    private void sendJson(String eventType, UUID eventId, UUID correlationId, String json) {
        if (!CorePublishedEventTypes.contains(eventType)) {
            log.debug("El evento {} es interno de Tránsito; no se publica en Core", eventType);
            return;
        }
        // A fanout exchange ignores the routing key; Core routes by registered subscriptions.
        rabbitTemplate.send(exchange, "", CoreMessageFormat.toMessage(json, moduleUserId));
        // Solo metadatos del sobre: nunca el payload (puede tener datos personales).
        log.info("[event-published] type={} eventId={} correlationId={}", eventType, eventId, correlationId);
    }

}
