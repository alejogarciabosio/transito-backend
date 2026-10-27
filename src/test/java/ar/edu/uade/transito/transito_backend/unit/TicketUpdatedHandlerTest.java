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

import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.TicketStatusEnvelope;
import ar.edu.uade.transito.transito_backend.listener.dto.TicketUpdatedPayload;
import ar.edu.uade.transito.transito_backend.listener.handler.TicketUpdatedHandler;
import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.model.enums.OperationType;
import ar.edu.uade.transito.transito_backend.service.TrafficOperationService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class TicketUpdatedHandlerTest {

    @Test
    void routedTicketForM7CreatesOperationAndPublishesStartedUsingM2Envelope() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        TrafficOperationService operations = mock(TrafficOperationService.class);
        EventPublisher publisher = mock(EventPublisher.class);
        JsonNode data = mock(JsonNode.class);
        UUID ticketId = UUID.randomUUID();
        when(mapper.convertValue(data, TicketUpdatedPayload.class))
                .thenReturn(new TicketUpdatedPayload(ticketId, "M7", "ROUTED", null));

        new TicketUpdatedHandler(mapper, operations, publisher).handle(data);

        ArgumentCaptor<TrafficOperation> operation = ArgumentCaptor.forClass(TrafficOperation.class);
        verify(operations).create(operation.capture());
        assertThat(operation.getValue().getType()).isEqualTo(OperationType.COMPLAINT_VERIFICATION);
        assertThat(operation.getValue().getTicketId()).isEqualTo(ticketId.toString());

        ArgumentCaptor<TicketStatusEnvelope> status = ArgumentCaptor.forClass(TicketStatusEnvelope.class);
        verify(publisher).publish(status.capture());
        assertThat(status.getValue().eventType()).isEqualTo("updateTicketStatus");
        assertThat(status.getValue().producer().moduleId()).isEqualTo("M7");
        assertThat(status.getValue().subject()).isEqualTo("tickets/" + ticketId);
        assertThat(status.getValue().data().ticketId()).isEqualTo(ticketId);
        assertThat(status.getValue().data().updateType()).isEqualTo("STARTED");
        assertThat(status.getValue().data().progress()).isZero();
    }

    @Test
    void ignoresTicketThatIsNotRoutedToM7() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        TrafficOperationService operations = mock(TrafficOperationService.class);
        EventPublisher publisher = mock(EventPublisher.class);
        JsonNode data = mock(JsonNode.class);
        when(mapper.convertValue(data, TicketUpdatedPayload.class))
                .thenReturn(new TicketUpdatedPayload(UUID.randomUUID(), "M3", "ROUTED", null));

        new TicketUpdatedHandler(mapper, operations, publisher).handle(data);

        verify(operations, never()).create(any());
        verify(publisher, never()).publish(any(TicketStatusEnvelope.class));
    }
}
