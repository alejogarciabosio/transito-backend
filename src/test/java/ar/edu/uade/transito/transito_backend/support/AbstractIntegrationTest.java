package ar.edu.uade.transito.transito_backend.support;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.JwtService;
import ar.edu.uade.transito.transito_backend.listener.IncomingEventDispatcher;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingSessionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingTariffRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingZoneRepository;
import ar.edu.uade.transito.transito_backend.repository.RoadAccidentRepository;
import ar.edu.uade.transito.transito_backend.repository.StreetClosureRepository;
import ar.edu.uade.transito.transito_backend.repository.TrafficOperationRepository;
import ar.edu.uade.transito.transito_backend.repository.VehicleImpoundmentRepository;
import com.jayway.jsonpath.JsonPath;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Base de los tests de integración: levanta el contexto completo (seguridad + JPA sobre H2),
 * firma tokens JWT reales con {@link JwtService} y limpia la base y los eventos antes de cada test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEventsConfig.class)
public abstract class AbstractIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected JwtService jwtService;
    @Autowired protected RecordingEventPublisher events;
    @Autowired protected IncomingEventDispatcher incomingEventDispatcher;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected ar.edu.uade.transito.transito_backend.auth.LoginRateLimitFilter loginRateLimitFilter;
    @Autowired protected ar.edu.uade.transito.transito_backend.auth.IdempotencyFilter idempotencyFilter;

    @Autowired protected InfractionRepository infractionRepository;
    @Autowired protected ParkingSessionRepository parkingSessionRepository;
    @Autowired protected ParkingTariffRepository parkingTariffRepository;
    @Autowired protected ParkingZoneRepository parkingZoneRepository;
    @Autowired protected RoadAccidentRepository roadAccidentRepository;
    @Autowired protected StreetClosureRepository streetClosureRepository;
    @Autowired protected TrafficOperationRepository trafficOperationRepository;
    @Autowired protected VehicleImpoundmentRepository vehicleImpoundmentRepository;

    @BeforeEach
    void cleanState() {
        infractionRepository.deleteAll();
        parkingSessionRepository.deleteAll();
        parkingTariffRepository.deleteAll();
        parkingZoneRepository.deleteAll();
        roadAccidentRepository.deleteAll();
        streetClosureRepository.deleteAll();
        trafficOperationRepository.deleteAll();
        vehicleImpoundmentRepository.deleteAll();
        events.clear();
        loginRateLimitFilter.reset();
        idempotencyFilter.reset();
    }

    protected String token(AuthenticatedUser user) {
        return jwtService.generateToken(user);
    }

    /** Prefijo de versión del contrato REST (ver docs/actividad-api-rest-entrega.md §3). */
    protected static final String API_PREFIX = "/api/v1";

    protected ResultActions call(HttpMethod method, String url, AuthenticatedUser user, String jsonBody) throws Exception {
        MockHttpServletRequestBuilder builder = request(method, API_PREFIX + url);
        if (user != null) {
            builder.header("Authorization", "Bearer " + token(user));
        }
        if (jsonBody != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(jsonBody);
        }
        return mockMvc.perform(builder);
    }

    protected ResultActions get(String url, AuthenticatedUser user) throws Exception {
        return call(HttpMethod.GET, url, user, null);
    }

    protected ResultActions post(String url, AuthenticatedUser user, String json) throws Exception {
        return call(HttpMethod.POST, url, user, json);
    }

    protected ResultActions patch(String url, AuthenticatedUser user, String json) throws Exception {
        return call(HttpMethod.PATCH, url, user, json);
    }

    protected ResultActions put(String url, AuthenticatedUser user, String json) throws Exception {
        return call(HttpMethod.PUT, url, user, json);
    }

    protected ResultActions delete(String url, AuthenticatedUser user) throws Exception {
        return call(HttpMethod.DELETE, url, user, null);
    }

    protected static String read(MvcResult result, String jsonPath) throws Exception {
        Object value = JsonPath.read(result.getResponse().getContentAsString(), jsonPath);
        return String.valueOf(value);
    }

    /**
     * Simula la llegada de un evento entrante al despachador, como lo haría el broker real.
     * {@code dataJson} es solo el contenido de "data"; acá se arma el sobre completo.
     */
    protected void dispatchIncoming(String eventType, String dataJson) {
        String envelope = "{\"eventId\":\"" + java.util.UUID.randomUUID() + "\","
                + "\"eventType\":\"" + eventType + "\","
                + "\"occurredAt\":\"" + java.time.OffsetDateTime.now() + "\","
                + "\"sourceModule\":\"test\","
                + "\"data\":" + dataJson + "}";
        incomingEventDispatcher.dispatch(objectMapper.readTree(envelope));
    }
}
