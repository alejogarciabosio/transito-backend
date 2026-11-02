package ar.edu.uade.transito.transito_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.jayway.jsonpath.JsonPath;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.JwtService;
import ar.edu.uade.transito.transito_backend.support.Payloads;
import ar.edu.uade.transito.transito_backend.support.TestEventsConfig;
import ar.edu.uade.transito.transito_backend.support.TestUsers;

/** Concurrencia optimista (ETag / If-Match → 412) contra un servidor HTTP real (Tomcat embebido). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestEventsConfig.class)
@DisplayName("REST — ETag, If-Match y 412 sobre servidor real")
class EtagServerTest {

    @LocalServerPort int port;
    @Autowired JwtService jwt;
    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> send(String method, String path, AuthenticatedUser u, String ifMatch, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Authorization", "Bearer " + jwt.generateToken(u)).header("Content-Type", "application/json");
        if (ifMatch != null) {
            b.header("If-Match", ifMatch);
        }
        b.method(method, body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body));
        return http.send(b.build(), BodyHandlers.ofString());
    }

    @Test
    @DisplayName("REST-ETAG-01: If-Match vigente → 200; desactualizado → 412 Problem Details; sin If-Match no cambia nada")
    void optimisticConcurrency() throws Exception {
        var created = send("POST", "/infractions", TestUsers.INSPECTOR, null, Payloads.infraction("30777888"));
        assertThat(created.statusCode()).isEqualTo(201);
        String id = JsonPath.read(created.body(), "$.infractionId");

        String etag = send("GET", "/infractions/" + id, TestUsers.INSPECTOR, null, null).headers().firstValue("ETag").orElse(null);
        assertThat(etag).isNotNull();

        assertThat(send("PATCH", "/infractions/" + id, TestUsers.INSPECTOR, "\"viejo\"", Payloads.infractionUpdate()).statusCode())
                .as("If-Match inválido").isEqualTo(412);
        var ok = send("PATCH", "/infractions/" + id, TestUsers.INSPECTOR, etag, Payloads.infractionUpdate());
        assertThat(ok.statusCode()).as("If-Match vigente: " + ok.body()).isBetween(200, 299);

        var stale = send("PATCH", "/infractions/" + id, TestUsers.INSPECTOR, etag, Payloads.infractionUpdate());
        System.out.println("EVIDENCIA ETAG reuso de ETag tras modificar=" + stale.statusCode());
        assertThat(stale.statusCode()).as("ETag usado dos veces tras cambiar el recurso").isEqualTo(412);
        assertThat(stale.body()).contains("\"status\":412");

        assertThat(send("PATCH", "/infractions/" + id, TestUsers.INSPECTOR, null, Payloads.infractionUpdate()).statusCode())
                .as("sin If-Match el comportamiento previo se conserva").isBetween(200, 299);
    }
}
