package ar.edu.uade.transito.transito_backend.listener.dto;

import tools.jackson.databind.JsonNode;

public record TicketUpdatedPayload(
        Integer ticketId,
        String responsibleAreaId,
        String updateType,
        JsonNode details) {
}
