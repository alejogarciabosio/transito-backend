package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateParkingSessionRequest(
        String licensePlate,
        UUID zoneId,
        LocalDateTime startDateTime) {
}
