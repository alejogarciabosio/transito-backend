package ar.edu.uade.transito.transito_backend.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * DEF-06: el DNI ya no viaja dentro del JWT (cualquiera puede decodificar el payload).
 * Al emitir el token se guarda acá, del lado servidor, asociado al subject y con la
 * misma expiración del token; el filtro JWT lo resuelve por subject en cada request.
 * Vive en memoria: si el backend se reinicia, el validador de identidad puede volver a
 * resolverlo (stub) o el ciudadano recibe 401 y vuelve a iniciar sesión (Usuarios/M1).
 */
@Component
public class NationalIdRegistry {

    private static final int PURGE_THRESHOLD = 10_000;

    private record Entry(String nationalId, Instant expiresAt) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    public NationalIdRegistry() {
        this(Clock.systemUTC());
    }

    NationalIdRegistry(Clock clock) {
        this.clock = clock;
    }

    public void remember(String subject, String nationalId, Instant expiresAt) {
        if (subject == null) {
            return;
        }
        if (nationalId == null) {
            entries.remove(subject);
            return;
        }
        if (entries.size() >= PURGE_THRESHOLD) {
            purgeExpired();
        }
        entries.merge(subject, new Entry(nationalId, expiresAt),
                (old, nuevo) -> nuevo.expiresAt().isAfter(old.expiresAt()) ? nuevo : new Entry(nuevo.nationalId(), old.expiresAt()));
    }

    public Optional<String> resolve(String subject) {
        if (subject == null) {
            return Optional.empty();
        }
        Entry entry = entries.get(subject);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.expiresAt().isAfter(clock.instant())) {
            entries.remove(subject, entry);
            return Optional.empty();
        }
        return Optional.of(entry.nationalId());
    }

    void purgeExpired() {
        Instant now = clock.instant();
        entries.entrySet().removeIf(e -> !e.getValue().expiresAt().isAfter(now));
    }

    int size() {
        return entries.size();
    }
}
