package ar.edu.uade.transito.transito_backend.auth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Limita los intentos fallidos de login (defensa contra fuerza bruta, OWASP API2 / SEC-05).
 * Lleva dos contadores independientes: por IP del cliente y por identidad (username/gmail).
 * Si cualquiera supera {@code maxAttempts} fallos dentro de la ventana responde 429 con Problem Details
 * y {@code Retry-After}. Un login correcto limpia ambos contadores. Estado en memoria: suficiente para una instancia.
 *
 * <p>IP del cliente (KI-2): detrás de {@code trustedProxies} proxies que AGREGAN la IP al final de
 * {@code X-Forwarded-For} (Railway), la IP real es la N-ésima entrada contando desde la derecha;
 * lo que esté más a la izquierda lo controla el cliente y se ignora. Sin header se usa {@code getRemoteAddr()}.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxAttempts;
    private final int maxAttemptsPerUsername;
    private final long windowMillis;
    private final int trustedProxies;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final Map<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(@Value("${login.rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${login.rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${login.rate-limit.max-attempts-per-username:${login.rate-limit.max-attempts:10}}") int maxAttemptsPerUsername,
            @Value("${login.rate-limit.trusted-proxies:1}") int trustedProxies) {
        this.maxAttempts = maxAttempts;
        this.maxAttemptsPerUsername = maxAttemptsPerUsername;
        this.windowMillis = windowSeconds * 1000;
        this.trustedProxies = Math.max(0, trustedProxies);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && LOGIN_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        CachedBodyRequest cached = new CachedBodyRequest(request);
        long now = System.currentTimeMillis();
        pruneIfNeeded(now);

        Deque<Long> byIp = failures.computeIfAbsent("ip:" + clientIp(request), k -> new ArrayDeque<>());
        String identity = identity(cached.body);
        Deque<Long> byUser = identity == null ? null
                : failures.computeIfAbsent("user:" + identity, k -> new ArrayDeque<>());

        long retryAfter = Math.max(retryAfter(byIp, maxAttempts, now), retryAfter(byUser, maxAttemptsPerUsername, now));
        if (retryAfter > 0) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                    {"type":"https://transito.uade.edu.ar/problems/too-many-requests","title":"Too Many Requests","status":429,"detail":"Demasiados intentos de inicio de sesión. Reintentá en %d segundos.","instance":"%s"}
                    """.formatted(retryAfter, LOGIN_PATH));
            return;
        }
        chain.doFilter(cached, response);
        int status = response.getStatus();
        for (Deque<Long> recent : byUser == null ? List.of(byIp) : List.of(byIp, byUser)) {
            synchronized (recent) {
                if (status == 401 || status == 400) {
                    recent.addLast(System.currentTimeMillis());
                } else if (status < 300) {
                    recent.clear();
                }
            }
        }
    }

    private long retryAfter(Deque<Long> recent, int limit, long now) {
        if (recent == null) {
            return 0;
        }
        synchronized (recent) {
            while (!recent.isEmpty() && now - recent.peekFirst() > windowMillis) {
                recent.pollFirst();
            }
            if (recent.size() >= limit) {
                return Math.max(1, (windowMillis - (now - recent.peekFirst())) / 1000);
            }
            return 0;
        }
    }

    /** Evita crecimiento ilimitado del mapa si un atacante rota IPs o usernames. */
    private void pruneIfNeeded(long now) {
        if (failures.size() < MAX_TRACKED_KEYS) {
            return;
        }
        failures.entrySet().removeIf(e -> {
            Deque<Long> d = e.getValue();
            synchronized (d) {
                return d.isEmpty() || now - d.peekLast() > windowMillis;
            }
        });
    }

    /** IP del cliente según la cantidad de proxies de confianza (ver javadoc de la clase). */
    String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (trustedProxies == 0 || forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }
        String[] parts = forwarded.split(",", -1);
        int index = Math.max(0, parts.length - trustedProxies);
        String ip = parts[index].trim();
        return ip.isEmpty() ? request.getRemoteAddr() : ip;
    }

    /** Misma identidad que usa AuthController: gmail si viene, si no username. Normalizada. */
    private String identity(byte[] body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node == null || !node.isObject()) {
                return null;
            }
            String gmail = text(node, "gmail");
            String value = gmail != null ? gmail : text(node, "username");
            return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isString() && !value.asString().isBlank() ? value.asString() : null;
    }

    /** Solo para tests: limpia el estado. */
    public void reset() {
        failures.clear();
    }

    /** Permite leer el body en el filtro y volver a entregarlo intacto al controlador. */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return in.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return in.read(b, off, len);
                }

                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(getInputStream(),
                    encoding != null ? java.nio.charset.Charset.forName(encoding) : StandardCharsets.UTF_8));
        }
    }
}
