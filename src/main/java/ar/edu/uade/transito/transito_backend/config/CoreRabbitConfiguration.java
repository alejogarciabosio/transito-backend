package ar.edu.uade.transito.transito_backend.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** No declara q.transito: Core la administra y el listener la verifica pasivamente. */
@Configuration
@ConditionalOnProperty(prefix = "core.integration", name = "enabled", havingValue = "true")
public class CoreRabbitConfiguration {

    @Bean
    SimpleRabbitListenerContainerFactory coreRabbitListenerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
