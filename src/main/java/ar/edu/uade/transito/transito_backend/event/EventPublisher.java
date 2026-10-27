package ar.edu.uade.transito.transito_backend.event;

public interface EventPublisher {

    <T> void publish(EventEnvelope<T> event);

    /** Publishes the M2-specific ticket envelope, which deliberately differs from Core's envelope. */
    void publish(TicketStatusEnvelope event);
}
