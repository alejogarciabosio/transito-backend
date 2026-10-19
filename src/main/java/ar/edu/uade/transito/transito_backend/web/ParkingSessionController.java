package ar.edu.uade.transito.transito_backend.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.transito.transito_backend.auth.CurrentUserProvider;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.ParkingSession;
import ar.edu.uade.transito.transito_backend.service.ParkingSessionService;
import ar.edu.uade.transito.transito_backend.web.dto.CreateParkingSessionRequest;
import ar.edu.uade.transito.transito_backend.web.dto.FlagUnpaidRequest;
import ar.edu.uade.transito.transito_backend.web.dto.InfractionResponse;
import ar.edu.uade.transito.transito_backend.web.dto.ParkingSessionResponse;
import ar.edu.uade.transito.transito_backend.web.mapper.InfractionMapper;
import ar.edu.uade.transito.transito_backend.web.mapper.ParkingSessionMapper;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/parking-sessions")
@RequiredArgsConstructor
public class ParkingSessionController {

    private final ParkingSessionService parkingSessionService;
    private final CurrentUserProvider currentUserProvider;

    @PreAuthorize("hasAnyRole('CIUDADANO', 'INSPECTOR')")
    @PostMapping
    public ResponseEntity<ParkingSessionResponse> start(@RequestBody CreateParkingSessionRequest request) {
        ParkingSession created = parkingSessionService.start(ParkingSessionMapper.toEntity(request));
        return ResponseEntity.created(URI.create("/api/v1/parking-sessions/" + created.getSessionId()))
                .body(ParkingSessionMapper.toResponse(created));
    }

    @PreAuthorize("hasAnyRole('INSPECTOR', 'ADMINISTRADOR')")
    @GetMapping
    public List<ParkingSessionResponse> findAll() {
        return parkingSessionService.findAll().stream().map(ParkingSessionMapper::toResponse).toList();
    }

    // NOTA: ParkingSession no guarda ningún dato del titular (solo licensePlate), así que no hay
    // forma de validar "dueño" sin un registro de propiedad de vehículos, que está fuera del
    // alcance de Tránsito. Por ahora cualquier Ciudadano autenticado puede consultar cualquier sesión.
    @PreAuthorize("hasAnyRole('INSPECTOR', 'CIUDADANO')")
    @GetMapping("/{id}")
    public ParkingSessionResponse findById(@PathVariable UUID id) {
        return ParkingSessionMapper.toResponse(parkingSessionService.findById(id));
    }

    @PreAuthorize("hasAnyRole('CIUDADANO', 'INSPECTOR')")
    @PostMapping("/{id}/closure")
    public ParkingSessionResponse close(@PathVariable UUID id) {
        return ParkingSessionMapper.toResponse(parkingSessionService.close(id));
    }

    @PreAuthorize("hasRole('INSPECTOR')")
    @PostMapping("/{id}/unpaid-flag")
    //este es un ejemplo del patrón de diseño Facade ya que el controlador actúa como una interfaz simplificada para el servicio de ParkingSession, ocultando la complejidad de la lógica de negocio y proporcionando un punto de entrada único para las operaciones relacionadas con las sesiones de estacionamiento.
    public InfractionResponse flagUnpaid(@PathVariable UUID id, @RequestBody FlagUnpaidRequest request) {
        String inspectorId = currentUserProvider.get().userId();
        Infraction infraction = parkingSessionService.flagUnpaid(id, inspectorId, request.baseAmount());
        return InfractionMapper.toResponse(infraction);
    }
}
