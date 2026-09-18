package ar.edu.uade.transito.transito_backend.listener.dto;

import java.time.LocalDate;

public record StreetClosureRequestedPayload(
        String closureRequestId,
        String sourceModule,
        LocalDate requestedFrom,
        LocalDate requestedTo) {
}
