package ar.edu.uade.transito.transito_backend.web.mapper;

import ar.edu.uade.transito.transito_backend.model.ParkingSession;
import ar.edu.uade.transito.transito_backend.web.dto.CreateParkingSessionRequest;
import ar.edu.uade.transito.transito_backend.web.dto.ParkingSessionResponse;

public final class ParkingSessionMapper {

    private ParkingSessionMapper() {
    }

    public static ParkingSession toEntity(CreateParkingSessionRequest request) {
        return ParkingSession.builder()
                .licensePlate(request.licensePlate())
                .zoneId(request.zoneId())
                .startDateTime(request.startDateTime())
                .build();
    }

    public static ParkingSessionResponse toResponse(ParkingSession session) {
        return new ParkingSessionResponse(
                session.getSessionId(),
                session.getLicensePlate(),
                session.getZoneId(),
                session.getStartDateTime(),
                session.getEndDateTime(),
                session.isPaid(),
                session.getPaymentReference());
    }
}
