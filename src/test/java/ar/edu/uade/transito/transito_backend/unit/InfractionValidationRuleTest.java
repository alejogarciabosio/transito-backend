package ar.edu.uade.transito.transito_backend.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.exception.InvalidRequestException;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.enums.Role;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import ar.edu.uade.transito.transito_backend.service.InfractionService;

/** DEF-16: regla "evidencia o validación del inspector" en InfractionService.register. */
class InfractionValidationRuleTest {

    private static final String MESSAGE = "La infracción requiere evidencia o la validación de un inspector";

    private final InfractionRepository repository = mock(InfractionRepository.class);
    private final InfractionService service = new InfractionService(repository, mock(EventPublisher.class));

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String userId, Role role) {
        AuthenticatedUser user = new AuthenticatedUser(userId, null, role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private Infraction infraction(String inspectorId, String... evidence) {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return Infraction.builder().licensePlate("AB123CD").inspectorId(inspectorId)
                .evidenceUrl(new ArrayList<>(Arrays.asList(evidence))).build();
    }

    @Test
    void evidenceIsEnoughEvenWithoutUser() {
        assertThat(service.register(infraction(null, "https://e/1.jpg")).getStatus()).isNotNull();
    }

    @Test
    void adminWithEvidenceIsAccepted() {
        loginAs("u-1002", Role.ADMINISTRADOR);
        assertThat(service.register(infraction(null, "https://e/1.jpg")).getStatus()).isNotNull();
    }

    @Test
    void inspectorWithoutEvidenceIsAccepted() {
        loginAs("u-1001", Role.INSPECTOR);
        assertThat(service.register(infraction("u-1001")).getInspectorId()).isEqualTo("u-1001");
    }

    @Test
    void inspectorWithoutEvidenceAndMissingInspectorIdIsCompleted() {
        loginAs("u-1001", Role.INSPECTOR);
        assertThat(service.register(infraction(" ")).getInspectorId()).isEqualTo("u-1001");
        assertThat(service.register(infraction(null)).getInspectorId()).isEqualTo("u-1001");
    }

    @Test
    void inspectorWithForeignInspectorIdIsRejected() {
        loginAs("u-1001", Role.INSPECTOR);
        assertThatThrownBy(() -> service.register(infraction("u-OTRO")))
                .isInstanceOf(InvalidRequestException.class).hasMessage(MESSAGE);
        verify(repository, never()).save(any());
    }

    @Test
    void adminWithoutEvidenceIsRejected() {
        loginAs("u-1002", Role.ADMINISTRADOR);
        assertThatThrownBy(() -> service.register(infraction("u-1002")))
                .isInstanceOf(InvalidRequestException.class).hasMessage(MESSAGE);
    }

    @Test
    void blankEvidenceDoesNotCountAndNoUserIsRejected() {
        assertThatThrownBy(() -> service.register(infraction("u-1001", " ")))
                .isInstanceOf(InvalidRequestException.class).hasMessage(MESSAGE);
    }

    @Test
    void nullEvidenceListWithoutUserIsRejected() {
        Infraction inf = infraction("u-1001");
        inf.setEvidenceUrl(null);
        assertThatThrownBy(() -> service.register(inf)).isInstanceOf(InvalidRequestException.class);
    }
}
