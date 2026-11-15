package ar.edu.uade.transito.transito_backend.web.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReleaseImpoundmentRequest(
        @NotEmpty(message = "Debe verificarse al menos un requisito")
        List<@NotBlank(message = "Los requisitos verificados no pueden estar vacíos") @Size(max = 255, message = "No puede superar 255 caracteres") String> verifiedRequirements,
        @NotNull(message = "El costo de acarreo y estadía es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El costo no puede ser negativo")
        BigDecimal towingAndStorageFee,
        @Size(max = 255, message = "No puede superar 255 caracteres") String paymentReference) {

    @AssertTrue(message = "La referencia de pago es obligatoria cuando el costo es mayor que cero")
    public boolean isPaymentReferenceValid() {
        return towingAndStorageFee == null
                || towingAndStorageFee.signum() <= 0
                || (paymentReference != null && !paymentReference.isBlank());
    }
}
