package ar.edu.uade.transito.transito_backend.model.enums;

public enum RequestingModule {
    OBRAS,
    AMBIENTE;

    /**
     * Código M-number canónico (M3/M6), usado como clave interna en eventos como
     * streetClosureActivated, según el mapeo único de identidad de módulo acordado con el equipo.
     */
    public String code() {
        return this == OBRAS ? "M3" : "M6";
    }

    /**
     * Camino inverso de {@link #code()} -- lo usan los listeners que reciben el código M-number
     * (ej. streetClosureRequested) y necesitan el enum interno.
     */
    public static RequestingModule fromCode(String code) {
        return switch (code) {
            case "M3" -> OBRAS;
            case "M6" -> AMBIENTE;
            default -> throw new IllegalArgumentException("Código de módulo desconocido: " + code);
        };
    }
}
