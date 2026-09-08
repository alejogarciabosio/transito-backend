package ar.edu.uade.transito.transito_backend.web.mapper;

import ar.edu.uade.transito.transito_backend.model.ParkingTariff;
import ar.edu.uade.transito.transito_backend.web.dto.CreateParkingTariffRequest;
import ar.edu.uade.transito.transito_backend.web.dto.ParkingTariffResponse;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateParkingTariffRequest;

public final class ParkingTariffMapper {

    private ParkingTariffMapper() {
    }

    public static ParkingTariff toEntity(CreateParkingTariffRequest request) {
        return ParkingTariff.builder()
                .zoneId(request.zoneId())
                .pricePerHour(request.pricePerHour())
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .build();
    }

    public static ParkingTariff toEntity(UpdateParkingTariffRequest request) {
        return ParkingTariff.builder()
                .pricePerHour(request.pricePerHour())
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .build();
    }

    public static ParkingTariffResponse toResponse(ParkingTariff tariff) {
        return new ParkingTariffResponse(
                tariff.getTariffId(),
                tariff.getZoneId(),
                tariff.getPricePerHour(),
                tariff.getValidFrom(),
                tariff.getValidTo());
    }
}
