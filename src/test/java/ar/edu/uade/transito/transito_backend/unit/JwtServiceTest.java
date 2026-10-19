package ar.edu.uade.transito.transito_backend.unit;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.JwtService;
import ar.edu.uade.transito.transito_backend.model.enums.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET = "test-secret-de-al-menos-32-bytes-para-hs256-0123456789";
    private final AuthenticatedUser user = new AuthenticatedUser("u-1", "30111222", Role.ADMINISTRADOR);

    @Test
    @DisplayName("TC-BE-JWT-01: genera y parsea un token con subject, nationalId y role")
    void roundTrip() {
        JwtService service = new JwtService(SECRET, 120);
        var claims = service.parseClaims(service.generateToken(user));
        assertThat(service.extractUserId(claims)).isEqualTo("u-1");
        assertThat(service.extractNationalId(claims)).isEqualTo("30111222");
        assertThat(service.extractRole(claims)).isEqualTo(Role.ADMINISTRADOR);
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("TC-BE-JWT-02: expiración en segundos = minutos * 60")
    void expirationSeconds() {
        assertThat(new JwtService(SECRET, 120).getExpirationSeconds()).isEqualTo(7200);
    }

    @Test
    @DisplayName("TC-BE-JWT-03: token vencido → ExpiredJwtException")
    void expired() {
        JwtService service = new JwtService(SECRET, -1);
        String token = service.generateToken(user);
        assertThatThrownBy(() -> service.parseClaims(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("TC-BE-JWT-04: token firmado con otro secreto → JwtException")
    void otherSecret() {
        String token = new JwtService("otro-secreto-distinto-de-32-bytes-o-mas-xxxxxxxx", 10).generateToken(user);
        assertThatThrownBy(() -> new JwtService(SECRET, 10).parseClaims(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("TC-BE-JWT-05: secreto menor a 256 bits → la app no arranca (WeakKeyException)")
    void weakSecret() {
        assertThatThrownBy(() -> new JwtService("corto", 10)).isInstanceOf(io.jsonwebtoken.security.WeakKeyException.class);
    }
}
