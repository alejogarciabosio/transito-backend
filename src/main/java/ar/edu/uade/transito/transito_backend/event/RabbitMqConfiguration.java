package ar.edu.uade.transito.transito_backend.event;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "core.rabbitmq.enabled", havingValue = "true")
public class RabbitMqConfiguration {

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /** Core owns q.transito; verify it exists without redeclaring or changing it. */
    @Bean
    ApplicationRunner checkCoreQueue(RabbitTemplate rabbitTemplate,
                                     @Value("${core.rabbitmq.queue}") String queue) {
        return arguments -> rabbitTemplate.execute(channel -> channel.queueDeclarePassive(queue));
    }
}
