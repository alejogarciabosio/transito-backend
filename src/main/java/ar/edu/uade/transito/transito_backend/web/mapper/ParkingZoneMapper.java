package ar.edu.uade.transito.transito_backend.web.mapper;

import ar.edu.uade.transito.transito_backend.model.ParkingZone;
import ar.edu.uade.transito.transito_backend.web.dto.CreateParkingZoneRequest;
import ar.edu.uade.transito.transito_backend.web.dto.ParkingZoneResponse;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateParkingZoneRequest;

public final class ParkingZoneMapper {

    private ParkingZoneMapper() {
    }

    public static ParkingZone toEntity(CreateParkingZoneRequest request) {
        return ParkingZone.builder()
                .name(request.name())
                .boundaries(request.boundaries())
                .allowedHours(request.allowedHours())
                .build();
    }

    public static ParkingZone toEntity(UpdateParkingZoneRequest request) {
        return ParkingZone.builder()
                .name(request.name())
                .boundaries(request.boundaries())
                .allowedHours(request.allowedHours())
                .status(request.status())
                .build();
    }

    public static ParkingZoneResponse toResponse(ParkingZone zone) {
        return new ParkingZoneResponse(
                zone.getZoneId(),
                zone.getName(),
                zone.getBoundaries(),
                zone.getAllowedHours(),
                zone.getStatus());
    }
}
