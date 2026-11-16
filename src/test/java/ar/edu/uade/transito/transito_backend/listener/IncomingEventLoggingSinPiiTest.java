package ar.edu.uade.transito.transito_backend.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import ar.edu.uade.transito.transito_backend.repository.ProcessedEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** DEF-05: al consumir eventos de Core solo se loguean metadatos, nunca el payload. */
@ExtendWith(OutputCaptureExtension.class)
class IncomingEventLoggingSinPiiTest {

    private static final String DNI = "30111222";
    private static final String PATENTE = "AB123CD";
    private static final String EVENT_ID = "7f9c91f0-03c6-4baf-8791-8cb2c38981d2";
    private static final String CORRELATION_ID = "0b6f1a52-6a43-4b0e-9d0e-2c1f3e8a9b10";
    private static final String BODY = "{\"eventId\":\"" + EVENT_ID + "\",\"eventType\":\"vehicleOwnerUpdated\","
            + "\"correlationId\":\"" + CORRELATION_ID + "\",\"data\":{\"ownerNationalId\":\"" + DNI
            + "\",\"licensePlate\":\"" + PATENTE + "\"}}";

    private final ObjectMapper mapper = new ObjectMapper();

    private static Message amqp(String body) {
        return new Message(body.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }

    @Test
    void dispatcherNoLogueaPayloadAlIgnorarODescartar(CapturedOutput output) {
        ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
        JsonNode message = mapper.readTree(BODY);

        when(repository.existsById(any())).thenReturn(false);
        new IncomingEventDispatcher(List.of(), repository, java.time.Clock.systemUTC()).dispatch(message);
        when(repository.existsById(any())).thenReturn(true);
        new IncomingEventDispatcher(List.of(), repository, java.time.Clock.systemUTC()).dispatch(message);

        assertThat(output.getAll()).contains(EVENT_ID, "vehicleOwnerUpdated").doesNotContain(DNI, PATENTE);
    }

    @Test
    void listenerRechazaSinRequeueYSinFiltrarDatosPersonales(CapturedOutput output) {
        IncomingEventDispatcher dispatcher = mock(IncomingEventDispatcher.class);
        // Simula un error de deserialización cuyo mensaje incluye valores del payload.
        doThrow(new IllegalArgumentException("Cannot deserialize \"" + PATENTE + "\" de " + DNI))
                .when(dispatcher).dispatch(any());
        RabbitMqIncomingEventListener listener = new RabbitMqIncomingEventListener(dispatcher, mapper);

        assertThatThrownBy(() -> listener.receive(amqp(BODY)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasNoCause()
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain(DNI, PATENTE));

        assertThat(output.getAll())
                .contains("[event-rejected]", "vehicleOwnerUpdated", EVENT_ID, CORRELATION_ID,
                        "IllegalArgumentException")
                .doesNotContain(DNI, PATENTE);
    }

    @Test
    void listenerRechazaJsonInvalidoSinLoguearElCuerpo(CapturedOutput output) {
        IncomingEventDispatcher dispatcher = mock(IncomingEventDispatcher.class);
        RabbitMqIncomingEventListener listener = new RabbitMqIncomingEventListener(dispatcher, mapper);

        assertThatThrownBy(() -> listener.receive(amqp("{\"ownerNationalId\":\"" + DNI + "\",")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasNoCause()
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain(DNI));

        assertThat(output.getAll()).contains("[event-rejected]").doesNotContain(DNI);
    }

    @Test
    void listenerDespachaEventoValidoSinLoguearPayload(CapturedOutput output) throws Exception {
        IncomingEventDispatcher dispatcher = mock(IncomingEventDispatcher.class);

        new RabbitMqIncomingEventListener(dispatcher, mapper).receive(amqp(BODY));

        verify(dispatcher).dispatch(any());
        assertThat(output.getAll()).doesNotContain(DNI, PATENTE);
    }
}
