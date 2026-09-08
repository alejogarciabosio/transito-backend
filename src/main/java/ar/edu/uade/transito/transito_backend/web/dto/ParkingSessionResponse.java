package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ParkingSessionResponse(
        UUID sessionId,
        String licensePlate,
        UUID zoneId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        boolean paid,
        String paymentReference) {
}
