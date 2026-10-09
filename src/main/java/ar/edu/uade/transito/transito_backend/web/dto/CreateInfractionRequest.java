package ar.edu.uade.transito.transito_backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.transito.transito_backend.model.Location;
import ar.edu.uade.transito.transito_backend.model.enums.DebtorIdType;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;

public record CreateInfractionRequest(
        String licensePlate,
        InfractionType infractionType,
        LocalDateTime infractionDateTime,
        Location location,
        List<String> evidenceUrl,
        String offenderNationalId,
        String vehicleOwnerTaxId,
        String debtorId,
        DebtorIdType debtorIdType,
        BigDecimal baseAmount,
        List<String> aggravatingFactors,
        BigDecimal finalAmount,
        UUID originOperationId,
        String originTicketId,
        UUID originParkingSessionId) {
}
