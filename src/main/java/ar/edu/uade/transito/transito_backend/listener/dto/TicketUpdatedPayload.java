package ar.edu.uade.transito.transito_backend.listener.dto;

import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record TicketUpdatedPayload(
        UUID ticketId,
        String responsibleAreaId,
        String updateType,
        JsonNode details) {
}
