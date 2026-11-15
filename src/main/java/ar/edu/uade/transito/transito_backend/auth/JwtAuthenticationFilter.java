package ar.edu.uade.transito.transito_backend.auth;

import java.io.IOException;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import ar.edu.uade.transito.transito_backend.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final IdentityValidator identityValidator;

    //Aquí se puede ver el patrón de diseño Chain of Responsability, ya que este filtro se encarga de procesar la solicitud y luego pasarla al siguiente filtro en la cadena.
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            try {
                Claims claims = jwtService.parseClaims(token);
                String subject = jwtService.extractUserId(claims);
                Role role = jwtService.extractRole(claims);
                // DEF-06: el DNI no viaja en el token; se resuelve del lado servidor por subject.
                String nationalId = jwtService.extractNationalId(claims);
                if (nationalId == null) {
                    nationalId = identityValidator.findNationalId(subject).orElse(null);
                }
                if (nationalId == null && role == Role.CIUDADANO) {
                    // Sin DNI no se pueden filtrar sus datos: se pide volver a iniciar sesión (401).
                    throw new IllegalArgumentException("No se pudo resolver la identidad del ciudadano");
                }
                AuthenticatedUser principal = new AuthenticatedUser(
                        subject,
                        nationalId,
                        role,
                        jwtService.extractActorType(claims),
                        jwtService.extractCitizenId(claims));

                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
