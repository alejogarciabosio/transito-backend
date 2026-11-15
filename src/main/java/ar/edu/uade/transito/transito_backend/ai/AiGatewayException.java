package ar.edu.uade.transito.transito_backend.ai;

/** Falla del proveedor de IA. El mensaje nunca contiene texto del usuario ni respuesta cruda. */
public class AiGatewayException extends RuntimeException {

    private final boolean retryable;

    public AiGatewayException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public AiGatewayException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
