package ar.edu.uade.transito.transito_backend.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Publishes the Core envelope to its shared fanout exchange. */
@Component
@ConditionalOnProperty(name = "core.rabbitmq.enabled", havingValue = "true")
public class RabbitMqEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public RabbitMqEventPublisher(RabbitTemplate rabbitTemplate,
                                  @Value("${core.rabbitmq.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public <T> void publish(EventEnvelope<T> event) {
        // A fanout exchange ignores the routing key; Core routes by registered subscriptions.
        rabbitTemplate.convertAndSend(exchange, "", event);
    }
}
