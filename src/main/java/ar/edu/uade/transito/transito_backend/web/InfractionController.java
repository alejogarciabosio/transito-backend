package ar.edu.uade.transito.transito_backend.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.CurrentUserProvider;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.enums.Role;
import ar.edu.uade.transito.transito_backend.service.InfractionService;
import ar.edu.uade.transito.transito_backend.web.dto.AnnulRequest;
import ar.edu.uade.transito.transito_backend.web.dto.AppealRequest;
import ar.edu.uade.transito.transito_backend.web.dto.CreateInfractionRequest;
import ar.edu.uade.transito.transito_backend.web.dto.InfractionResponse;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateInfractionRequest;
import ar.edu.uade.transito.transito_backend.web.mapper.InfractionMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/infractions")
@RequiredArgsConstructor
//Esta clase corresponde al patrón de diseño Controller o  Mediator, ya que actúa como un intermediario entre las solicitudes HTTP entrantes y la lógica de negocio de la aplicación. Su responsabilidad principal es recibir las solicitudes, delegar el procesamiento a los servicios correspondientes y devolver las respuestas adecuadas al cliente. Al utilizar este patrón, se logra una separación clara de responsabilidades, lo que facilita el mantenimiento y la escalabilidad del sistema.
public class InfractionController {

    private final InfractionService infractionService;
    private final CurrentUserProvider currentUserProvider;

    @PreAuthorize("hasRole('INSPECTOR')")
    @PostMapping
    public ResponseEntity<InfractionResponse> register(@Valid @RequestBody CreateInfractionRequest request) {
        Infraction infraction = InfractionMapper.toEntity(request);
        infraction.setInspectorId(currentUserProvider.get().userId());
        Infraction created = infractionService.register(infraction);
        return ResponseEntity.created(URI.create("/api/v1/infractions/" + created.getInfractionId()))
                .body(InfractionMapper.toResponse(created));
    }

    @PreAuthorize("hasAnyRole('INSPECTOR', 'SUPERVISOR', 'AUDITOR')")
    @GetMapping
    public List<InfractionResponse> findAll() {
        return infractionService.findAll().stream().map(InfractionMapper::toResponse).toList();
    }

    @PreAuthorize("hasRole('CIUDADANO')")
    @GetMapping("/mine")
    public List<InfractionResponse> findMine() {
        String nationalId = currentUserProvider.get().nationalId();
        return infractionService.findByOffenderNationalId(nationalId).stream().map(InfractionMapper::toResponse).toList();
    }

    @PreAuthorize("hasAnyRole('INSPECTOR', 'AUDITOR', 'CIUDADANO')")
    @GetMapping("/{id}")
    public InfractionResponse findById(@PathVariable UUID id) {
        Infraction infraction = infractionService.findById(id);
        AuthenticatedUser currentUser = currentUserProvider.get();
        if (currentUser.role() == Role.CIUDADANO
                && !currentUser.nationalId().equals(infraction.getOffenderNationalId())) {
            throw new AccessDeniedException("La infracción no pertenece al ciudadano autenticado");
        }
        return InfractionMapper.toResponse(infraction);
    }

    @PreAuthorize("hasRole('INSPECTOR')")
    @PatchMapping("/{id}")
    public InfractionResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateInfractionRequest request) {
        Infraction updated = infractionService.update(id, InfractionMapper.toEntity(request));
        return InfractionMapper.toResponse(updated);
    }

    @PreAuthorize("hasRole('CIUDADANO')")
    @PostMapping("/{id}/appeal")
    public InfractionResponse appeal(@PathVariable UUID id, @Valid @RequestBody AppealRequest request) {
        Infraction existing = infractionService.findById(id);
        if (!currentUserProvider.get().nationalId().equals(existing.getOffenderNationalId())) {
            throw new AccessDeniedException("La infracción no pertenece al ciudadano autenticado");
        }
        Infraction appealed = infractionService.appeal(id, request.reason());
        return InfractionMapper.toResponse(appealed);
    }

    @PreAuthorize("hasAnyRole('INSPECTOR', 'SUPERVISOR')")
    @PostMapping("/{id}/confirmation")
    public InfractionResponse confirm(@PathVariable UUID id) {
        Infraction confirmed = infractionService.confirm(id);
        return InfractionMapper.toResponse(confirmed);
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PostMapping("/{id}/annulment")
    public InfractionResponse annul(@PathVariable UUID id, @Valid @RequestBody AnnulRequest request) {
        String authorizedBy = currentUserProvider.get().userId();
        Infraction annulled = infractionService.annul(id, request.annulmentReason(), authorizedBy);
        return InfractionMapper.toResponse(annulled);
    }
}
