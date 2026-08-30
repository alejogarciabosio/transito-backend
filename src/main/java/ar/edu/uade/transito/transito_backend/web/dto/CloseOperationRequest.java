package ar.edu.uade.transito.transito_backend.web.dto;

import ar.edu.uade.transito.transito_backend.model.enums.OperationResolution;

public record CloseOperationRequest(OperationResolution resolution) {
}
