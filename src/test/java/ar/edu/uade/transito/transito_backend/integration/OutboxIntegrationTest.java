package ar.edu.uade.transito.transito_backend.integration;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.JwtService;
import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.event.EventTransport;
import ar.edu.uade.transito.transito_backend.event.OutboxEventPublisher;
import ar.edu.uade.transito.transito_backend.event.OutboxRelay;
import ar.edu.uade.transito.transito_backend.model.OutboxEvent;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.OutboxStatus;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import ar.edu.uade.transito.transito_backend.repository.OutboxEventRepository;
import ar.edu.uade.transito.transito_backend.repository.StreetClosureRepository;
import ar.edu.uade.transito.transito_backend.service.StreetClosureExpirationScheduler;
import ar.edu.uade.transito.transito_backend.service.StreetClosureService;
import ar.edu.uade.transito.transito_backend.support.Payloads;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.ConnectException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.ADMIN;
import static ar.edu.uade.transito.transito_backend.support.TestUsers.AUDITOR;
import static ar.edu.uade.transito.transito_backend.support.TestUsers.CIUDADANO;
import static ar.edu.uade.transito.transito_backend.support.TestUsers.INSPECTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DEF-14 / DEF-04 de punta a punta con el contexto real: los servicios usan el {@link OutboxEventPublisher}
 * (sin el RecordingEventPublisher de los demás tests) y el transporte al broker es un mock que puede fallar.
 */
