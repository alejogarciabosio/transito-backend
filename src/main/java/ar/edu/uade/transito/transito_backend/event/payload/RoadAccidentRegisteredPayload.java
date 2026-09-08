package ar.edu.uade.transito.transito_backend.event.payload;

import java.time.LocalDateTime;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.AccidentStatus;

public record RoadAccidentRegisteredPayload(
        UUID accidentId,
        LocalDateTime accidentDateTime,
        boolean involvesInfrastructureDamage,
        String inspectorId,
        AccidentStatus status,
        String originTicketId) {
}
