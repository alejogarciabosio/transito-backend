package ar.edu.uade.transito.transito_backend.web.dto;

public record CreateParkingZoneRequest(
        String name,
        String boundaries,
        String allowedHours) {
}
