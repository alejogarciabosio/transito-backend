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
                  "sourceModule": "obras",
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
                    && List.of("Av. Cabildo entre Juramento y Mendoza (BOTH)").equals(closure.getAffectedStreet())
                    && closure.getPreviousClosureRequestId() == null
                    && closure.getAffectedSections().size() == 1
                    && "Av. Cabildo".equals(closure.getAffectedSections().get(0).getStreet())
                    && "Juramento".equals(closure.getAffectedSections().get(0).getFrom())
                    && "Mendoza".equals(closure.getAffectedSections().get(0).getTo())
                    && "BOTH".equals(closure.getAffectedSections().get(0).getDirection());
        }));
    }

    @Test
    void keepsTheReferenceToThePreviousRequestForAnExtension() {
        StreetClosureService service = mock(StreetClosureService.class);
        StreetClosureRequestedHandler handler = new StreetClosureRequestedHandler(mapper, service);
        JsonNode data = mapper.readTree("""
                {
                  "closureRequestId": "REQ-M3-502",
                  "previousClosureRequestId": "REQ-M3-501",
                  "sourceModule": "obras",
                  "sourceRef": "WO-1050",
                  "reason": "Prórroga de obra",
                  "affectedSections": [{"street":"Av. Cabildo","from":"Juramento","to":"Mendoza","direction":"FORWARD"}],
                  "requestedFrom": "2026-09-04T18:00:00-03:00",
                  "requestedTo": "2026-09-05T02:00:00-03:00",
                  "requestedAt": "2026-09-04T16:58:00-03:00"
                }
                """);

        handler.handle(data);

        verify(service).createFromRequest(argThat(closure ->
                "REQ-M3-501".equals(closure.getPreviousClosureRequestId())
                        && "FORWARD".equals(closure.getAffectedSections().get(0).getDirection())));
    }

    @Test
    void rejectsRequestsWithoutTheRequiredWorksPayloadFields() {
        StreetClosureService service = mock(StreetClosureService.class);
        StreetClosureRequestedHandler handler = new StreetClosureRequestedHandler(mapper, service);
        JsonNode data = mapper.readTree("""
                {"closureRequestId":"REQ-M3-503","sourceModule":"obras","requestedFrom":"2026-09-04T08:00:00-03:00","requestedTo":"2026-09-04T18:00:00-03:00","requestedAt":"2026-09-01T16:58:00-03:00"}
                """);

        assertThrows(IllegalArgumentException.class, () -> handler.handle(data));
    }
}
