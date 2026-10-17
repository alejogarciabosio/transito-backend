package ar.edu.uade.transito.transito_backend.event;

import java.nio.charset.StandardCharsets;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/** Publica el envelope oficial de M7 en el exchange fanout administrado por Core. */
@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "core.integration", name = "enabled", havingValue = "true")
public class CoreRabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${core.rabbitmq.exchange:muni.inbox}")
    private String exchange;

    @Override
    public <T> void publish(EventEnvelope<T> event) {
        try {
            MessageProperties properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setMessageId(event.getEventId().toString());
            rabbitTemplate.send(exchange, "", new Message(
                    objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8), properties));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo publicar el evento " + event.getEventType() + " en Core", exception);
        }
    }
}
