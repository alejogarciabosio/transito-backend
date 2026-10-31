package ar.edu.uade.transito.transito_backend.event;

public interface EventPublisher {

    <T> void publish(EventEnvelope<T> event);
}
