package ar.edu.uade.transito.transito_backend.auth;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limita los intentos fallidos de login por IP (defensa contra fuerza bruta, OWASP API2 / SEC-05).
 * Tras {@code maxAttempts} fallos dentro de la ventana responde 429 con Problem Details y {@code Retry-After}.
 * Un login correcto limpia el contador. Estado en memoria: suficiente para una instancia.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private final int maxAttempts;
    private final long windowMillis;
    private final Map<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(@Value("${login.rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${login.rate-limit.window-seconds:60}") long windowSeconds) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowSeconds * 1000;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && LOGIN_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = clientKey(request);
        long now = System.currentTimeMillis();
        Deque<Long> recent = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        long retryAfter = 0;
        synchronized (recent) {
            while (!recent.isEmpty() && now - recent.peekFirst() > windowMillis) {
                recent.pollFirst();
            }
            if (recent.size() >= maxAttempts) {
                retryAfter = Math.max(1, (windowMillis - (now - recent.peekFirst())) / 1000);
            }
        }
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
        chain.doFilter(request, response);
        int status = response.getStatus();
        synchronized (recent) {
            if (status == 401 || status == 400) {
                recent.addLast(System.currentTimeMillis());
            } else if (status < 300) {
                recent.clear();
            }
        }
    }

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }

    /** Solo para tests: limpia el estado. */
    public void reset() {
        failures.clear();
    }
}
