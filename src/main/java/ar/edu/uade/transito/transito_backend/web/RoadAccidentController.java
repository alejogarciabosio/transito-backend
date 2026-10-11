package ar.edu.uade.transito.transito_backend.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.transito.transito_backend.auth.CurrentUserProvider;
import ar.edu.uade.transito.transito_backend.model.RoadAccident;
import ar.edu.uade.transito.transito_backend.service.RoadAccidentService;
import ar.edu.uade.transito.transito_backend.web.dto.CreateRoadAccidentRequest;
import ar.edu.uade.transito.transito_backend.web.dto.RoadAccidentResponse;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateRoadAccidentRequest;
import ar.edu.uade.transito.transito_backend.web.mapper.RoadAccidentMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/accidents")
@RequiredArgsConstructor
public class RoadAccidentController {

    private final RoadAccidentService roadAccidentService;
    private final CurrentUserProvider currentUserProvider;

    @PreAuthorize("hasAnyRole('INSPECTOR', 'OPERADOR_CONTROL')")
    @PostMapping
    public ResponseEntity<RoadAccidentResponse> register(@Valid @RequestBody CreateRoadAccidentRequest request) {
        RoadAccident created = roadAccidentService.register(
                RoadAccidentMapper.toEntity(request), currentUserProvider.get().userId());
        return ResponseEntity.created(URI.create("/api/v1/accidents/" + created.getAccidentId()))
                .body(RoadAccidentMapper.toResponse(created));
    }

    @PreAuthorize("hasAnyRole('INSPECTOR', 'SUPERVISOR', 'AUDITOR')")
    @GetMapping
    public List<RoadAccidentResponse> findAll() {
        return roadAccidentService.findAll().stream().map(RoadAccidentMapper::toResponse).toList();
    }

    @PreAuthorize("hasAnyRole('INSPECTOR', 'SUPERVISOR', 'AUDITOR')")
    @GetMapping("/{id}")
    public RoadAccidentResponse findById(@PathVariable UUID id) {
        return RoadAccidentMapper.toResponse(roadAccidentService.findById(id));
    }

    @PreAuthorize("hasRole('INSPECTOR')")
    @PatchMapping("/{id}")
    public RoadAccidentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateRoadAccidentRequest request) {
        RoadAccident updated = roadAccidentService.update(id, RoadAccidentMapper.toCommand(request));
        return RoadAccidentMapper.toResponse(updated);
    }
}
