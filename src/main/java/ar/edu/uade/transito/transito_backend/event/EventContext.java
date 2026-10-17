package ar.edu.uade.transito.transito_backend.event;

import java.util.UUID;

/** Conserva la trazabilidad del evento recibido mientras se procesa su respuesta. */
public final class EventContext {

    public record Metadata(UUID eventId, UUID correlationId) {}

    private static final ThreadLocal<Metadata> CURRENT = new ThreadLocal<>();

    private EventContext() {}

    public static Metadata current() { return CURRENT.get(); }

    public static Scope open(UUID eventId, UUID correlationId) {
        CURRENT.set(new Metadata(eventId, correlationId == null ? eventId : correlationId));
        return CURRENT::remove;
    }

    public interface Scope extends AutoCloseable {
        @Override void close();
    }
}
