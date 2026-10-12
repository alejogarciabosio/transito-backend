package ar.edu.uade.transito.transito_backend.integration;

import ar.edu.uade.transito.transito_backend.event.payload.TrafficOperationCreatedPayload;
import ar.edu.uade.transito.transito_backend.model.enums.OperationStatus;
import ar.edu.uade.transito.transito_backend.model.enums.OperationType;
import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Evento entrante ticketUpdated (Módulo 2)")
class TicketEventListenerIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("TC-BE-EVT-01: ticket ROUTED a M7 → crea operativo COMPLAINT_VERIFICATION OPEN y publica trafficOperationCreated")
    void routedToTransito() {
        dispatchIncoming("ticketUpdated", "{\"ticketId\":4521,\"responsibleAreaId\":\"M7\",\"updateType\":\"ROUTED\"}");
        var ops = trafficOperationRepository.findAll();
        assertThat(ops).hasSize(1);
        assertThat(ops.get(0).getType()).isEqualTo(OperationType.COMPLAINT_VERIFICATION);
        assertThat(ops.get(0).getStatus()).isEqualTo(OperationStatus.OPEN);
        assertThat(ops.get(0).getTicketId()).isEqualTo("4521");
        var p = events.lastPayload("trafficOperationCreated", TrafficOperationCreatedPayload.class);
        assertThat(p.ticketId()).isEqualTo("4521");
    }

    @Test
    @DisplayName("TC-BE-EVT-02: ticket de otra área → se ignora")
    void otherArea() {
        dispatchIncoming("ticketUpdated", "{\"ticketId\":1,\"responsibleAreaId\":\"M3\",\"updateType\":\"ROUTED\"}");
        dispatchIncoming("ticketUpdated", "{\"ticketId\":2,\"responsibleAreaId\":null,\"updateType\":\"ROUTED\"}");
        assertThat(trafficOperationRepository.count()).isZero();
        assertThat(events.all()).isEmpty();
    }

    @Test
    @DisplayName("TC-BE-EVT-03: updateType distinto de ROUTED (PROGRESS/RETURNED) → no crea nada")
    void otherUpdateTypes() {
        dispatchIncoming("ticketUpdated", "{\"ticketId\":1,\"responsibleAreaId\":\"M7\",\"updateType\":\"PROGRESS\"}");
        dispatchIncoming("ticketUpdated", "{\"ticketId\":1,\"responsibleAreaId\":\"M7\",\"updateType\":\"RETURNED\"}");
        assertThat(trafficOperationRepository.count()).isZero();
    }
}
