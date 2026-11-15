package ar.edu.uade.transito.transito_backend.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.CurrentUserProvider;
import ar.edu.uade.transito.transito_backend.auth.IdentityValidator;
import ar.edu.uade.transito.transito_backend.auth.JwtService;
import ar.edu.uade.transito.transito_backend.web.dto.CurrentUserResponse;
import ar.edu.uade.transito.transito_backend.web.dto.LoginRequest;
import ar.edu.uade.transito.transito_backend.web.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IdentityValidator identityValidator;
    private final JwtService jwtService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String identity = request.gmail() != null && !request.gmail().isBlank()
                ? request.gmail()
                : request.username();
        AuthenticatedUser user = identityValidator.validate(identity, request.password());
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user.role(), jwtService.getExpirationSeconds());
    }

    /** Datos del propio usuario autenticado (DEF-06: el DNI ya no se lee del JWT). */
    @GetMapping("/me")
    public CurrentUserResponse me() {
        AuthenticatedUser user = currentUserProvider.get();
        return new CurrentUserResponse(user.userId(), user.role(), user.nationalId(), user.actorType(), user.citizenId());
    }
}
