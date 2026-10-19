package ar.edu.uade.transito.transito_backend.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventEnvelope<T> {

    private UUID eventId;

    private String eventType;

    private String eventVersion;

    private OffsetDateTime occurredAt;

    private String sourceModule;

    private UUID correlationId;

    private UUID causationId;

    private T data;

    public static <T> EventEnvelope<T> of(String eventType, T data) {
        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .eventVersion("1.0")
                .occurredAt(OffsetDateTime.now())
                .sourceModule("transito")
                .correlationId(UUID.randomUUID())
                .data(data)
                .build();
    }
}
