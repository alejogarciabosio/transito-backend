package ar.edu.uade.transito.transito_backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.enums.ParkingTariffStatus;

public record ParkingTariffResponse(
        UUID tariffId,
        UUID zoneId,
        BigDecimal pricePerHour,
        LocalDate validFrom,
        LocalDate validTo,
        ParkingTariffStatus status) {
}
