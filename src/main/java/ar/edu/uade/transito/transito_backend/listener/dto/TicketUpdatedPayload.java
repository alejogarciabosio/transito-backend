package ar.edu.uade.transito.transito_backend.listener.dto;

// Simplificado a propósito: el contrato real de ticketUpdated trae muchos más campos anidados
// (details.routing.location, formData, etc.) -- se completa cuando se conecte un broker de verdad.
public record TicketUpdatedPayload(
        Integer ticketId,
        String responsibleAreaId,
        String updateType) {
}
