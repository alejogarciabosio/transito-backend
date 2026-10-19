package ar.edu.uade.transito.transito_backend.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

/** Receives Core-routed events from Tránsito's queue. */
@Component
@ConditionalOnProperty(name = "core.rabbitmq.enabled", havingValue = "true")
public class RabbitMqIncomingEventListener {

    private final IncomingEventDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    public RabbitMqIncomingEventListener(IncomingEventDispatcher dispatcher, ObjectMapper objectMapper) {
        this.dispatcher = dispatcher;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${core.rabbitmq.queue}")
    public void receive(Message rawMessage) throws Exception {
        // Any exception reaches the container, which nacks with requeue=false as configured.
        JsonNode message = objectMapper.readTree(rawMessage.getBody());
        dispatcher.dispatch(message);
    }
}
