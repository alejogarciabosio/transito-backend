package ar.edu.uade.transito.transito_backend.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Contract envelope agreed with M2 for ticket status updates (M2 v1.78). */
public record TicketStatusEnvelope(
        String specVersion,
        UUID eventId,
        String eventType,
        OffsetDateTime occurredAt,
        Producer producer,
        String subject,
        Data data) {

    public static TicketStatusEnvelope started(UUID ticketId) {
        return status(ticketId, "STARTED", "El área de Tránsito inició la verificación del reclamo.", 0, null);
    }

    public static TicketStatusEnvelope completed(UUID ticketId, String updateType, String publicMessage, Object details) {
        return status(ticketId, updateType, publicMessage, 100, details);
    }

    private static TicketStatusEnvelope status(UUID ticketId, String updateType, String publicMessage,
                                               Integer progress, Object details) {
        OffsetDateTime now = OffsetDateTime.now();
        return new TicketStatusEnvelope(
                "1.0",
                UUID.randomUUID(),
                "updateTicketStatus",
                now,
                new Producer("M7", "transito-api"),
                "tickets/" + ticketId,
                new Data(ticketId, updateType, publicMessage,
                        null, progress, details, List.of(), new UpdatedBy("AREA", "M7"), now));
    }

    public record Producer(String moduleId, String service) {
    }

    public record UpdatedBy(String type, String id) {
    }

    public record Data(
            UUID ticketId,
            String updateType,
            String publicMessage,
            String internalMessage,
            Integer progress,
            Object details,
            List<Object> attachments,
            UpdatedBy updatedBy,
            OffsetDateTime statusChangedAt) {
    }
}
