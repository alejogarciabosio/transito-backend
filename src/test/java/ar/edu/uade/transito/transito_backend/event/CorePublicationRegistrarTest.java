package ar.edu.uade.transito.transito_backend.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.ObjectMapper;

class CorePublicationRegistrarTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void registersEveryEventWithTheCachedModuleToken() throws Exception {
        List<String> publicationBodies = new ArrayList<>();
        AtomicInteger tokenRequests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/auth/module-token", exchange -> {
            tokenRequests.incrementAndGet();
            byte[] response = "{\"accessToken\":\"module-token\",\"expiresIn\":900}".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/api/v1/publications", exchange -> {
            publicationBodies.add(new String(exchange.getRequestBody().readAllBytes()));
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer module-token");
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        ObjectMapper mapper = new ObjectMapper();
        CoreTokenClient tokenClient = new CoreTokenClient(baseUrl, "secret", mapper);
        CorePublicationRegistrar registrar = new CorePublicationRegistrar(tokenClient, mapper,
                java.net.URI.create(baseUrl + "/api/v1/publications"), HttpClient.newHttpClient());

        registrar.registerAll();

        assertThat(tokenRequests).hasValue(1);
        assertThat(publicationBodies).hasSize(CorePublishedEventTypes.ALL.size());
        assertThat(publicationBodies).contains("{\"eventType\":\"infractionConfirmed\"}");
        assertThat(publicationBodies).doesNotContain("{\"eventType\":\"infractionAnulled\"}");
    }
}
