package ar.edu.uade.transito.transito_backend.listener;

import java.util.UUID;

import tools.jackson.databind.JsonNode;

/** Handler variant that needs the Core envelope metadata in addition to data. */
public interface ContextualIncomingEventHandler extends IncomingEventHandler {

    void handle(JsonNode data, UUID eventId, UUID correlationId);
}
