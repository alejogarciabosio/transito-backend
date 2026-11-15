package ar.edu.uade.transito.transito_backend.event.payload;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Payload M2 v1.78 inside the standard Core envelope. */
public record UpdateTicketStatusPayload(
        UUID ticketId,
        String updateType,
        String publicMessage,
        String internalMessage,
        Integer progress,
        Object details,
        List<Object> attachments,
        UpdatedBy updatedBy,
        OffsetDateTime updateOccurredAt) {

    public record UpdatedBy(String type, UUID id) {
    }

    public static UpdateTicketStatusPayload started(UUID ticketId) {
        return status(ticketId, "STARTED", "El área de Tránsito inició la verificación del reclamo.", 0, null);
    }

    /**
     * RESOLVED según el esquema de Core v1.78: {@code publicMessage} no vacío y
     * {@code details = {"resolution": {"type": <tipo>}}} sin otros campos.
     */
    public static UpdateTicketStatusPayload resolved(UUID ticketId, String publicMessage,
                                                      String internalMessage, String resolutionType) {
        return status(ticketId, "RESOLVED", publicMessage, internalMessage, 100,
                Map.of("resolution", Map.of("type", resolutionType)));
    }

    private static UpdateTicketStatusPayload status(UUID ticketId, String updateType, String publicMessage,
                                                    Integer progress, Object details) {
        return status(ticketId, updateType, publicMessage, null, progress, details);
    }

    private static UpdateTicketStatusPayload status(UUID ticketId, String updateType, String publicMessage,
                                                    String internalMessage, Integer progress, Object details) {
        return new UpdateTicketStatusPayload(ticketId, updateType, publicMessage, internalMessage, progress,
                details, List.of(), new UpdatedBy("SYSTEM", null), OffsetDateTime.now());
    }
}
