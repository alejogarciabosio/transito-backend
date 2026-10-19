package ar.edu.uade.transito.transito_backend.model.enums;

public enum RequestingModule {
    OBRAS,
    AMBIENTE;

    /** Código funcional acordado dentro de data para los eventos de cortes. */
    public String sourceModule() {
        return this == OBRAS ? "M3" : "M6";
    }

    /** Nombre funcional publicado hacia el módulo que originó la solicitud. */
    public String displayName() {
        return this == OBRAS ? "Obras" : "Ambiente";
    }

    /** Technical Core identifier used by integration payloads. */
    public String technicalName() {
        return this == OBRAS ? "obras" : "ambiente";
    }

    /**
     * Acepta el código funcional del payload y los nombres de envelope para
     * mantener compatibilidad con los mensajes de transición ya intercambiados.
     */
    public static RequestingModule fromSourceModule(String sourceModule) {
        return switch (sourceModule) {
            case "M3", "obras" -> OBRAS;
            case "M6", "ambiente" -> AMBIENTE;
            default -> throw new IllegalArgumentException("sourceModule desconocido: " + sourceModule);
        };
    }
}
