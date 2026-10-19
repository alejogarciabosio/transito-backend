package ar.edu.uade.transito.transito_backend.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ar.edu.uade.transito.transito_backend.listener.dto.RentasPaymentPayload;
import ar.edu.uade.transito.transito_backend.model.InfractionPaymentEvent;
import ar.edu.uade.transito.transito_backend.repository.InfractionPaymentEventRepository;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import ar.edu.uade.transito.transito_backend.service.InfractionPaymentEventService;

class InfractionPaymentEventServiceTest {

    @Test
    void ignoresEventsThatAreNotTrafficFines() {
        InfractionRepository infractions = mock(InfractionRepository.class);
        InfractionPaymentEventRepository events = mock(InfractionPaymentEventRepository.class);
        InfractionPaymentEventService service = new InfractionPaymentEventService(infractions, events);

        service.record("paymentRegistered", new RentasPaymentPayload(
                "pay-ignored", null, "TRAFFIC_INFRACTION", "inf-00850", null,
                BigDecimal.ONE, null, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null,
                OffsetDateTime.now(), null, null));

        org.mockito.Mockito.verifyNoInteractions(events);
    }

    @Test
    void recordsTheTrafficFineContractDefinedByRentas() {
        InfractionRepository infractions = mock(InfractionRepository.class);
        InfractionPaymentEventRepository events = mock(InfractionPaymentEventRepository.class);
        InfractionPaymentEventService service = new InfractionPaymentEventService(infractions, events);
        OffsetDateTime paidAt = OffsetDateTime.parse("2026-10-10T16:00:00-03:00");
        UUID infractionId = UUID.randomUUID();
        when(infractions.existsById(infractionId)).thenReturn(true);

        service.record("paymentRegistered", new RentasPaymentPayload(
                "PAY-9200", "DEBT-3100", "TRAFFIC_FINE", infractionId.toString(), infractionId.toString(),
                new BigDecimal("75000.00"), null, BigDecimal.ZERO, null, null, null, null,
                paidAt, null, null));

        ArgumentCaptor<InfractionPaymentEvent> saved = ArgumentCaptor.forClass(InfractionPaymentEvent.class);
        verify(events).save(saved.capture());
        assertThat(saved.getValue().getOriginId()).isEqualTo(infractionId.toString());
        assertThat(saved.getValue().getInfractionId()).isEqualTo(infractionId);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("75000.00");
        assertThat(saved.getValue().getOccurredAt()).isEqualTo(paidAt);
    }

    @Test
    void linksToTheLocalInfractionWhenOriginIdIsAKnownUuid() {
        InfractionRepository infractions = mock(InfractionRepository.class);
        InfractionPaymentEventRepository events = mock(InfractionPaymentEventRepository.class);
        InfractionPaymentEventService service = new InfractionPaymentEventService(infractions, events);
        UUID infractionId = UUID.randomUUID();
        when(infractions.existsById(infractionId)).thenReturn(true);

        service.record("debtSettled", new RentasPaymentPayload(
                null, "DEBT-3100", "TRAFFIC_FINE", infractionId.toString(), infractionId.toString(),
                null, null, BigDecimal.ZERO, BigDecimal.ZERO, "MULTA_TRANSITO", "c-00123", null, null,
                OffsetDateTime.parse("2026-10-10T16:00:00-03:00"), null));

        ArgumentCaptor<InfractionPaymentEvent> saved = ArgumentCaptor.forClass(InfractionPaymentEvent.class);
        verify(events).save(saved.capture());
        assertThat(saved.getValue().getOriginId()).isEqualTo(infractionId.toString());
        assertThat(saved.getValue().getInfractionId()).isEqualTo(infractionId);
        assertThat(saved.getValue().getConceptCode()).isEqualTo("MULTA_TRANSITO");
    }
}
