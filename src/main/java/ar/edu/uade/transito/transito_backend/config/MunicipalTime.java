package ar.edu.uade.transito.transito_backend.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Hora municipal (Argentina). Toda LocalDateTime de negocio que persiste el backend representa
 * hora local argentina, igual que las que carga el frontend desde un input datetime-local.
 * El servidor corre en UTC, por eso nunca se usa LocalDateTime.now() sin zona.
 */
public final class MunicipalTime {

    public static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private MunicipalTime() {
    }

    /** Fecha y hora actual en la zona municipal, tomada del reloj inyectado. */
    public static LocalDateTime now(Clock clock) {
        return LocalDateTime.now(clock.withZone(ZONE));
    }
}
