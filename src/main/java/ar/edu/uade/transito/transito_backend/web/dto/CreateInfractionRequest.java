package ar.edu.uade.transito.transito_backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.Location;
import ar.edu.uade.transito.transito_backend.model.enums.DebtorIdType;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record CreateInfractionRequest(
        @NotBlank(message = "La patente es obligatoria")
        @Size(max = 15, message = "La patente no puede superar 15 caracteres") String licensePlate,
        @NotNull(message = "El tipo de infracción es obligatorio") InfractionType infractionType,
        @NotNull(message = "La fecha de la infracción es obligatoria")
        @PastOrPresent(message = "La fecha de la infracción no puede estar en el futuro") LocalDateTime infractionDateTime,
        @NotNull(message = "La ubicación es obligatoria") @Valid Location location,
        @NotNull(message = "La lista de evidencias es obligatoria")
        List<@NotBlank(message = "Las URL de evidencia no pueden estar vacías") @Size(max = 255, message = "No puede superar 255 caracteres") String> evidenceUrl,
        @Size(max = 255, message = "No puede superar 255 caracteres") String offenderNationalId,
        @Size(max = 255, message = "No puede superar 255 caracteres") String vehicleOwnerTaxId,
        @Size(max = 255, message = "No puede superar 255 caracteres") String debtorId,
        DebtorIdType debtorIdType,
        @NotNull(message = "El importe base es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El importe base no puede ser negativo") BigDecimal baseAmount,
        @NotNull(message = "La lista de agravantes es obligatoria")
        List<@NotBlank(message = "Los agravantes no pueden estar vacíos") @Size(max = 255, message = "No puede superar 255 caracteres") String> aggravatingFactors,
        @NotNull(message = "El importe final es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El importe final no puede ser negativo") BigDecimal finalAmount,
        UUID originOperationId,
        @Size(max = 255, message = "No puede superar 255 caracteres") String originTicketId,
        UUID originParkingSessionId) {

    @AssertTrue(message = "debtorId y debtorIdType deben informarse juntos")
    public boolean isDebtorIdentificationComplete() {
        return (debtorId == null || debtorId.isBlank()) == (debtorIdType == null);
    }

    @AssertTrue(message = "El importe final no puede ser menor que el importe base")
    public boolean isFinalAmountValid() {
        return baseAmount == null || finalAmount == null || finalAmount.compareTo(baseAmount) >= 0;
    }
}