@SpringBootTest(properties = {"outbox.relay.backoff-base-ms=0", "outbox.relay.max-attempts=3"})
@AutoConfigureMockMvc
class OutboxIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired EventPublisher eventPublisher;
    @Autowired OutboxRelay relay;
    @Autowired OutboxEventRepository outbox;
    @Autowired InfractionRepository infractions;
    @Autowired StreetClosureRepository closures;
    @Autowired StreetClosureService streetClosureService;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean EventTransport transport;

    @BeforeEach
    void limpiar() {
        outbox.deleteAll();
        infractions.deleteAll();
        closures.deleteAll();
        reset(transport);
    }

    private String bearer(AuthenticatedUser user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private String registrarYConfirmar() throws Exception {
        String body = mockMvc.perform(post("/api/v1/infractions").header("Authorization", bearer(INSPECTOR))
                        .contentType("application/json").content(Payloads.infraction("1")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.infractionId");
        mockMvc.perform(post("/api/v1/infractions/" + id + "/confirmation").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isOk());
        return id;
    }

    private StreetClosure corte(String id, StreetClosureStatus status, OffsetDateTime end) {
        return closures.save(StreetClosure.builder().closureRequestId(id).reason("Obra")
                .sourceEventId(UUID.randomUUID()).sourceCorrelationId(UUID.randomUUID()).sourceRef("REF-" + id)
                .requestedAt(end.minusDays(2)).requestingModule(RequestingModule.OBRAS)
                .startDate(end.minusDays(1)).endDate(end).status(status).build());
    }

    private OutboxEvent fila(String eventType) {
        return outbox.findAll().stream().filter(e -> e.getEventType().equals(eventType)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("TC-BE-OUT-10: confirmar guarda el evento PENDING en la misma transacción; el relay lo publica y queda PUBLISHED")
    void confirmarYPublicar() throws Exception {
        registrarYConfirmar();

        OutboxEvent pendiente = fila("infractionConfirmed");
        assertThat(pendiente.getStatus()).isEqualTo(OutboxStatus.PENDING);
        verify(transport, never()).send(any());

        assertThat(relay.relayPending()).isEqualTo(2); // infractionRegistered + infractionConfirmed

        OutboxEvent publicado = fila("infractionConfirmed");
        assertThat(publicado.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(publicado.getAttempts()).isEqualTo(1);
        assertThat(publicado.getPublishedAt()).isNotNull();
        verify(transport).send(argThat(e -> e.getEventId().equals(publicado.getEventId())
                && e.getPayload().contains("\"eventId\":\"" + publicado.getEventId() + "\"")));
        assertThat(relay.relayPending()).isZero(); // no se vuelve a publicar
    }

    @Test
    @DisplayName("TC-BE-OUT-11: broker caído → el evento queda PENDING con el error; al volver el broker se publica")
    void brokerCaidoYVuelve() throws Exception {
        doThrow(new AmqpConnectException(new ConnectException("Connection refused"))).when(transport).send(any());
        registrarYConfirmar();

        assertThat(relay.relayPending()).isZero();
        List<OutboxEvent> filas = outbox.findAll();
        assertThat(filas).hasSize(2).allMatch(e -> e.getStatus() == OutboxStatus.PENDING);
        // Sin conexión, el relay corta el lote al primer fallo: el resto no gasta intentos.
        assertThat(filas.stream().mapToInt(OutboxEvent::getAttempts).sum()).isEqualTo(1);
        assertThat(filas.stream().filter(e -> e.getAttempts() == 1).findFirst().orElseThrow().getLastError())
                .isEqualTo("AmqpConnectException <- ConnectException: Connection refused");

        doNothing().when(transport).send(any());
        assertThat(relay.relayPending()).isEqualTo(2);
        assertThat(outbox.findAll()).allMatch(e -> e.getStatus() == OutboxStatus.PUBLISHED && e.getLastError() == null);
    }

    @Test
    @DisplayName("TC-BE-OUT-12: un evento que falla siempre queda FAILED tras N intentos sin bloquear al resto")
    void failedTrasNIntentosSinBloquear() {
        doAnswer(inv -> {
            OutboxEvent e = inv.getArgument(0);
            if (e.getEventType().equals("roto")) {
                throw new IllegalStateException("rechazado");
            }
            return null;
        }).when(transport).send(any());
        eventPublisher.publish(EventEnvelope.of("roto", Map.of("a", 1)));
        eventPublisher.publish(EventEnvelope.of("sano", Map.of("b", 2)));

        assertThat(relay.relayPending()).isEqualTo(1);
        assertThat(fila("sano").getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        relay.relayPending();
        relay.relayPending();
        OutboxEvent roto = fila("roto");
        assertThat(roto.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(roto.getAttempts()).isEqualTo(3);
        assertThat(roto.getLastError()).isEqualTo("IllegalStateException: rechazado");
        relay.relayPending();
        assertThat(fila("roto").getAttempts()).isEqualTo(3); // FAILED no se reintenta solo
    }

    @Test
    @DisplayName("TC-BE-OUT-13: si la transacción hace rollback no queda evento en el outbox ni se publica")
    void rollbackNoDejaEvento() {
        StreetClosure vencido = corte("OBR-1", StreetClosureStatus.ACTIVE, OffsetDateTime.now().minusHours(1));

        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            streetClosureService.expireClosure(vencido.getStreetClosureId(), OffsetDateTime.now());
            eventPublisher.publish(EventEnvelope.of("otro", Map.of()));
            tx.setRollbackOnly();
        });

        assertThat(outbox.count()).isZero();
        assertThat(closures.findById(vencido.getStreetClosureId()).orElseThrow().getStatus())
                .isEqualTo(StreetClosureStatus.ACTIVE);
        assertThat(relay.relayPending()).isZero();
        verify(transport, never()).send(any());
    }

    @Test
    @DisplayName("TC-BE-OUT-14: el scheduler vence cada corte en su transacción y deja streetClosureEnded en el outbox")
    void schedulerUsaOutbox() {
        StreetClosure activo = corte("OBR-2", StreetClosureStatus.ACTIVE, OffsetDateTime.now().minusMinutes(5));
        StreetClosure aprobado = corte("OBR-3", StreetClosureStatus.APPROVED, OffsetDateTime.now().minusMinutes(5));
        StreetClosure futuro = corte("OBR-4", StreetClosureStatus.ACTIVE, OffsetDateTime.now().plusHours(2));

        new StreetClosureExpirationScheduler(streetClosureService).expireDueClosures();

        assertThat(closures.findById(activo.getStreetClosureId()).orElseThrow().getStatus()).isEqualTo(StreetClosureStatus.FINISHED);
        assertThat(closures.findById(aprobado.getStreetClosureId()).orElseThrow().getStatus()).isEqualTo(StreetClosureStatus.EXPIRED);
        assertThat(closures.findById(futuro.getStreetClosureId()).orElseThrow().getStatus()).isEqualTo(StreetClosureStatus.ACTIVE);
        assertThat(outbox.findAll()).singleElement()
                .satisfies(e -> {
                    assertThat(e.getEventType()).isEqualTo("streetClosureEnded");
                    assertThat(e.getStatus()).isEqualTo(OutboxStatus.PENDING);
                });
        assertThat(streetClosureService.expireClosure(futuro.getStreetClosureId(), OffsetDateTime.now())).isFalse();
    }

    @Test
    @DisplayName("TC-BE-OUT-15: varios relays en paralelo no publican dos veces el mismo evento")
    void sinDuplicadosEnParalelo() throws Exception {
        for (int i = 0; i < 30; i++) {
            eventPublisher.publish(EventEnvelope.of("paralelo", Map.of("n", i)));
        }
        List<UUID> enviados = new CopyOnWriteArrayList<>();
        doAnswer(inv -> {
            enviados.add(((OutboxEvent) inv.getArgument(0)).getEventId());
            Thread.sleep(2);
            return null;
        }).when(transport).send(any());

        int relays = 4;
        ExecutorService pool = Executors.newFixedThreadPool(relays);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < relays; i++) {
            resultados.add(pool.submit(() -> {
                largada.await();
                return relay.relayPending();
            }));
        }
        largada.countDown();
        int total = 0;
        for (Future<Integer> r : resultados) {
            total += r.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(enviados).hasSize(30).doesNotHaveDuplicates();
        assertThat(total).isEqualTo(30);
        assertThat(outbox.countByStatus(OutboxStatus.PUBLISHED)).isEqualTo(30);
    }

    @Test
    @DisplayName("TC-BE-OUT-16: GET /admin/outbox lista el registro (sin payload) para ADMINISTRADOR/AUDITOR")
    void endpointRegistro() throws Exception {
        registrarYConfirmar();
        relay.relayPending();
        eventPublisher.publish(EventEnvelope.of("pendiente", Map.of()));

        mockMvc.perform(get("/api/v1/admin/outbox").param("status", "PUBLISHED").header("Authorization", bearer(AUDITOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$[0].payload").doesNotExist());
        mockMvc.perform(get("/api/v1/admin/outbox").param("limit", "1").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventType").value("pendiente"));
        mockMvc.perform(get("/api/v1/admin/outbox").header("Authorization", bearer(CIUDADANO)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/outbox").header("Authorization", bearer(INSPECTOR)))
                .andExpect(status().isForbidden());
    }
}
