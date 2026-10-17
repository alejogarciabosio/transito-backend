package ar.edu.uade.transito.transito_backend.listener;

import java.nio.charset.StandardCharsets;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/** Consume q.transito; ante fallas delega los reintentos y la DLQ al Core. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "core.integration", name = "enabled", havingValue = "true")
public class CoreRabbitListener {

    private final IncomingEventDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${core.rabbitmq.incoming-queue:q.transito}", containerFactory = "coreRabbitListenerFactory")
    public void receive(Message message, Channel channel) throws Exception {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            dispatcher.dispatch(objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8)));
            channel.basicAck(tag, false);
        } catch (Exception exception) {
            channel.basicNack(tag, false, false);
        }
    }
}
