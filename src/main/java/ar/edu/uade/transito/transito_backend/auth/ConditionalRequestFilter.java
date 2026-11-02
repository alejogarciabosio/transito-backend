package ar.edu.uade.transito.transito_backend.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Concurrencia optimista sobre la API REST (RFC 9110 §13):
 * <ul>
 *   <li>Las respuestas GET 200 de {@code /api/} llevan {@code ETag} (hash del contenido) y responden 304 a {@code If-None-Match}.</li>
 *   <li>Un PUT/PATCH/DELETE con {@code If-Match} se ejecuta solo si coincide con el ETag actual del recurso;
 *       si no, responde 412 Precondition Failed. Sin {@code If-Match} nada cambia.</li>
 * </ul>
 */
@Component
public class ConditionalRequestFilter extends OncePerRequestFilter implements org.springframework.core.Ordered {

    /** Último filtro de la cadena: la lectura interna del ETag reinvoca solo al servlet. */
    @Override
    public int getOrder() {
        return org.springframework.core.Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String method = request.getMethod();
        String ifMatch = request.getHeader("If-Match");
        if (ifMatch != null && ("PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method))) {
            String current = currentEtag(request, response, chain);
            if (current != null && !matches(ifMatch, current)) {
                response.setStatus(412);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/problem+json");
                response.getWriter().write("""
                        {"type":"https://transito.uade.edu.ar/problems/precondition-failed","title":"Precondition Failed","status":412,"detail":"El recurso cambió desde que lo leíste (If-Match no coincide con el ETag actual). Volvé a consultarlo.","instance":"%s"}
                        """.formatted(request.getRequestURI()));
                return;
            }
        }
        if (!"GET".equals(method)) {
            chain.doFilter(request, response);
            return;
        }
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, wrapper);
        if (wrapper.getStatus() == 200 && wrapper.getContentAsByteArray().length > 0 && !wrapper.isCommitted()) {
            String etag = etagOf(wrapper.getContentAsByteArray());
            wrapper.setHeader("ETag", etag);
            if (matches(request.getHeader("If-None-Match"), etag)) {
                wrapper.reset();
                wrapper.setStatus(304);
                wrapper.setHeader("ETag", etag);
            }
        }
        wrapper.copyBodyToResponse();
    }

    /** Ejecuta internamente un GET al mismo recurso (mismo usuario) y devuelve su ETag; null si no se puede leer. */
    private String currentEtag(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest asGet = new HttpServletRequestWrapper(request) {
            @Override
            public String getMethod() {
                return "GET";
            }
        };
        ProbeResponse probe = new ProbeResponse(response);
        try {
            chain.doFilter(asGet, probe);
            probe.getWriter().flush();
        } catch (RuntimeException e) {
            return null;
        }
        return probe.getStatus() == 200 && probe.body().length > 0 ? etagOf(probe.body()) : null;
    }

    private static boolean matches(String header, String etag) {
        if (header == null) {
            return false;
        }
        for (String candidate : header.split(",")) {
            String c = candidate.trim();
            if ("*".equals(c) || c.replaceFirst("^W/", "").equals(etag)) {
                return true;
            }
        }
        return false;
    }

    private static String etagOf(byte[] body) {
        try {
            return "\"" + HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(body)) + "\"";
        } catch (Exception e) {
            return "\"" + new String(body, StandardCharsets.UTF_8).hashCode() + "\"";
        }
    }

    /** Respuesta descartable: captura estado y cuerpo sin tocar la respuesta real. */
    private static final class ProbeResponse extends HttpServletResponseWrapper {
        private int status = 200;
        private final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();

        ProbeResponse(HttpServletResponse real) {
            super(real);
        }

        @Override public void setStatus(int sc) { this.status = sc; }
        @Override public void sendError(int sc) { this.status = sc; }
        @Override public void sendError(int sc, String msg) { this.status = sc; }
        @Override public int getStatus() { return status; }
        @Override public void setHeader(String n, String v) { }
        @Override public void addHeader(String n, String v) { }
        @Override public void setIntHeader(String n, int v) { }
        @Override public void addIntHeader(String n, int v) { }
        @Override public void setDateHeader(String n, long v) { }
        @Override public void addDateHeader(String n, long v) { }
        @Override public void setContentType(String t) { }
        @Override public void setContentLength(int l) { }
        @Override public void setContentLengthLong(long l) { }
        @Override public void setCharacterEncoding(String c) { }
        @Override public void flushBuffer() { }
        @Override public boolean isCommitted() { return false; }
        @Override public void reset() { buffer.reset(); }

        @Override
        public ServletOutputStream getOutputStream() {
            return new ServletOutputStream() {
                @Override public void write(int b) { buffer.write(b); }
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(WriteListener l) { }
            };
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(new OutputStreamWriter(buffer, StandardCharsets.UTF_8), true);
        }

        byte[] body() { return buffer.toByteArray(); }
    }
}
