package ar.edu.uade.transito.transito_backend.support;

import static org.awaitility.Awaitility.await;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * Base de los tests de integración con infraestructura REAL: PostgreSQL y RabbitMQ en contenedores
 * (Testcontainers), con {@code core.rabbitmq.enabled=true}: se usan el {@code RabbitMqEventPublisher},
 * el relay del outbox y el {@code RabbitMqIncomingEventListener} de producción, sin mocks.
 *
 * <p>Los contenedores son únicos para toda la suite (patrón singleton: se levantan una vez y Ryuk los
 * borra al terminar la JVM) y todas las subclases comparten el mismo contexto de Spring cacheado.
 *
 * <p>Topología: en producción el exchange {@code muni.inbox}, la cola {@code q.transito} y su DLQ los
 * declara y administra <b>Core</b>; la app solo publica y consume. Acá el test hace de Core y los declara
 * antes de arrancar el contexto: la cola consumida tiene un dead-letter exchange, como haría Core, y una
 * cola de captura enlazada al exchange recibe lo que publica Tránsito.
 *
 * <p>Etiqueta {@code testcontainers}: corren en {@code ./mvnw verify} (requieren Docker). Para correr solo
 * estos: {@code ./mvnw test -Dgroups=testcontainers -Djacoco.skip=true}.
 */
@Tag("testcontainers")
@SpringBootTest(properties = {
        "core.rabbitmq.enabled=true",
        // Nunca se llama a Core por HTTP desde los tests.
        "core.publications.registration.enabled=false",
        "core.base-url=http://localhost:9", // inalcanzable a propósito: ningún test habla con Core
        "core.module-secret=solo-tests",
        "core.rabbitmq.exchange=" + AbstractBrokerIntegrationTest.CORE_EXCHANGE,
        "core.rabbitmq.queue=" + AbstractBrokerIntegrationTest.TRANSITO_QUEUE,
        // El relay no corre solo: los tests lo invocan a mano (determinismo). Sin espera entre reintentos.
        "outbox.relay.enabled=false",
        "outbox.relay.backoff-base-ms=0",
        "spring.rabbitmq.ssl.enabled=false",
        "spring.rabbitmq.virtual-host=/",
        "spring.rabbitmq.connection-timeout=2s",
        "spring.rabbitmq.listener.simple.recovery-interval=500ms"
})
@AutoConfigureMockMvc
public abstract class AbstractBrokerIntegrationTest {

    /** Mismas imágenes con versión fija en toda la suite (la de Postgres es la de PostgresContainerIntegrationTest). */
    public static final String POSTGRES_IMAGE = "postgres:16-alpine";
    public static final String RABBITMQ_IMAGE = "rabbitmq:3.13.7-alpine";

    /** Nombres de producción (application.properties): core.rabbitmq.exchange / core.rabbitmq.queue. */
    protected static final String CORE_EXCHANGE = "muni.inbox";
    protected static final String TRANSITO_QUEUE = "q.transito";
    /** Topología propia del test que simula a Core. */
    protected static final String CAPTURE_QUEUE = "test.core.captura";
    protected static final String DLX = "test.core.dlx";
    protected static final String DLQ = "q.transito.dlq";

    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("transito_test")
            .withUsername("transito")
            .withPassword("transito");

    protected static final RabbitMQContainer RABBIT = new RabbitMQContainer(RABBITMQ_IMAGE);

    static {
        Startables.deepStart(POSTGRES, RABBIT).join();
        declareCoreTopology();
    }

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        // El publicador pone user_id = spring.rabbitmq.username; RabbitMQ exige que coincida con el usuario conectado.
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    }

    private static void declareCoreTopology() {
        withChannel(channel -> {
            channel.exchangeDeclare(CORE_EXCHANGE, "fanout", true);
            channel.queueDeclare(CAPTURE_QUEUE, true, false, false, null);
            channel.queueBind(CAPTURE_QUEUE, CORE_EXCHANGE, "");
            channel.exchangeDeclare(DLX, "fanout", true);
            channel.queueDeclare(DLQ, true, false, false, null);
            channel.queueBind(DLQ, DLX, "");
            channel.queueDeclare(TRANSITO_QUEUE, true, false, false, Map.of("x-dead-letter-exchange", DLX));
            return null;
        });
    }

    @FunctionalInterface
    protected interface ChannelCallback<T> {
        T apply(Channel channel) throws Exception;
    }

    /** Conexión nueva por operación (independiente de la app): sobrevive a reinicios del broker. */
    protected static <T> T withChannel(ChannelCallback<T> callback) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBIT.getHost());
        factory.setPort(RABBIT.getAmqpPort());
        factory.setUsername(RABBIT.getAdminUsername());
        factory.setPassword(RABBIT.getAdminPassword());
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            return callback.apply(channel);
        } catch (Exception ex) {
            throw new IllegalStateException("Operación contra RabbitMQ del test falló", ex);
        }
    }

    protected static void purge(String queue) {
        withChannel(channel -> channel.queuePurge(queue));
    }

    /** Publica en la cola que consume la app (default exchange), como lo haría Core al rutear. */
    protected static void publishToTransitoQueue(String body) {
        publishToTransitoQueue(body, null);
    }

    /** Variante con la propiedad AMQP message-id (la que el log de rechazo puede mostrar). */
    protected static void publishToTransitoQueue(String body, String messageId) {
        withChannel(channel -> {
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .contentType("application/json").deliveryMode(2).messageId(messageId).build();
            channel.basicPublish("", TRANSITO_QUEUE, props, body.getBytes(StandardCharsets.UTF_8));
            return null;
        });
    }

    /** Saca (con ack) todos los mensajes que haya hoy en la cola. */
    protected static List<GetResponse> drain(String queue) {
        return withChannel(channel -> {
            List<GetResponse> messages = new ArrayList<>();
            GetResponse response;
            while ((response = channel.basicGet(queue, true)) != null) {
                messages.add(response);
            }
            return messages;
        });
    }

    protected static String body(GetResponse response) {
        return new String(response.getBody(), StandardCharsets.UTF_8);
    }

    /** Broker caído de verdad: se detiene la aplicación RabbitMQ (cierra conexiones y el puerto AMQP). */
    protected static void stopBroker() throws Exception {
        rabbitmqctl("stop_app");
    }

    /** Vuelve el broker (mismo contenedor y puerto; colas durables y mensajes persistentes se conservan). */
    protected static void startBroker() throws Exception {
        rabbitmqctl("start_app");
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200)).ignoreExceptions()
                .until(() -> withChannel(channel -> channel.isOpen()));
    }

    private static void rabbitmqctl(String command) throws Exception {
        ExecResult result = RABBIT.execInContainer("rabbitmqctl", command);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("rabbitmqctl " + command + " falló: " + result.getStderr());
        }
    }
}
