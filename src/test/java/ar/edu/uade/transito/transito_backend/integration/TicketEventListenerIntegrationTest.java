package ar.edu.uade.transito.transito_backend.integration;

import ar.edu.uade.transito.transito_backend.event.payload.TrafficOperationCreatedPayload;
import ar.edu.uade.transito.transito_backend.model.enums.OperationStatus;
import ar.edu.uade.transito.transito_backend.model.enums.OperationType;
import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

@DisplayName("Evento entrante ticketUpdated (Módulo 2)")
class TicketEventListenerIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("TC-BE-EVT-01: ticket UUID ROUTED a M7 → crea operativo y publica STARTED para M2")
    void routedToTransito() {
        UUID ticketId = UUID.randomUUID();
        dispatchIncoming("ticketUpdated", "{\"ticketId\":\"" + ticketId
                + "\",\"responsibleAreaId\":\"M7\",\"updateType\":\"ROUTED\"}");
        var ops = trafficOperationRepository.findAll();
        assertThat(ops).hasSize(1);
        assertThat(ops.get(0).getType()).isEqualTo(OperationType.COMPLAINT_VERIFICATION);
        assertThat(ops.get(0).getStatus()).isEqualTo(OperationStatus.OPEN);
        assertThat(ops.get(0).getTicketId()).isEqualTo(ticketId.toString());
        var p = events.lastPayload("trafficOperationCreated", TrafficOperationCreatedPayload.class);
        assertThat(p.ticketId()).isEqualTo(ticketId.toString());
        assertThat(events.ticketEvents()).singleElement().satisfies(status -> {
            assertThat(status.specVersion()).isEqualTo("1.0");
            assertThat(status.eventType()).isEqualTo("updateTicketStatus");
            assertThat(status.subject()).isEqualTo("tickets/" + ticketId);
            assertThat(status.producer().moduleId()).isEqualTo("M7");
            assertThat(status.data().ticketId()).isEqualTo(ticketId);
            assertThat(status.data().updateType()).isEqualTo("STARTED");
            assertThat(status.data().progress()).isZero();
            assertThat(status.data().updatedBy().id()).isEqualTo("M7");
        });
    }

    @Test
    @DisplayName("TC-BE-EVT-02: ticket de otra área → se ignora")
    void otherArea() {
        dispatchIncoming("ticketUpdated", "{\"ticketId\":\"" + UUID.randomUUID()
                + "\",\"responsibleAreaId\":\"M3\",\"updateType\":\"ROUTED\"}");
        dispatchIncoming("ticketUpdated", "{\"ticketId\":\"" + UUID.randomUUID()
                + "\",\"responsibleAreaId\":null,\"updateType\":\"ROUTED\"}");
        assertThat(trafficOperationRepository.count()).isZero();
        assertThat(events.all()).isEmpty();
        assertThat(events.ticketEvents()).isEmpty();
    }

    @Test
    @DisplayName("TC-BE-EVT-03: updateType distinto de ROUTED (PROGRESS/RETURNED) → no crea nada")
    void otherUpdateTypes() {
        dispatchIncoming("ticketUpdated", "{\"ticketId\":\"" + UUID.randomUUID()
                + "\",\"responsibleAreaId\":\"M7\",\"updateType\":\"PROGRESS\"}");
        dispatchIncoming("ticketUpdated", "{\"ticketId\":\"" + UUID.randomUUID()
                + "\",\"responsibleAreaId\":\"M7\",\"updateType\":\"RETURNED\"}");
        assertThat(trafficOperationRepository.count()).isZero();
        assertThat(events.ticketEvents()).isEmpty();
    }
}
