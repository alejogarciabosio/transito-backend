package ar.edu.uade.transito.transito_backend.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;

import java.net.ConnectException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OutboxRelayUnitTest {

    @Test
    @DisplayName("TC-BE-OUT-05: backoff exponencial con tope")
    void backoffExponencialConTope() {
        OutboxRelay relay = new OutboxRelay(null, mock(EventTransport.class),
                mock(org.springframework.transaction.PlatformTransactionManager.class),
                java.time.Clock.systemUTC(), 10, 5, Duration.ofSeconds(2), Duration.ofSeconds(60), Duration.ofSeconds(30));
        assertThat(relay.backoff(1)).isEqualTo(Duration.ofSeconds(2));
        assertThat(relay.backoff(2)).isEqualTo(Duration.ofSeconds(4));
        assertThat(relay.backoff(3)).isEqualTo(Duration.ofSeconds(8));
        assertThat(relay.backoff(10)).isEqualTo(Duration.ofSeconds(60));
        assertThat(relay.backoff(100)).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("TC-BE-OUT-06: el error guardado es técnico y recortado (sin payload)")
    void describeError() {
        String connection = OutboxRelay.describe(new AmqpConnectException(new ConnectException("Connection refused")));
        assertThat(connection).isEqualTo("AmqpConnectException <- ConnectException: Connection refused");
        assertThat(OutboxRelay.describe(new IllegalStateException("x".repeat(900)))).hasSize(500)
                .startsWith("IllegalStateException: x");
    }

    @Test
    @DisplayName("TC-BE-OUT-07: el scheduler del relay no se cae si el relay falla (p. ej. base caída)")
    void schedulerToleraErrores() {
        OutboxRelay relay = mock(OutboxRelay.class);
        org.mockito.Mockito.when(relay.relayPending()).thenThrow(new IllegalStateException("db down"))
                .thenReturn(1);
        OutboxRelayScheduler scheduler = new OutboxRelayScheduler(relay);
        org.assertj.core.api.Assertions.assertThatCode(scheduler::run).doesNotThrowAnyException();
        scheduler.run();
        org.mockito.Mockito.verify(relay, org.mockito.Mockito.times(2)).relayPending();
    }

    @Test
    @DisplayName("TC-BE-OUT-08: se consideran broker inalcanzable las fallas de conexión, IO y timeout de AMQP")
    void brokerInalcanzable() {
        assertThat(OutboxRelay.isBrokerUnreachable(new AmqpConnectException(new ConnectException("x")))).isTrue();
        assertThat(OutboxRelay.isBrokerUnreachable(new org.springframework.amqp.AmqpIOException(
                new java.io.IOException(new java.net.UnknownHostException("rabbitmq"))))).isTrue();
        assertThat(OutboxRelay.isBrokerUnreachable(new org.springframework.amqp.AmqpTimeoutException("t"))).isTrue();
        assertThat(OutboxRelay.isBrokerUnreachable(new IllegalStateException("x"))).isFalse();
    }
}
