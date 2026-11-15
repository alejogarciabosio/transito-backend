package ar.edu.uade.transito.transito_backend.event;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import java.nio.charset.StandardCharsets;

/**
 * Formato del mensaje que recibe Core. Antes se armaba con {@code rabbitTemplate.convertAndSend(envelope)}
 * y el {@link JacksonJsonMessageConverter} por defecto; ahora el JSON se genera con ese MISMO conversor
 * al guardar en el outbox y el relay reenvía esos bytes con las mismas propiedades. El contenido que
 * llega a Core es idéntico: solo cambia el momento del envío (segundos después del commit).
 */
public final class CoreMessageFormat {

    static final String TYPE_ID_HEADER = "__TypeId__";

    private static final JacksonJsonMessageConverter CONVERTER = new JacksonJsonMessageConverter();

    private CoreMessageFormat() {
    }

    /** Sobre completo en JSON, byte a byte igual al cuerpo que producía convertAndSend. */
    public static String toJson(EventEnvelope<?> envelope) {
        Message message = CONVERTER.toMessage(envelope, new MessageProperties());
        return new String(message.getBody(), StandardCharsets.UTF_8);
    }

    /** Mensaje AMQP con las mismas propiedades que generaba el conversor + las que agregaba el publicador. */
    static Message toMessage(String json, String moduleUserId) {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setContentLength(body.length);
        properties.setHeader(TYPE_ID_HEADER, EventEnvelope.class.getName());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setUserId(moduleUserId);
        return new Message(body, properties);
    }
}
