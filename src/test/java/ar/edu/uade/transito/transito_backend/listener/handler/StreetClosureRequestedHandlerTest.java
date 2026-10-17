package ar.edu.uade.transito.transito_backend.listener.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.AffectedSectionDirection;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.service.StreetClosureService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;

class StreetClosureRequestedHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapsCompletePayloadToStreetClosure() {
        StreetClosureService service = mock(StreetClosureService.class);
        StreetClosureRequestedHandler handler = new StreetClosureRequestedHandler(mapper, service);

        JsonNode data = mapper.readTree("""
                {
                  "closureRequestId": "REQ-M3-501",
                  "previousClosureRequestId": "REQ-M3-500",
                  "sourceModule": "M3",
                  "sourceRef": "WO-1050",
                  "reason": "Reparación de calzada",
                  "affectedSections": [{
                    "street": "Av. Cabildo",
                    "from": "Juramento",
                    "to": "Mendoza",
                    "direction": "BOTH"
                  }],
                  "requestedFrom": "2026-09-04T08:00:00-03:00",
                  "requestedTo": "2026-09-04T18:00:00-03:00",
                  "closureType": "TOTAL",
                  "requestedAt": "2026-09-01T16:58:00-03:00"
                }
                """);

        handler.handle(data);

        ArgumentCaptor<StreetClosure> closureCaptor = ArgumentCaptor.forClass(StreetClosure.class);
        verify(service).createFromRequest(closureCaptor.capture());

        StreetClosure closure = closureCaptor.getValue();
        assertEquals(
                OffsetDateTime.parse("2026-09-04T08:00:00-03:00").toInstant(),
                closure.getStartDate().toInstant());
        assertEquals(
                OffsetDateTime.parse("2026-09-04T18:00:00-03:00").toInstant(),
                closure.getEndDate().toInstant());
        assertEquals(RequestingModule.OBRAS, closure.getRequestingModule());
        assertEquals("REQ-M3-501", closure.getClosureRequestId());
        assertEquals("REQ-M3-500", closure.getPreviousClosureRequestId());
        assertEquals("WO-1050", closure.getSourceRef());
        assertEquals("Reparación de calzada", closure.getReason());
        assertEquals("TOTAL", closure.getClosureType());
        assertEquals(
                OffsetDateTime.parse("2026-09-01T16:58:00-03:00").toInstant(),
                closure.getRequestedAt().toInstant());
        assertEquals(List.of("Av. Cabildo"), closure.getAffectedStreet());
        assertEquals(1, closure.getAffectedSections().size());
        assertEquals("Av. Cabildo", closure.getAffectedSections().get(0).getStreet());
        assertEquals("Juramento", closure.getAffectedSections().get(0).getFrom());
        assertEquals("Mendoza", closure.getAffectedSections().get(0).getTo());
        assertEquals(AffectedSectionDirection.BOTH, closure.getAffectedSections().get(0).getDirection());
    }

    @Test
    void rejectsUnknownAffectedSectionDirection() {
        StreetClosureService service = mock(StreetClosureService.class);
        StreetClosureRequestedHandler handler = new StreetClosureRequestedHandler(mapper, service);

        JsonNode data = mapper.readTree("""
                {
                  "closureRequestId": "REQ-M3-502",
                  "sourceModule": "M3",
                  "affectedSections": [{
                    "street": "Av. Cabildo",
                    "from": "Juramento",
                    "to": "Mendoza",
                    "direction": "SIDEWAYS"
                  }],
                  "requestedFrom": "2026-09-04T08:00:00-03:00",
                  "requestedTo": "2026-09-04T18:00:00-03:00",
                  "requestedAt": "2026-09-01T16:58:00-03:00"
                }
                """);

        assertThrows(InvalidFormatException.class, () -> handler.handle(data));
        verifyNoInteractions(service);
    }
}
