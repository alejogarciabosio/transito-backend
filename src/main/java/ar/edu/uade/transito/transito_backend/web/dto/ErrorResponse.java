package ar.edu.uade.transito.transito_backend.web.dto;

import java.time.OffsetDateTime;

public record ErrorResponse(OffsetDateTime timestamp, int status, String error, String message) {
}
