package ar.edu.uade.transito.transito_backend.unit;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.CurrentUserProvider;
import ar.edu.uade.transito.transito_backend.auth.StubIdentityValidator;
import ar.edu.uade.transito.transito_backend.exception.InvalidCredentialsException;
import ar.edu.uade.transito.transito_backend.model.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Componentes de autenticación")
class AuthComponentsTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-BE-AUTH-04: StubIdentityValidator valida usuario y contraseña")
    void stubValidator() {
        var validator = new StubIdentityValidator();
        AuthenticatedUser user = validator.validate("admin1", "admin123");
        assertThat(user.userId()).isEqualTo("u-1002");
        assertThat(user.role()).isEqualTo(Role.ADMINISTRADOR);
        assertThatThrownBy(() -> validator.validate("deposito1", "x")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> validator.validate("nadie", "x")).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("TC-BE-AUTH-05: CurrentUserProvider devuelve el principal del SecurityContext")
    void currentUser() {
        var principal = new AuthenticatedUser("u-1", "1", Role.INSPECTOR);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        assertThat(new CurrentUserProvider().get()).isEqualTo(principal);
    }
}
