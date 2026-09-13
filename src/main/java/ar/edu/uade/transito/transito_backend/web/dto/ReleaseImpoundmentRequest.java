package ar.edu.uade.transito.transito_backend.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReleaseImpoundmentRequest(
        List<String> verifiedRequirements,
        BigDecimal towingAndStorageFee,
        String paymentReference) {
}
