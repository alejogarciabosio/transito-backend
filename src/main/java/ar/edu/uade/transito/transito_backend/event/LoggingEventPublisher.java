package ar.edu.uade.transito.transito_backend.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "core.rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
//Esta clase corresponde al patrón de diseño Strategy porque implementa la interfaz EventPublisher y proporciona una implementación concreta del método publish. Al utilizar esta clase, se puede cambiar fácilmente la forma en que se publican los eventos sin modificar el código que depende de la interfaz EventPublisher. Esto permite una mayor flexibilidad y escalabilidad en el sistema, ya que se pueden agregar nuevas estrategias de publicación de eventos en el futuro sin afectar el resto del código.
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public <T> void publish(EventEnvelope<T> event) {
        log.info("[event] type={} eventId={} occurredAt={} data={}",
                event.getEventType(), event.getEventId(), event.getOccurredAt(), event.getData());
    }

    @Override
    public void publish(TicketStatusEnvelope event) {
        log.info("[ticket-event] type={} eventId={} occurredAt={} data={}",
                event.eventType(), event.eventId(), event.occurredAt(), event.data());
    }
}
