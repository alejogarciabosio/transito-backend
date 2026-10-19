package ar.edu.uade.transito.transito_backend.web.dto;

import ar.edu.uade.transito.transito_backend.model.enums.OperationResolution;
import jakarta.validation.constraints.NotNull;

public record CloseOperationRequest(
        @NotNull(message = "La resolución del operativo es obligatoria") OperationResolution resolution) {
}
