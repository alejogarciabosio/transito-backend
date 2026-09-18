package ar.edu.uade.transito.transito_backend.listener;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import ar.edu.uade.transito.transito_backend.listener.dto.TicketUpdatedPayload;
import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.model.enums.OperationType;
import ar.edu.uade.transito.transito_backend.service.TrafficOperationService;
import lombok.RequiredArgsConstructor;

/**
 * Escucha ticketUpdated (Módulo 2), vía el Core. Hoy no hay broker conectado, así que nada invoca
 * todavía a {@link #onTicketUpdated}: cuando el equipo defina Kafka/RabbitMQ, este método se anota
 * con @KafkaListener(topics = "...") o el mecanismo que corresponda, y pasa a ejecutarse en serio.
 */
@Component
@RequiredArgsConstructor
public class TicketEventListener {

    private static final String ROUTED = "ROUTED";
    private static final String TRANSITO_AREA_ID = "M7";

    private final TrafficOperationService trafficOperationService;

    // TODO: reemplazar por @KafkaListener/@RabbitListener una vez definido el broker con Core.
    public void onTicketUpdated(TicketUpdatedPayload payload) {
        if (!TRANSITO_AREA_ID.equals(payload.responsibleAreaId())) {
            return; // el ticket no es de Tránsito, se ignora (contrato ya documentado con M2)
        }

        if (ROUTED.equals(payload.updateType())) {
            // Nace un caso nuevo -> se crea el TrafficOperation. El "type" y "zone" reales saldrían
            // del detalle completo del ticket (hoy simplificado, ver TicketUpdatedPayload).
            TrafficOperation operation = TrafficOperation.builder()
                    .type(OperationType.COMPLAINT_VERIFICATION)
                    .zone("A definir desde details.routing.location")
                    .startDateTime(LocalDateTime.now())
                    .ticketId(String.valueOf(payload.ticketId()))
                    .build();

            trafficOperationService.create(operation);
        }

        // El resto de los updateType (PROGRESS, RETURNED, etc.) actualizarían un TrafficOperation
        // ya existente -- lógica todavía sin definir.
    }
}
