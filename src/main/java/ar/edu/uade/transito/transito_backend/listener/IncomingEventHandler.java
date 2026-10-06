package ar.edu.uade.transito.transito_backend.listener;

import tools.jackson.databind.JsonNode;

public interface IncomingEventHandler {

    String eventType();

    void handle(JsonNode data);
}
