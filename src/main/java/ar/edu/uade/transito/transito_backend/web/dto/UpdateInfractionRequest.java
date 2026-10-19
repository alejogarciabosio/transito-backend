package ar.edu.uade.transito.transito_backend.web.dto;

import java.math.BigDecimal;
import java.util.List;

import ar.edu.uade.transito.transito_backend.model.Location;
import ar.edu.uade.transito.transito_backend.model.enums.DebtorIdType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateInfractionRequest(
        @Size(min = 1, max = 15, message = "La patente debe tener entre 1 y 15 caracteres") String licensePlate,
        @Valid Location location,
        List<@NotBlank(message = "Las URL de evidencia no pueden estar vacías") String> evidenceUrl,
        String offenderNationalId,
        String vehicleOwnerTaxId,
        String debtorId,
        DebtorIdType debtorIdType,
        @DecimalMin(value = "0.0", inclusive = true, message = "El importe base no puede ser negativo") BigDecimal baseAmount,
        List<@NotBlank(message = "Los agravantes no pueden estar vacíos") String> aggravatingFactors,
        @DecimalMin(value = "0.0", inclusive = true, message = "El importe final no puede ser negativo") BigDecimal finalAmount) {

    @AssertTrue(message = "debtorId y debtorIdType deben informarse juntos")
    public boolean isDebtorIdentificationComplete() {
        return (debtorId == null && debtorIdType == null)
                || (debtorId != null && !debtorId.isBlank() && debtorIdType != null);
    }

    @AssertTrue(message = "El importe final no puede ser menor que el importe base")
    public boolean isFinalAmountValid() {
        return baseAmount == null || finalAmount == null || finalAmount.compareTo(baseAmount) >= 0;
    }
}
