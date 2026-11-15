package ar.edu.uade.transito.transito_backend.unit;

import ar.edu.uade.transito.transito_backend.service.StreetClosureExpirationScheduler;
import ar.edu.uade.transito.transito_backend.service.StreetClosureService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StreetClosureExpirationSchedulerTest {

    @Test
    @DisplayName("TC-BE-SC-U03: si falla un corte, el scheduler sigue con los demás (cada uno en su transacción)")
    void unFalloNoFrenaALosDemas() {
        StreetClosureService service = mock(StreetClosureService.class);
        UUID roto = UUID.randomUUID();
        UUID sano = UUID.randomUUID();
        when(service.findClosuresDueForExpiration(any(OffsetDateTime.class))).thenReturn(List.of(roto, sano));
        when(service.expireClosure(eq(roto), any(OffsetDateTime.class))).thenThrow(new IllegalStateException("x"));

        new StreetClosureExpirationScheduler(service).expireDueClosures();

        verify(service).expireClosure(eq(sano), any(OffsetDateTime.class));
    }
}
