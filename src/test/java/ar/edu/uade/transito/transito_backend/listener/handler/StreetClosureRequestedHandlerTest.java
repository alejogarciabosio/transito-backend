package ar.edu.uade.transito.transito_backend.listener.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.service.StreetClosureService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class StreetClosureRequestedHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapsCompletePayloadToStreetClosure() {
        StreetClosureService service = mock(StreetClosureService.class);
        StreetClosureRequestedHandler handler = new StreetClosureRequestedHandler(mapper, service);

        JsonNode data = mapper.readTree("""
                {
                  "closureRequestId": "REQ-M3-501",
                  "sourceModule": "M3",
                  "sourceRef": "WO-1050",
                  "reason": "Reparación de calzada",
                  "affectedSections": ["Av. Cabildo entre Juramento y Mendoza"],
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
        assertEquals("WO-1050", closure.getSourceRef());
        assertEquals("Reparación de calzada", closure.getReason());
        assertEquals("TOTAL", closure.getClosureType());
        assertEquals(
                OffsetDateTime.parse("2026-09-01T16:58:00-03:00").toInstant(),
                closure.getRequestedAt().toInstant());
        assertEquals(List.of("Av. Cabildo entre Juramento y Mendoza"), closure.getAffectedStreet());
    }
}
