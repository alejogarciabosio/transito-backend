package ar.edu.uade.transito.transito_backend.event;

import ar.edu.uade.transito.transito_backend.model.OutboxEvent;

/**
 * Envío real de un evento ya guardado en el outbox (Strategy): RabbitMQ hacia Core, o log en el
 * perfil local sin broker. Lo usa solo el {@link OutboxRelay}; los servicios siguen usando
 * {@link EventPublisher}, que ahora guarda en el outbox.
 */
public interface EventTransport {

    /** Envía el sobre tal cual quedó guardado. Lanza excepción si el broker no lo aceptó. */
    void send(OutboxEvent event);
}
