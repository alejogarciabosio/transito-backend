package ar.edu.uade.transito.transito_backend.web;

import org.springframework.context.ApplicationContext;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

/**
 * DEF-27 / KI-3: la validación del body (@Valid) corre al resolver los argumentos, antes de que el proxy
 * de @PreAuthorize evalúe el rol. Cuando la validación falla, este componente evalúa la misma expresión
 * {@code @PreAuthorize} del método destino para que un rol sin permiso reciba 403 y no 400 (que revela
 * las reglas de validación). Solo decide qué error se informa: la autorización real sigue en el proxy.
 * Las expresiones del proyecto son solo hasRole/hasAnyRole (no usan argumentos del método).
 */
@Component
public class HandlerAuthorizationChecker {

    private final PreAuthorizeAuthorizationManager manager = new PreAuthorizeAuthorizationManager();

    public HandlerAuthorizationChecker(ApplicationContext applicationContext) {
        manager.setApplicationContext(applicationContext);
    }

    /** true si el usuario actual NO tiene permiso para invocar el handler según su @PreAuthorize. */
    public boolean isDenied(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return false;
        }
        try {
            SimpleMethodInvocation invocation = new SimpleMethodInvocation(handlerMethod.getBean(),
                    handlerMethod.getMethod(), new Object[handlerMethod.getMethod().getParameterCount()]);
            AuthorizationResult result = manager.authorize(
                    () -> SecurityContextHolder.getContext().getAuthentication(), invocation);
            return result != null && !result.isGranted();
        } catch (RuntimeException ex) {
            // Ante cualquier problema evaluando la expresión se mantiene el 400 original:
            // el proxy de @PreAuthorize sigue protegiendo el método.
            return false;
        }
    }
}
