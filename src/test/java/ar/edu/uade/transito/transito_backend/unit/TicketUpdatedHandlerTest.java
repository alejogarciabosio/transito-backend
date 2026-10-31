package ar.edu.uade.transito.transito_backend.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.payload.UpdateTicketStatusPayload;
import ar.edu.uade.transito.transito_backend.listener.dto.TicketUpdatedPayload;
import ar.edu.uade.transito.transito_backend.listener.handler.TicketUpdatedHandler;
import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.model.enums.OperationType;
import ar.edu.uade.transito.transito_backend.service.TrafficOperationService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class TicketUpdatedHandlerTest {

    @Test
    void routedTicketForM7CreatesOperationAndPublishesStartedUsingCoreEnvelope() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        TrafficOperationService operations = mock(TrafficOperationService.class);
        EventPublisher publisher = mock(EventPublisher.class);
        JsonNode data = mock(JsonNode.class);
        UUID ticketId = UUID.randomUUID();
        when(mapper.convertValue(data, TicketUpdatedPayload.class))
                .thenReturn(new TicketUpdatedPayload(ticketId, "M7", "ROUTED", null));

        UUID incomingEventId = UUID.randomUUID();
        UUID incomingCorrelationId = UUID.randomUUID();
        new TicketUpdatedHandler(mapper, operations, publisher).handle(data, incomingEventId, incomingCorrelationId);

        ArgumentCaptor<TrafficOperation> operation = ArgumentCaptor.forClass(TrafficOperation.class);
        verify(operations).create(operation.capture());
        assertThat(operation.getValue().getType()).isEqualTo(OperationType.COMPLAINT_VERIFICATION);
        assertThat(operation.getValue().getTicketId()).isEqualTo(ticketId.toString());
        assertThat(operation.getValue().getSourceEventId()).isEqualTo(incomingEventId);
        assertThat(operation.getValue().getSourceCorrelationId()).isEqualTo(incomingCorrelationId);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<EventEnvelope> status = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(publisher).publish(status.capture());
        assertThat(status.getValue().getEventType()).isEqualTo("updateTicketStatus");
        assertThat(status.getValue().getEventVersion()).isEqualTo("1.0");
        assertThat(status.getValue().getSourceModule()).isEqualTo("transito");
        assertThat(status.getValue().getCorrelationId()).isEqualTo(incomingCorrelationId);
        assertThat(status.getValue().getCausationId()).isEqualTo(incomingEventId);
        UpdateTicketStatusPayload payload = (UpdateTicketStatusPayload) status.getValue().getData();
        assertThat(payload.ticketId()).isEqualTo(ticketId);
        assertThat(payload.updateType()).isEqualTo("STARTED");
        assertThat(payload.progress()).isZero();
        assertThat(payload.updatedBy().type()).isEqualTo("SYSTEM");
        assertThat(payload.updatedBy().id()).isNull();
    }

    @Test
    void ignoresTicketThatIsNotRoutedToM7() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        TrafficOperationService operations = mock(TrafficOperationService.class);
        EventPublisher publisher = mock(EventPublisher.class);
        JsonNode data = mock(JsonNode.class);
        when(mapper.convertValue(data, TicketUpdatedPayload.class))
                .thenReturn(new TicketUpdatedPayload(UUID.randomUUID(), "M3", "ROUTED", null));

        new TicketUpdatedHandler(mapper, operations, publisher).handle(data, UUID.randomUUID(), null);

        verify(operations, never()).create(any());
        verify(publisher, never()).publish(any(EventEnvelope.class));
    }
}
