package ar.edu.uade.transito.transito_backend.listener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;

import ar.edu.uade.transito.transito_backend.model.ProcessedEvent;
import ar.edu.uade.transito.transito_backend.repository.ProcessedEventRepository;

@Component
public class IncomingEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(IncomingEventDispatcher.class);

    private final Map<String, IncomingEventHandler> handlersByType;
    private final ProcessedEventRepository processedEventRepository;

    public IncomingEventDispatcher(List<IncomingEventHandler> handlers, ProcessedEventRepository processedEventRepository) {
        this.handlersByType = handlers.stream()
                .collect(Collectors.toMap(IncomingEventHandler::eventType, Function.identity()));
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public void dispatch(JsonNode message) {
        UUID eventId = UUID.fromString(message.path("eventId").asText());
        String eventType = message.path("eventType").asText();
        UUID correlationId = message.hasNonNull("correlationId")
                ? UUID.fromString(message.path("correlationId").asText())
                : eventId;

        if (processedEventRepository.existsById(eventId)) {
            log.info("[event] eventId={} type={} ya procesado, se descarta", eventId, eventType);
            return;
        }

        IncomingEventHandler handler = handlersByType.get(eventType);
        if (handler == null) {
            log.warn("[event] type={} eventId={} sin handler, se ignora", eventType, eventId);
            return;
        }

        if (handler instanceof ContextualIncomingEventHandler contextualHandler) {
            contextualHandler.handle(message.path("data"), eventId, correlationId);
        } else {
            handler.handle(message.path("data"));
        }

        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .processedAt(LocalDateTime.now())
                .build());
    }
}
