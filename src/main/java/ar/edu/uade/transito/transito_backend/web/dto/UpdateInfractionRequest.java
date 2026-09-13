package ar.edu.uade.transito.transito_backend.web.dto;

import java.math.BigDecimal;
import java.util.List;

import ar.edu.uade.transito.transito_backend.model.Location;
import ar.edu.uade.transito.transito_backend.model.enums.DebtorIdType;

public record UpdateInfractionRequest(
        String licensePlate,
        Location location,
        List<String> evidenceUrl,
        String offenderNationalId,
        String vehicleOwnerTaxId,
        String debtorId,
        DebtorIdType debtorIdType,
        BigDecimal baseAmount,
        List<String> aggravatingFactors,
        BigDecimal finalAmount) {
}
