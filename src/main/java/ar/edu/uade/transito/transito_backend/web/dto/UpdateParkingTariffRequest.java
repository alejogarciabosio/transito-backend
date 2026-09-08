package ar.edu.uade.transito.transito_backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateParkingTariffRequest(
        BigDecimal pricePerHour,
        LocalDate validFrom,
        LocalDate validTo) {
}
