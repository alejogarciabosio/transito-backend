package ar.edu.uade.transito.transito_backend.web.dto;

import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.ParkingZoneStatus;

public record ParkingZoneResponse(
        UUID zoneId,
        String name,
        String boundaries,
        String allowedHours,
        ParkingZoneStatus status) {
}
