package ar.edu.uade.transito.transito_backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ar.edu.uade.transito.transito_backend.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

    /** Claim que usaban los tokens anteriores a DEF-06; ya no se emite. */
    static final String LEGACY_NATIONAL_ID_CLAIM = "nationalId";

    private final Key signingKey;
    private final long expirationMinutes;
    private final NationalIdRegistry nationalIdRegistry;

    @Autowired
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes}") long expirationMinutes,
            NationalIdRegistry nationalIdRegistry) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
        this.nationalIdRegistry = nationalIdRegistry;
    }

    public JwtService(String secret, long expirationMinutes) {
        this(secret, expirationMinutes, new NationalIdRegistry());
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        // DEF-06: el DNI no va en el token; queda del lado servidor asociado al subject.
        nationalIdRegistry.remember(user.userId(), user.nationalId(), expiresAt);
        var builder = Jwts.builder()
                .subject(user.userId())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt));
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

    /**
     * DNI del usuario del token, resuelto del lado servidor por subject (DEF-06).
     * Compatibilidad hacia atrás: los tokens emitidos antes de este cambio (firmados por
     * nosotros, válidos hasta jwt.expiration-minutes) todavía traen el claim nationalId.
     */
    public String extractNationalId(Claims claims) {
        return nationalIdRegistry.resolve(claims.getSubject())
                .orElseGet(() -> claims.get(LEGACY_NATIONAL_ID_CLAIM, String.class));
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
