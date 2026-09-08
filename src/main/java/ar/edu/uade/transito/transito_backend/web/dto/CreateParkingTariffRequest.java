package ar.edu.uade.transito.transito_backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateParkingTariffRequest(
        UUID zoneId,
        BigDecimal pricePerHour,
        LocalDate validFrom,
        LocalDate validTo) {
}
