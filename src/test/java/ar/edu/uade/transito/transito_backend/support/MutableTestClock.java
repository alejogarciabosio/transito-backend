package ar.edu.uade.transito.transito_backend.support;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj de tests de integración: fijo y movible (reglas temporales, DEF-42). */
public class MutableTestClock extends Clock {

    /** Mediodía del 1/10/2026, dentro de la ventana que usan los fixtures de cortes (1/10 00:00 – 2/10 00:00). */
    public static final OffsetDateTime DEFAULT = OffsetDateTime.parse("2026-10-01T12:00:00-03:00");

    private volatile Instant instant = DEFAULT.toInstant();

    public void set(OffsetDateTime now) {
        this.instant = now.toInstant();
    }

    public void reset() {
        set(DEFAULT);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
