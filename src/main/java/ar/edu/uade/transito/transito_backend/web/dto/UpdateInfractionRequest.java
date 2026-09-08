package ar.edu.uade.transito.transito_backend.web.dto;

import java.math.BigDecimal;
import java.util.List;

import ar.edu.uade.transito.transito_backend.model.Location;

public record UpdateInfractionRequest(
        String licensePlate,
        Location location,
        List<String> evidenceUrl,
        String offenderNationalId,
        String vehicleOwnerTaxId,
        String debtorId,
        String debtorIdType,
        BigDecimal baseAmount,
        List<String> aggravatingFactors,
        BigDecimal finalAmount) {
}
