package ar.edu.uade.transito.transito_backend.listener.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.JsonNode;

/** Subconjunto de ticketUpdated (M2 v1.78) que consume M7; el resto de los campos se ignora. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketUpdatedPayload(
        UUID ticketId,
        String responsibleAreaId,
        String updateType,
        JsonNode details) {
}
