package ar.edu.uade.transito.transito_backend.web;

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
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/infractions")
@RequiredArgsConstructor
public class InfractionController {

    private final InfractionService infractionService;
    private final CurrentUserProvider currentUserProvider;

    @PreAuthorize("hasRole('INSPECTOR')")
    @PostMapping
    public ResponseEntity<InfractionResponse> register(@RequestBody CreateInfractionRequest request) {
        Infraction created = infractionService.register(InfractionMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(InfractionMapper.toResponse(created));
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
    public InfractionResponse update(@PathVariable UUID id, @RequestBody UpdateInfractionRequest request) {
        Infraction updated = infractionService.update(id, InfractionMapper.toEntity(request));
        return InfractionMapper.toResponse(updated);
    }

    @PreAuthorize("hasRole('CIUDADANO')")
    @PostMapping("/{id}/appeal")
    public InfractionResponse appeal(@PathVariable UUID id, @RequestBody AppealRequest request) {
        Infraction existing = infractionService.findById(id);
        if (!currentUserProvider.get().nationalId().equals(existing.getOffenderNationalId())) {
            throw new AccessDeniedException("La infracción no pertenece al ciudadano autenticado");
        }
        Infraction appealed = infractionService.appeal(id, request.reason());
        return InfractionMapper.toResponse(appealed);
    }

    @PreAuthorize("hasAnyRole('INSPECTOR', 'SUPERVISOR')")
    @PostMapping("/{id}/confirm")
    public InfractionResponse confirm(@PathVariable UUID id) {
        Infraction confirmed = infractionService.confirm(id);
        return InfractionMapper.toResponse(confirmed);
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PostMapping("/{id}/annul")
    public InfractionResponse annul(@PathVariable UUID id, @RequestBody AnnulRequest request) {
        Infraction annulled = infractionService.annul(id, request.annulmentReason(), request.authorizedBy());
        return InfractionMapper.toResponse(annulled);
    }
}
