package ar.edu.uade.transito.transito_backend.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.transito.transito_backend.auth.CurrentUserProvider;
import ar.edu.uade.transito.transito_backend.model.Detour;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.service.StreetClosureService;
import ar.edu.uade.transito.transito_backend.web.dto.ApproveClosureRequest;
import ar.edu.uade.transito.transito_backend.web.dto.FinishClosureRequest;
import ar.edu.uade.transito.transito_backend.web.dto.RejectClosureRequest;
import ar.edu.uade.transito.transito_backend.web.dto.StreetClosureResponse;
import ar.edu.uade.transito.transito_backend.web.mapper.StreetClosureMapper;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/street-closures")
@RequiredArgsConstructor
public class StreetClosureController {

    private final StreetClosureService streetClosureService;
    private final CurrentUserProvider currentUserProvider;

    @PreAuthorize("hasAnyRole('INSPECTOR', 'OPERADOR_CONTROL', 'SUPERVISOR')")
    @GetMapping
    public List<StreetClosureResponse> findAll() {
        return streetClosureService.findAll().stream().map(StreetClosureMapper::toResponse).toList();
    }

    @PreAuthorize("hasAnyRole('INSPECTOR', 'OPERADOR_CONTROL', 'SUPERVISOR')")
    @GetMapping("/{id}")
    public StreetClosureResponse findById(@PathVariable UUID id) {
        return StreetClosureMapper.toResponse(streetClosureService.findById(id));
    }

    @PreAuthorize("hasAnyRole('INSPECTOR', 'SUPERVISOR')")
    @PostMapping("/{id}/approve")
    public StreetClosureResponse approve(@PathVariable UUID id, @RequestBody ApproveClosureRequest request) {
        StreetClosure changes = StreetClosure.builder()
                .detours(request.detours() != null ? request.detours() : List.<Detour>of())
                .conditions(request.conditions() != null ? request.conditions() : List.<String>of())
                .build();
        return StreetClosureMapper.toResponse(streetClosureService.approve(id, changes));
    }

    @PreAuthorize("hasAnyRole('INSPECTOR', 'SUPERVISOR')")
    @PostMapping("/{id}/reject")
    public StreetClosureResponse reject(@PathVariable UUID id, @RequestBody RejectClosureRequest request) {
        return StreetClosureMapper.toResponse(streetClosureService.reject(id, request.rejectionReason()));
    }

    @PreAuthorize("hasRole('OPERADOR_CONTROL')")
    @PostMapping("/{id}/activate")
    public StreetClosureResponse activate(@PathVariable UUID id) {
        String activatedBy = currentUserProvider.get().userId();
        return StreetClosureMapper.toResponse(streetClosureService.activate(id, activatedBy));
    }

    @PreAuthorize("hasRole('OPERADOR_CONTROL')")
    @PostMapping("/{id}/finish")
    public StreetClosureResponse finish(@PathVariable UUID id, @RequestBody FinishClosureRequest request) {
        return StreetClosureMapper.toResponse(streetClosureService.finish(id, request.notes()));
    }
}
