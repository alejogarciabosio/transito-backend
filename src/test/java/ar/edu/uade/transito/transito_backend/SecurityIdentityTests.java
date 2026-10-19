package ar.edu.uade.transito.transito_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.model.RoadAccident;
import ar.edu.uade.transito.transito_backend.repository.RoadAccidentRepository;
import ar.edu.uade.transito.transito_backend.service.RoadAccidentService;

class SecurityIdentityTests {

    @Test
    void accidentRegistrationUsesAuthenticatedInspectorInsteadOfEntityValue() {
        RoadAccidentRepository repository = mock(RoadAccidentRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> {
            RoadAccident accident = invocation.getArgument(0);
            accident.setAccidentId(UUID.randomUUID());
            return accident;
        });
        RoadAccidentService service = new RoadAccidentService(repository, mock(EventPublisher.class));
        RoadAccident accident = RoadAccident.builder()
                .accidentDateTime(LocalDateTime.now().minusMinutes(10))
                .inspectorId("usuario-falsificado")
                .build();

        RoadAccident saved = service.register(accident, "inspector-jwt-1");

        assertEquals("inspector-jwt-1", saved.getInspectorId());
        assertNotNull(saved.getAccidentId());
    }

}
