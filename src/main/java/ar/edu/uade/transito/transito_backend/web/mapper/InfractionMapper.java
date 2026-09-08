package ar.edu.uade.transito.transito_backend.web.mapper;

import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.web.dto.CreateInfractionRequest;
import ar.edu.uade.transito.transito_backend.web.dto.InfractionResponse;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateInfractionRequest;

public final class InfractionMapper {

    private InfractionMapper() {
    }

    public static Infraction toEntity(CreateInfractionRequest request) {
        return Infraction.builder()
                .licensePlate(request.licensePlate())
                .infractionType(request.infractionType())
                .infractionDateTime(request.infractionDateTime())
                .location(request.location())
                .evidenceUrl(request.evidenceUrl())
                .inspectorId(request.inspectorId())
                .offenderNationalId(request.offenderNationalId())
                .vehicleOwnerTaxId(request.vehicleOwnerTaxId())
                .debtorId(request.debtorId())
                .debtorIdType(request.debtorIdType())
                .baseAmount(request.baseAmount())
                .aggravatingFactors(request.aggravatingFactors())
                .finalAmount(request.finalAmount())
                .originOperationId(request.originOperationId())
                .originTicketId(request.originTicketId())
                .originParkingSessionId(request.originParkingSessionId())
                .build();
    }

    public static Infraction toEntity(UpdateInfractionRequest request) {
        return Infraction.builder()
                .licensePlate(request.licensePlate())
                .location(request.location())
                .evidenceUrl(request.evidenceUrl())
                .offenderNationalId(request.offenderNationalId())
                .vehicleOwnerTaxId(request.vehicleOwnerTaxId())
                .debtorId(request.debtorId())
                .debtorIdType(request.debtorIdType())
                .baseAmount(request.baseAmount())
                .aggravatingFactors(request.aggravatingFactors())
                .finalAmount(request.finalAmount())
                .build();
    }

    public static InfractionResponse toResponse(Infraction infraction) {
        return new InfractionResponse(
                infraction.getInfractionId(),
                infraction.getLicensePlate(),
                infraction.getInfractionType(),
                infraction.getInfractionDateTime(),
                infraction.getLocation(),
                infraction.getEvidenceUrl(),
                infraction.getInspectorId(),
                infraction.getOffenderNationalId(),
                infraction.getVehicleOwnerTaxId(),
                infraction.getDebtorId(),
                infraction.getDebtorIdType(),
                infraction.getBaseAmount(),
                infraction.getAggravatingFactors(),
                infraction.getFinalAmount(),
                infraction.getStatus(),
                infraction.getOriginOperationId(),
                infraction.getOriginTicketId(),
                infraction.getOriginParkingSessionId());
    }
}
