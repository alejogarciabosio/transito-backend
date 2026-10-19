package ar.edu.uade.transito.transito_backend.listener.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.service.StreetClosureService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class StreetClosureRequestedHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void keepsTimeAndZoneOfRequestedWindow() {
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

        verify(service).createFromRequest(argThat(closure -> {
            OffsetDateTime expectedFrom = OffsetDateTime.parse("2026-09-04T08:00:00-03:00");
            OffsetDateTime expectedTo = OffsetDateTime.parse("2026-09-04T18:00:00-03:00");
            return closure.getStartDate().isEqual(expectedFrom)
                    && closure.getEndDate().isEqual(expectedTo)
                    && closure.getRequestingModule() == RequestingModule.OBRAS
                    && "REQ-M3-501".equals(closure.getClosureRequestId())
                    && "WO-1050".equals(closure.getSourceRef())
                    && "Reparación de calzada".equals(closure.getReason())
                    && "TOTAL".equals(closure.getClosureType())
                    && closure.getRequestedAt().isEqual(OffsetDateTime.parse("2026-09-01T16:58:00-03:00"))
                    && List.of("Av. Cabildo entre Juramento y Mendoza").equals(closure.getAffectedStreet());
        }));
    }

    @Test
    void rejectsRequestsWithoutTheRequiredWorksPayloadFields() {
        StreetClosureService service = mock(StreetClosureService.class);
        StreetClosureRequestedHandler handler = new StreetClosureRequestedHandler(mapper, service);
        JsonNode data = mapper.readTree("""
                {"closureRequestId":"REQ-M3-502","sourceModule":"M3","requestedFrom":"2026-09-04T08:00:00-03:00","requestedTo":"2026-09-04T18:00:00-03:00","requestedAt":"2026-09-01T16:58:00-03:00"}
                """);

        assertThrows(IllegalArgumentException.class, () -> handler.handle(data));
    }
}
