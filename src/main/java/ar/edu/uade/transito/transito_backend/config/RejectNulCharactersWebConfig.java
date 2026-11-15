package ar.edu.uade.transito.transito_backend.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.jdk.StringDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Rechaza el carácter NUL (U+0000) en cualquier String del cuerpo JSON de las peticiones HTTP.
 * PostgreSQL no admite NUL en columnas de texto y respondía 500 (hallazgo de OWASP ZAP); así la petición falla
 * en la deserialización y el GlobalExceptionHandler devuelve 400 Problem Details. Solo afecta al conversor
 * JSON de lectura de Spring MVC: el ObjectMapper que usan los listeners de RabbitMQ queda igual.
 */
@Configuration
public class RejectNulCharactersWebConfig implements WebMvcConfigurer {

    private final JsonMapper jsonMapper;

    public RejectNulCharactersWebConfig(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        JsonMapper strictMapper = jsonMapper.rebuild()
                .addModule(new SimpleModule("reject-nul-characters")
                        .addDeserializer(String.class, new NulRejectingStringDeserializer()))
                .build();
        // Se agrega primero y SOLO para leer: la escritura (respuestas, /v3/api-docs) sigue con el conversor original.
        converters.add(0, new ReadOnlyJsonConverter(strictMapper));
    }

    static final class ReadOnlyJsonConverter extends JacksonJsonHttpMessageConverter {

        ReadOnlyJsonConverter(JsonMapper mapper) {
            super(mapper);
        }

        @Override
        public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return false;
        }

        @Override
        public boolean canWrite(ResolvableType type, Class<?> valueClass, MediaType mediaType) {
            return false;
        }
    }

    static final class NulRejectingStringDeserializer extends StringDeserializer {

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) {
            String value = super.deserialize(parser, context);
            if (value != null && value.indexOf('\u0000') >= 0) {
                throw context.weirdStringException(value, String.class, "el texto contiene el carácter NUL (U+0000)");
            }
            return value;
        }
    }
}
