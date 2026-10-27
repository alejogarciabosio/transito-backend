package ar.edu.uade.transito.transito_backend.support;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.TicketStatusEnvelope;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Publisher de test: en vez de loguear, guarda cada evento publicado para que los tests
 * puedan validar tipo, sobre (envelope) y payload.
 */
public class RecordingEventPublisher implements EventPublisher {

    private final List<EventEnvelope<?>> events = new CopyOnWriteArrayList<>();
    private final List<TicketStatusEnvelope> ticketEvents = new CopyOnWriteArrayList<>();

    @Override
    public <T> void publish(EventEnvelope<T> event) {
        events.add(event);
    }

    @Override
    public void publish(TicketStatusEnvelope event) {
        ticketEvents.add(event);
    }

    public List<EventEnvelope<?>> all() {
        return List.copyOf(events);
    }

    public List<EventEnvelope<?>> ofType(String eventType) {
        return events.stream().filter(e -> eventType.equals(e.getEventType())).toList();
    }

    @SuppressWarnings("unchecked")
    public <T> T lastPayload(String eventType, Class<T> type) {
        List<EventEnvelope<?>> matching = ofType(eventType);
        if (matching.isEmpty()) {
            throw new AssertionError("No se publicó ningún evento " + eventType + ". Publicados: "
                    + events.stream().map(EventEnvelope::getEventType).toList());
        }
        return (T) matching.get(matching.size() - 1).getData();
    }

    public void clear() {
        events.clear();
        ticketEvents.clear();
    }

    public List<TicketStatusEnvelope> ticketEvents() {
        return List.copyOf(ticketEvents);
    }
}
