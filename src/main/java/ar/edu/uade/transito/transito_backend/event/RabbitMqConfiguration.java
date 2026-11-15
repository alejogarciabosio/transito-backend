package ar.edu.uade.transito.transito_backend.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import ar.edu.uade.transito.transito_backend.listener.SafeRejectingErrorHandler;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "core.rabbitmq.enabled", havingValue = "true")
public class RabbitMqConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConfiguration.class);

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Misma fábrica que arma Spring Boot (mismas propiedades spring.rabbitmq.listener.*), con un ErrorHandler
     * que rechaza sin requeue y loguea solo metadatos (DEF-05: sin cuerpo ni fragmentos del mensaje).
     */
    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setErrorHandler(new SafeRejectingErrorHandler());
        return factory;
    }

    /**
     * Core es dueña de q.transito; solo verificamos que exista, sin redeclararla ni cambiarla.
     * Si la conexión falla (ej. nuestra IP todavía no está habilitada en el firewall de Core),
     * solo lo avisa por log: no debe tirar abajo el arranque de toda la app por un chequeo
     * meramente informativo.
     */
    @Bean
    ApplicationRunner checkCoreQueue(RabbitTemplate rabbitTemplate,
                                     @Value("${core.rabbitmq.queue}") String queue) {
        return arguments -> {
            try {
                rabbitTemplate.execute(channel -> channel.queueDeclarePassive(queue));
                log.info("[core-rabbitmq] conexión verificada, cola '{}' accesible", queue);
            } catch (Exception ex) {
                log.warn("[core-rabbitmq] no se pudo verificar la cola '{}' al arrancar "
                        + "(¿IP todavía no habilitada en Core, o credenciales incorrectas?): {}",
                        queue, ex.getMessage());
            }
        };
    }
}
