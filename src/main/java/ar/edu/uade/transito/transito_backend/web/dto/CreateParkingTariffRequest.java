package ar.edu.uade.transito.transito_backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateParkingTariffRequest(
        @NotNull(message = "La zona es obligatoria") UUID zoneId,
        @NotNull(message = "El precio por hora es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio por hora debe ser mayor que cero") BigDecimal pricePerHour,
        @NotNull(message = "La fecha de vigencia inicial es obligatoria") LocalDate validFrom,
        @NotNull(message = "La fecha de vigencia final es obligatoria") LocalDate validTo) {

    @AssertTrue(message = "La fecha de vigencia final no puede ser anterior a la inicial")
    public boolean isDateRangeValid() {
        return validFrom == null || validTo == null || !validTo.isBefore(validFrom);
    }
}
