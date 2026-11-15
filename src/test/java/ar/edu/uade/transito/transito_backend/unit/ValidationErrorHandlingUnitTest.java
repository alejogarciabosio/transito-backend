package ar.edu.uade.transito.transito_backend.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.HandlerMethod;

import ar.edu.uade.transito.transito_backend.web.GlobalExceptionHandler;
import ar.edu.uade.transito.transito_backend.web.HandlerAuthorizationChecker;

@DisplayName("DEF-27/DEF-28: ramas defensivas del manejo de errores")
class ValidationErrorHandlingUnitTest {

    private final HandlerAuthorizationChecker checker = new HandlerAuthorizationChecker(mock(ApplicationContext.class));

    @Test
    @DisplayName("sin handler o con error al evaluar la expresión se mantiene el 400 (no se deniega)")
    void checkerIsLenientOnMissingOrBrokenHandler() {
        assertThat(checker.isDenied(null)).isFalse();
        HandlerMethod broken = mock(HandlerMethod.class);
        when(broken.getMethod()).thenThrow(new IllegalStateException("x"));
        assertThat(checker.isDenied(broken)).isFalse();
    }

    @Test
    @DisplayName("cabecera obligatoria faltante → 400 application/problem+json")
    void missingHeaderIsProblemDetails() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(checker);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/x");
        var parameter = new org.springframework.core.MethodParameter(
                Object.class.getMethod("equals", Object.class), 0);
        var response = handler.handleRequestBinding(new MissingRequestHeaderException("X-Test", parameter), request);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody().getInstance()).hasToString("/api/v1/x");
    }
}
