package ar.edu.uade.transito.transito_backend.unit;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.CurrentUserProvider;
import ar.edu.uade.transito.transito_backend.auth.StubIdentityValidator;
import ar.edu.uade.transito.transito_backend.exception.InvalidCredentialsException;
import ar.edu.uade.transito.transito_backend.model.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("Componentes de autenticación")
class AuthComponentsTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-BE-AUTH-04: StubIdentityValidator valida usuario y contraseña")
    void stubValidator() {
        var validator = new StubIdentityValidator(new MockEnvironment()
                .withProperty("stub.password.admin1", "test-admin1-pass"));
        AuthenticatedUser user = validator.validate("admin1", "test-admin1-pass");
        assertThat(user.userId()).isEqualTo("u-1002");
        assertThat(user.role()).isEqualTo(Role.ADMINISTRADOR);
        assertThatThrownBy(() -> validator.validate("deposito1", "x")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> validator.validate("nadie", "x")).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("TC-BE-AUTH-04b: usuario sin variable STUB_PASSWORD_* queda deshabilitado (fail-closed) y se avisa sin el valor")
    void stubValidatorWithoutPasswordIsDisabled(CapturedOutput output) {
        var validator = new StubIdentityValidator(new MockEnvironment()
                .withProperty("stub.password.inspector1", "test-inspector1-pass")
                .withProperty("stub.password.admin1", ""));
        assertThat(validator.validate("inspector1", "test-inspector1-pass").role()).isEqualTo(Role.INSPECTOR);
        assertThatThrownBy(() -> validator.validate("inspector1", "otra")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> validator.validate("inspector1", null)).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> validator.validate(null, "x")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> validator.validate("admin1", "")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> validator.validate("ciudadano1", "")).isInstanceOf(InvalidCredentialsException.class);
        assertThat(output).contains("STUB_PASSWORD_ADMIN1", "STUB_PASSWORD_CIUDADANO1")
                .doesNotContain("STUB_PASSWORD_INSPECTOR1").doesNotContain("test-inspector1-pass");
    }

    @Test
    @DisplayName("TC-BE-AUTH-05: CurrentUserProvider devuelve el principal del SecurityContext")
    void currentUser() {
        var principal = new AuthenticatedUser("u-1", "1", Role.INSPECTOR);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        assertThat(new CurrentUserProvider().get()).isEqualTo(principal);
    }
}
