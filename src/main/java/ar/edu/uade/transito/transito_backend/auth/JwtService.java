package ar.edu.uade.transito.transito_backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ar.edu.uade.transito.transito_backend.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

    private final Key signingKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(user.userId())
                .claim("nationalId", user.nationalId())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)));
        if (user.actorType() != null) {
            builder.claim("actorType", user.actorType());
        }
        if (user.citizenId() != null) {
            builder.claim("citizenId", user.citizenId());
        }
        return builder.signWith(signingKey).compact();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }

    public String extractNationalId(Claims claims) {
        return claims.get("nationalId", String.class);
    }

    public Role extractRole(Claims claims) {
        String role = claims.get("role", String.class);
        if (role == null) {
            throw new IllegalArgumentException("El token no incluye el claim role");
        }
        return Role.valueOf(role);
    }

    public String extractActorType(Claims claims) {
        return claims.get("actorType", String.class);
    }

    public String extractCitizenId(Claims claims) {
        return claims.get("citizenId", String.class);
    }
}
