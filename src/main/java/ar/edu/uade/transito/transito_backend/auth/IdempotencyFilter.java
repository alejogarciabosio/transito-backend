package ar.edu.uade.transito.transito_backend.auth;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Idempotencia de POST: si el cliente repite un POST con la misma cabecera {@code Idempotency-Key}
 * (mismo usuario y misma ruta), no se vuelve a ejecutar: se devuelve la respuesta original (2xx) con
 * {@code Idempotent-Replay: true}. Sin la cabecera no cambia nada. Estado en memoria con vencimiento de 24 h.
 */
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Duration TTL = Duration.ofHours(24);
    private static final int MAX_ENTRIES = 10_000;

    private record Cached(int status, String contentType, String location, byte[] body, long at) {
    }

    private final Map<String, Cached> store = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || request.getHeader("Idempotency-Key") == null
                || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String who = auth == null ? "anon" : auth.getName();
        String key = who + "|" + request.getRequestURI() + "|" + request.getHeader("Idempotency-Key");
        long now = System.currentTimeMillis();
        Cached hit = store.get(key);
        if (hit != null && now - hit.at() < TTL.toMillis()) {
            response.setStatus(hit.status());
            if (hit.contentType() != null) {
                response.setContentType(hit.contentType());
            }
            if (hit.location() != null) {
                response.setHeader("Location", hit.location());
            }
            response.setHeader("Idempotent-Replay", "true");
            response.getOutputStream().write(hit.body());
            return;
        }
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrapper);
        } finally {
            if (wrapper.getStatus() >= 200 && wrapper.getStatus() < 300) {
                if (store.size() >= MAX_ENTRIES) {
                    store.entrySet().removeIf(e -> now - e.getValue().at() >= TTL.toMillis());
                }
                if (store.size() < MAX_ENTRIES) {
                    store.put(key, new Cached(wrapper.getStatus(), wrapper.getContentType(),
                            wrapper.getHeader("Location"), wrapper.getContentAsByteArray(), now));
                }
            }
            wrapper.copyBodyToResponse();
        }
    }

    /** Solo para tests. */
    public void reset() {
        store.clear();
    }

}
