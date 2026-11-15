package ar.edu.uade.transito.transito_backend.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.util.ErrorHandler;

/**
 * DEF-05: reemplaza al {@code ConditionalRejectingErrorHandler} por defecto, que ante un JSON roto loguea
 * el mensaje de Jackson (cita fragmentos del cuerpo: DNI, patente...) y el stack trace completo.
 *
 * <p>Comportamiento: TODO fallo del listener (JSON roto, conversión fallida, payload inválido) se rechaza
 * sin requeue, igual que antes ({@code default-requeue-rejected=false}); si la cola tiene DLX (la de Core)
 * termina en la DLQ. El log solo lleva metadatos: cola, messageId y eventId de las propiedades AMQP si
 * vienen, y los tipos de excepción. Nunca el cuerpo ni el texto de la excepción original.
 */
public class SafeRejectingErrorHandler implements ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(SafeRejectingErrorHandler.class);

    @Override
    public void handleError(Throwable t) {
        Message failed = t instanceof ListenerExecutionFailedException lefe ? lefe.getFailedMessage() : null;
        MessageProperties props = failed == null ? null : failed.getMessageProperties();
        String queue = props == null ? null : props.getConsumerQueue();
        String messageId = props == null ? null : props.getMessageId();
        Object eventId = props == null ? null : props.getHeaders().get("eventId");
        String error = exceptionTypes(t);
        log.warn("[event-rejected] mensaje rechazado sin requeue (a la DLQ si la cola tiene DLX) "
                + "queue={} messageId={} eventId={} error={}", queue, messageId, eventId, error);
        // Sin causa ni texto original: el contenedor loguea esta excepción y no debe arrastrar el payload.
        throw new AmqpRejectAndDontRequeueException("Mensaje rechazado sin requeue (" + error + ")");
    }

    /** Cadena de tipos de excepción (sin mensajes), p. ej. "ListenerExecutionFailedException <- StreamReadException". */
    static String exceptionTypes(Throwable t) {
        StringBuilder types = new StringBuilder(t.getClass().getSimpleName());
        Throwable current = t;
        int depth = 0;
        while (current.getCause() != null && current.getCause() != current && depth++ < 5) {
            current = current.getCause();
            types.append(" <- ").append(current.getClass().getSimpleName());
        }
        return types.toString();
    }
}
