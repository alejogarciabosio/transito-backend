package ar.edu.uade.transito.transito_backend.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
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

    private static final Logger log = LoggerFactory.getLogger(RabbitMqIncomingEventListener.class);

    private final IncomingEventDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    public RabbitMqIncomingEventListener(IncomingEventDispatcher dispatcher, ObjectMapper objectMapper) {
        this.dispatcher = dispatcher;
        this.objectMapper = objectMapper;
    }

    /**
     * Cualquier fallo se rechaza sin reencolar (igual que antes, default-requeue-rejected=false).
     * DEF-05: la excepción original no se propaga al contenedor porque su mensaje (p. ej. de Jackson)
     * puede incluir valores del payload; se loguean solo metadatos y la clase del error.
     */
    @RabbitListener(queues = "${core.rabbitmq.queue}")
    public void receive(Message rawMessage) {
        JsonNode message;
        try {
            message = objectMapper.readTree(rawMessage.getBody());
        } catch (RuntimeException ex) {
            log.warn("[event-rejected] mensaje no es JSON válido error={}", ex.getClass().getSimpleName());
            throw new AmqpRejectAndDontRequeueException("Mensaje de Core inválido (" + ex.getClass().getSimpleName() + ")");
        }
        try {
            dispatcher.dispatch(message);
        } catch (RuntimeException ex) {
            String eventType = message.path("eventType").asText();
            String eventId = message.path("eventId").asText();
            log.warn("[event-rejected] type={} eventId={} correlationId={} error={}", eventType, eventId,
                    message.path("correlationId").asText(), ex.getClass().getSimpleName());
            throw new AmqpRejectAndDontRequeueException(
                    "Evento " + eventType + " " + eventId + " rechazado (" + ex.getClass().getSimpleName() + ")");
        }
    }
}
