package ar.edu.uade.transito.transito_backend.listener;

import org.springframework.stereotype.Component;

import ar.edu.uade.transito.transito_backend.listener.dto.StreetClosureRequestedPayload;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.enums.RequestingModule;
import ar.edu.uade.transito.transito_backend.service.StreetClosureService;
import lombok.RequiredArgsConstructor;

/**
 * Escucha streetClosureRequested (Obras/Ambiente), vía el Core. Mismo caso que
 * {@link TicketEventListener}: hoy nada invoca a {@link #onStreetClosureRequested}, queda listo
 * para cuando haya un broker real conectado.
 */
@Component
@RequiredArgsConstructor
public class StreetClosureEventListener {

    private final StreetClosureService streetClosureService;

    // TODO: reemplazar por @KafkaListener/@RabbitListener una vez definido el broker con Core.
    public void onStreetClosureRequested(StreetClosureRequestedPayload payload) {
        StreetClosure closure = StreetClosure.builder()
                .closureRequestId(payload.closureRequestId())
                .requestingModule(RequestingModule.fromCode(payload.sourceModule()))
                .startDate(payload.requestedFrom())
                .endDate(payload.requestedTo())
                .build();

        streetClosureService.createFromRequest(closure);
    }
}
