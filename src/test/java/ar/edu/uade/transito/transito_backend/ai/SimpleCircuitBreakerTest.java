package ar.edu.uade.transito.transito_backend.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SimpleCircuitBreaker")
class SimpleCircuitBreakerTest {

    static class MutableClock extends Clock {
        long now = 1_000;
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(now); }
    }

    @Test
    void opensAfterThresholdAndClosesAfterSuccessfulTrial() {
        MutableClock clock = new MutableClock();
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker(2, 1000, clock);
        assertThat(cb.allowRequest()).isTrue();
        cb.recordFailure();
        assertThat(cb.isOpen()).isFalse();
        cb.recordFailure();
        assertThat(cb.isOpen()).isTrue();
        assertThat(cb.allowRequest()).isFalse();
        clock.now += Duration.ofMillis(1000).toMillis();
        assertThat(cb.allowRequest()).isTrue();   // intento de prueba
        assertThat(cb.allowRequest()).isFalse();  // solo uno
        cb.recordSuccess();
        assertThat(cb.isOpen()).isFalse();
        assertThat(cb.allowRequest()).isTrue();
    }

    @Test
    void failedTrialReopensAndSuccessResetsCounter() {
        MutableClock clock = new MutableClock();
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker(1, 500, clock);
        cb.recordFailure();
        clock.now += 500;
        assertThat(cb.allowRequest()).isTrue();
        cb.recordFailure();
        assertThat(cb.allowRequest()).isFalse();
        cb.reset();
        assertThat(cb.isOpen()).isFalse();
    }
}
