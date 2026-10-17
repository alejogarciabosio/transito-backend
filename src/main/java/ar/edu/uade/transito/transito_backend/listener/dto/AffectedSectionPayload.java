package ar.edu.uade.transito.transito_backend.listener.dto;

import ar.edu.uade.transito.transito_backend.model.enums.AffectedSectionDirection;

public record AffectedSectionPayload(
        String street,
        String from,
        String to,
        AffectedSectionDirection direction) {
}
