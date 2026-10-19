package ar.edu.uade.transito.transito_backend.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record FlagUnpaidRequest(
        @NotNull(message = "El importe base es obligatorio")
        @DecimalMin(value = "0.01", message = "El importe base debe ser mayor que cero")
        BigDecimal baseAmount) {
}
