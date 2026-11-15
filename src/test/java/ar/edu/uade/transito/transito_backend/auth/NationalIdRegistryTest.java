package ar.edu.uade.transito.transito_backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NationalIdRegistry (DEF-06)")
class NationalIdRegistryTest {

    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");
    private final NationalIdRegistry registry = new NationalIdRegistry(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("resuelve el DNI por subject mientras no venza")
    void resolves() {
        registry.remember("u-1", "30777888", NOW.plusSeconds(60));
        assertThat(registry.resolve("u-1")).contains("30777888");
        assertThat(registry.resolve("u-2")).isEmpty();
        assertThat(registry.resolve(null)).isEmpty();
    }

    @Test
    @DisplayName("una entrada vencida no se resuelve y se descarta")
    void expired() {
        registry.remember("u-1", "30777888", NOW.minusSeconds(1));
        assertThat(registry.resolve("u-1")).isEmpty();
        assertThat(registry.size()).isZero();
    }

    @Test
    @DisplayName("un nuevo login conserva la expiración más lejana; DNI null o subject null no se guardan")
    void mergeAndNulls() {
        registry.remember("u-1", "30777888", NOW.plusSeconds(600));
        registry.remember("u-1", "30777888", NOW.plusSeconds(60));
        assertThat(registry.resolve("u-1")).contains("30777888");
        registry.remember(null, "1", NOW.plusSeconds(60));
        registry.remember("u-1", null, NOW.plusSeconds(60));
        assertThat(registry.resolve("u-1")).isEmpty();
        assertThat(registry.size()).isZero();
    }

    @Test
    @DisplayName("purgeExpired elimina solo las vencidas")
    void purge() {
        registry.remember("viejo", "1", NOW.minusSeconds(1));
        registry.remember("vigente", "2", NOW.plusSeconds(60));
        registry.purgeExpired();
        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.resolve("vigente")).contains("2");
    }
}
