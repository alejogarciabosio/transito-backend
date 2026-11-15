package ar.edu.uade.transito.transito_backend.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ar.edu.uade.transito.transito_backend.auth.LoginRateLimitFilter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** KI-2: selección de la IP del cliente según los proxies de confianza y robustez ante bodies inválidos. */
class LoginRateLimitFilterTest {

    private static MockHttpServletRequest login(String xff, String body) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        req.setRemoteAddr("10.10.10.10");
        if (xff != null) {
            req.addHeader("X-Forwarded-For", xff);
        }
        req.setContent(body.getBytes());
        return req;
    }

    /** Responde 401 y lee el body por getReader(), como haría un controlador. */
    private static MockFilterChain failingChain() {
        return new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
                req.getReader().lines().count();
                resp.setStatus(401);
            }
        });
    }

    private static int status(LoginRateLimitFilter filter, String xff, String body) throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(login(xff, body), resp, failingChain());
        return resp.getStatus();
    }

    @Test
    @DisplayName("trusted-proxies=2 toma la segunda entrada desde la derecha")
    void twoTrustedProxies() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(2, 60, 100, 2);
        assertThat(status(filter, "1.1.1.1, 9.9.9.9, 172.16.0.1", "{}")).isEqualTo(401);
        assertThat(status(filter, "2.2.2.2, 9.9.9.9, 172.16.0.2", "{}")).isEqualTo(401);
        assertThat(status(filter, "3.3.3.3, 9.9.9.9, 172.16.0.3", "{}")).isEqualTo(429);
    }

    @Test
    @DisplayName("trusted-proxies=0 ignora X-Forwarded-For y usa la IP remota")
    void zeroTrustedProxiesIgnoresHeader() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(2, 60, 100, 0);
        assertThat(status(filter, "1.1.1.1", "{}")).isEqualTo(401);
        assertThat(status(filter, "2.2.2.2", "{}")).isEqualTo(401);
        assertThat(status(filter, "3.3.3.3", "{}")).isEqualTo(429);
    }

    @Test
    @DisplayName("body no JSON o sin identidad no rompe el filtro (solo cuenta por IP)")
    void invalidBodies() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(100, 60, 1, 1);
        assertThat(status(filter, "5.5.5.1", "no-es-json")).isEqualTo(401);
        assertThat(status(filter, "5.5.5.2", "[1,2]")).isEqualTo(401);
        assertThat(status(filter, "5.5.5.3", "{\"username\":123}")).isEqualTo(401);
        assertThat(status(filter, "5.5.5.4", "{\"username\":\"  \"}")).isEqualTo(401);
        assertThat(status(filter, ",", "{}")).isEqualTo(401);
    }

    @Test
    @DisplayName("gmail tiene prioridad sobre username como identidad")
    void gmailIdentity() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(100, 60, 1, 1);
        assertThat(status(filter, "6.6.6.1", "{\"gmail\":\"A@x.com\",\"username\":\"u1\"}")).isEqualTo(401);
        assertThat(status(filter, "6.6.6.2", "{\"gmail\":\"a@x.com\",\"username\":\"u2\"}")).isEqualTo(429);
        assertThat(status(filter, "6.6.6.3", "{\"username\":\"u1\"}")).isEqualTo(401);
    }
}
