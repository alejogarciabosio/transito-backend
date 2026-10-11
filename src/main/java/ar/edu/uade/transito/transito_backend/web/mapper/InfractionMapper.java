package ar.edu.uade.transito.transito_backend.web.mapper;

import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.web.dto.CreateInfractionRequest;
import ar.edu.uade.transito.transito_backend.web.dto.InfractionResponse;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateInfractionRequest;

public final class InfractionMapper {

    private InfractionMapper() {
    }

    //Este método sirve para mapear un CreateInfractionRequest a una entidad Infraction. Se utiliza cuando se crea una nueva infracción a partir de los datos proporcionados en la solicitud.
    public static Infraction toEntity(CreateInfractionRequest request) {
        return Infraction.builder()
                .licensePlate(request.licensePlate())
                .infractionType(request.infractionType())
                .infractionDateTime(request.infractionDateTime())
                .location(request.location())
                .evidenceUrl(request.evidenceUrl())
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

    //Este método sirve para mapear un UpdateInfractionRequest a una entidad Infraction. Se utiliza cuando se actualiza una infracción existente.
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

    //Este método sirve para mapear una entidad Infraction a una respuesta InfractionResponse. Se utiliza cuando se devuelve información sobre una infracción.
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
