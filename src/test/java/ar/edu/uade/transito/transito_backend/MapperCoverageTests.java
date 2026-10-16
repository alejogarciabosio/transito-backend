package ar.edu.uade.transito.transito_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ar.edu.uade.transito.transito_backend.model.ParkingSession;
import ar.edu.uade.transito.transito_backend.model.ParkingTariff;
import ar.edu.uade.transito.transito_backend.model.ParkingZone;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.model.VehicleImpoundment;
import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentOriginType;
import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentStatus;
import ar.edu.uade.transito.transito_backend.model.enums.OperationResolution;
import ar.edu.uade.transito.transito_backend.model.enums.OperationStatus;
import ar.edu.uade.transito.transito_backend.model.enums.OperationType;
import ar.edu.uade.transito.transito_backend.model.enums.ParkingTariffStatus;
import ar.edu.uade.transito.transito_backend.model.enums.ParkingZoneStatus;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;
import ar.edu.uade.transito.transito_backend.web.dto.CreateParkingSessionRequest;
import ar.edu.uade.transito.transito_backend.web.dto.CreateParkingTariffRequest;
import ar.edu.uade.transito.transito_backend.web.dto.CreateParkingZoneRequest;
import ar.edu.uade.transito.transito_backend.web.dto.CreateTrafficOperationRequest;
import ar.edu.uade.transito.transito_backend.web.dto.CreateVehicleImpoundmentRequest;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateParkingTariffRequest;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateParkingZoneRequest;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateTrafficOperationRequest;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateVehicleImpoundmentRequest;
import ar.edu.uade.transito.transito_backend.web.mapper.ParkingSessionMapper;
import ar.edu.uade.transito.transito_backend.web.mapper.ParkingTariffMapper;
import ar.edu.uade.transito.transito_backend.web.mapper.ParkingZoneMapper;
import ar.edu.uade.transito.transito_backend.web.mapper.StreetClosureMapper;
import ar.edu.uade.transito.transito_backend.web.mapper.TrafficOperationMapper;
import ar.edu.uade.transito.transito_backend.web.mapper.VehicleImpoundmentMapper;

class MapperCoverageTests {

    @Test
    void mapsVehicleImpoundmentRequestsAndResponse() {
        UUID originId = UUID.randomUUID();
        LocalDateTime entry = LocalDateTime.now().minusHours(1);
        VehicleImpoundment created = VehicleImpoundmentMapper.toEntity(new CreateVehicleImpoundmentRequest(
                "AB123CD", "30111222", "Falta grave", "Depósito Norte", entry,
                ImpoundmentOriginType.INFRACTION, originId));
        VehicleImpoundment changes = VehicleImpoundmentMapper.toEntity(
                new UpdateVehicleImpoundmentRequest("Nuevo motivo", "Depósito Sur", "30222333"));
        created.setImpoundmentId(UUID.randomUUID());
        created.setStatus(ImpoundmentStatus.IMPOUNDED);

        assertEquals("AB123CD", created.getLicensePlate());
        assertEquals("Nuevo motivo", changes.getReason());
        assertEquals(originId, VehicleImpoundmentMapper.toResponse(created).originId());
    }

    @Test
    void mapsTrafficOperationRequestsAndResponse() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        TrafficOperation created = TrafficOperationMapper.toEntity(new CreateTrafficOperationRequest(
                OperationType.ROUTINE_CONTROL, "Centro", start, null,
                List.of("inspector-1"), List.of("movil-1"), "TICKET-1"));
        TrafficOperation changes = TrafficOperationMapper.toEntity(new UpdateTrafficOperationRequest(
                "Norte", LocalDateTime.now(), List.of("inspector-2"), List.of("movil-2")));
        created.setOperationId(UUID.randomUUID());
        created.setStatus(OperationStatus.CLOSED);
        created.setResolution(OperationResolution.NO_ACTION);

        assertEquals(OperationType.ROUTINE_CONTROL, created.getType());
        assertEquals("Norte", changes.getZone());
        assertEquals(OperationResolution.NO_ACTION, TrafficOperationMapper.toResponse(created).resolution());
    }

    @Test
    void mapsParkingZoneRequestsAndResponse() {
        ParkingZone created = ParkingZoneMapper.toEntity(
                new CreateParkingZoneRequest("Centro", "A-B-C-D", "08:00-20:00"));
        ParkingZone changes = ParkingZoneMapper.toEntity(
                new UpdateParkingZoneRequest("Norte", "E-F-G-H", "09:00-21:00", ParkingZoneStatus.INACTIVE));
        created.setZoneId(UUID.randomUUID());
        created.setStatus(ParkingZoneStatus.ACTIVE);

        assertEquals("Centro", created.getName());
        assertEquals(ParkingZoneStatus.INACTIVE, changes.getStatus());
        assertEquals(ParkingZoneStatus.ACTIVE, ParkingZoneMapper.toResponse(created).status());
    }

    @Test
    void mapsParkingTariffRequestsAndResponse() {
        UUID zoneId = UUID.randomUUID();
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusMonths(1);
        ParkingTariff created = ParkingTariffMapper.toEntity(
                new CreateParkingTariffRequest(zoneId, new BigDecimal("1000"), from, to));
        ParkingTariff changes = ParkingTariffMapper.toEntity(
                new UpdateParkingTariffRequest(new BigDecimal("1200"), from, to.plusMonths(1)));
        created.setTariffId(UUID.randomUUID());
        created.setStatus(ParkingTariffStatus.ACTIVE);

        assertEquals(zoneId, created.getZoneId());
        assertEquals(new BigDecimal("1200"), changes.getPricePerHour());
        assertEquals(ParkingTariffStatus.ACTIVE, ParkingTariffMapper.toResponse(created).status());
    }

    @Test
    void mapsParkingSessionAndStreetClosureResponses() {
        UUID zoneId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().minusMinutes(5);
        ParkingSession session = ParkingSessionMapper.toEntity(
                new CreateParkingSessionRequest("AB123CD", zoneId, start));
        session.setSessionId(UUID.randomUUID());
        StreetClosure closure = StreetClosure.builder()
                .streetClosureId(UUID.randomUUID())
                .closureRequestId("REQ-1")
                .requestingModule(RequestingModule.AMBIENTE)
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now().plusDays(1))
                .affectedStreet(List.of("Av. Corrientes"))
                .status(StreetClosureStatus.REQUESTED)
                .build();

        assertEquals(zoneId, ParkingSessionMapper.toResponse(session).zoneId());
        assertNull(ParkingSessionMapper.toResponse(session).endDateTime());
        assertEquals(RequestingModule.AMBIENTE, StreetClosureMapper.toResponse(closure).requestingModule());
    }
}
