package ar.edu.uade.transito.transito_backend.web.dto;

import ar.edu.uade.transito.transito_backend.model.enums.ParkingZoneStatus;

public record UpdateParkingZoneRequest(
        String name,
        String boundaries,
        String allowedHours,
        ParkingZoneStatus status) {
}
