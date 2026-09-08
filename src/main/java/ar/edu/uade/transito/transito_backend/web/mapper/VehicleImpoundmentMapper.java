package ar.edu.uade.transito.transito_backend.web.mapper;

import ar.edu.uade.transito.transito_backend.model.VehicleImpoundment;
import ar.edu.uade.transito.transito_backend.web.dto.CreateVehicleImpoundmentRequest;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateVehicleImpoundmentRequest;
import ar.edu.uade.transito.transito_backend.web.dto.VehicleImpoundmentResponse;

public final class VehicleImpoundmentMapper {

    private VehicleImpoundmentMapper() {
    }

    public static VehicleImpoundment toEntity(CreateVehicleImpoundmentRequest request) {
        return VehicleImpoundment.builder()
                .licensePlate(request.licensePlate())
                .ownerNationalId(request.ownerNationalId())
                .reason(request.reason())
                .depotLocation(request.depotLocation())
                .entryDateTime(request.entryDateTime())
                .originType(request.originType())
                .originId(request.originId())
                .build();
    }

    public static VehicleImpoundment toEntity(UpdateVehicleImpoundmentRequest request) {
        return VehicleImpoundment.builder()
                .reason(request.reason())
                .depotLocation(request.depotLocation())
                .ownerNationalId(request.ownerNationalId())
                .build();
    }

    public static VehicleImpoundmentResponse toResponse(VehicleImpoundment impoundment) {
        return new VehicleImpoundmentResponse(
                impoundment.getImpoundmentId(),
                impoundment.getLicensePlate(),
                impoundment.getOwnerNationalId(),
                impoundment.getReason(),
                impoundment.getDepotLocation(),
                impoundment.getEntryDateTime(),
                impoundment.getOriginType(),
                impoundment.getOriginId(),
                impoundment.getStatus(),
                impoundment.getPaymentReference());
    }
}
