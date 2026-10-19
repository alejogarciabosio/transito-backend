package ar.edu.uade.transito.transito_backend.listener.handler;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import ar.edu.uade.transito.transito_backend.listener.IncomingEventHandler;
import ar.edu.uade.transito.transito_backend.listener.dto.RentasPaymentPayload;
import ar.edu.uade.transito.transito_backend.service.InfractionPaymentEventService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentRegisteredHandler implements IncomingEventHandler {
    private final ObjectMapper objectMapper;
    private final InfractionPaymentEventService paymentEventService;

    @Override public String eventType() { return "paymentRegistered"; }

    @Override public void handle(JsonNode data) {
        paymentEventService.record(eventType(), objectMapper.convertValue(data, RentasPaymentPayload.class));
    }
}
