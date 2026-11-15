package ar.edu.uade.transito.transito_backend.ai;

/**
 * Puerta única hacia el proveedor de IA (Adapter/Facade). El resto del sistema no conoce
 * HTTP, claves ni formato del proveedor. Toda la salida es input NO confiable.
 */
public interface AiGateway {

    /**
     * @param systemPrompt instrucciones de sistema (prompt versionado)
     * @param userData     dato del usuario ya enmascarado; el gateway lo delimita como DATO
     * @return respuesta cruda del modelo y tokens usados
     * @throws AiGatewayException ante timeout, red, HTTP no exitoso o respuesta ilegible
     */
    AiCompletion complete(String systemPrompt, String userData);

    /** Respuesta cruda del modelo. {@code totalTokens} es -1 si el proveedor no lo informa. */
    record AiCompletion(String content, int totalTokens) {
        @Override
        public String toString() {
            return "AiCompletion[tokens=" + totalTokens + "]"; // nunca exponer el contenido en logs
        }
    }
}
