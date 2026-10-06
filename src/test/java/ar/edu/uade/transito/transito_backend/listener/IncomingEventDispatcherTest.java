package ar.edu.uade.transito.transito_backend.listener;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import ar.edu.uade.transito.transito_backend.repository.ProcessedEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class IncomingEventDispatcherTest {

    private static final String EVENT_ID = "7f9c91f0-03c6-4baf-8791-8cb2c38981d2";

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode message(String eventType) {
        return mapper.readTree("{\"eventId\":\"" + EVENT_ID + "\",\"eventType\":\"" + eventType
                + "\",\"data\":{\"ticketId\":1}}");
    }

    @Test
    void dispatchesNewEventToItsHandlerAndRecordsIt() {
        IncomingEventHandler handler = mock(IncomingEventHandler.class);
        when(handler.eventType()).thenReturn("ticketUpdated");
        ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
        when(repository.existsById(any())).thenReturn(false);

        new IncomingEventDispatcher(List.of(handler), repository).dispatch(message("ticketUpdated"));

        verify(handler).handle(argThat(data -> data.path("ticketId").asInt() == 1));
        verify(repository).save(argThat(saved -> saved.getEventId().toString().equals(EVENT_ID)
                && "ticketUpdated".equals(saved.getEventType())));
    }

    @Test
    void discardsEventAlreadyProcessed() {
        IncomingEventHandler handler = mock(IncomingEventHandler.class);
        when(handler.eventType()).thenReturn("ticketUpdated");
        ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
        when(repository.existsById(any())).thenReturn(true);

        new IncomingEventDispatcher(List.of(handler), repository).dispatch(message("ticketUpdated"));

        verify(handler, never()).handle(any());
        verify(repository, never()).save(any());
    }

    @Test
    void ignoresEventWithoutHandlerAndDoesNotRecordIt() {
        IncomingEventHandler handler = mock(IncomingEventHandler.class);
        when(handler.eventType()).thenReturn("ticketUpdated");
        ProcessedEventRepository repository = mock(ProcessedEventRepository.class);

        new IncomingEventDispatcher(List.of(handler), repository).dispatch(message("paymentRegistered"));

        verify(handler, never()).handle(any());
        verify(repository, never()).save(any());
    }

    @Test
    void handlerFailureIsPropagatedAndEventIsNotRecorded() {
        IncomingEventHandler handler = mock(IncomingEventHandler.class);
        when(handler.eventType()).thenReturn("ticketUpdated");
        doThrow(new IllegalStateException("fallo")).when(handler).handle(any());
        ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
        when(repository.existsById(any())).thenReturn(false);
        IncomingEventDispatcher dispatcher = new IncomingEventDispatcher(List.of(handler), repository);

        assertThrows(IllegalStateException.class, () -> dispatcher.dispatch(message("ticketUpdated")));

        verify(repository, never()).save(any());
    }
}
