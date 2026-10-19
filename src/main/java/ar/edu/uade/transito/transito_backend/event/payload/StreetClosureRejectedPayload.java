package ar.edu.uade.transito.transito_backend.event.payload;

public record StreetClosureRejectedPayload(
        String closureRequestId,
        String rejectionReason,
        String requestingModule) {
}
