package ar.edu.uade.transito.transito_backend.event.payload;

import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;

public record StreetClosureRejectedPayload(
        String closureRequestId,
        String rejectionReason,
        RequestingModule requestingModule) {
}
