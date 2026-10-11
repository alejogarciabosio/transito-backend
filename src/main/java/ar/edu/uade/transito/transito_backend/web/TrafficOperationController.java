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

import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.service.TrafficOperationService;
import ar.edu.uade.transito.transito_backend.web.dto.CloseOperationRequest;
import ar.edu.uade.transito.transito_backend.web.dto.CreateTrafficOperationRequest;
import ar.edu.uade.transito.transito_backend.web.dto.TrafficOperationResponse;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateTrafficOperationRequest;
import ar.edu.uade.transito.transito_backend.web.mapper.TrafficOperationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/operations")
@RequiredArgsConstructor
public class TrafficOperationController {

    private final TrafficOperationService trafficOperationService;

    @PreAuthorize("hasAnyRole('INSPECTOR', 'OPERADOR_CONTROL')")
    @PostMapping
    public ResponseEntity<TrafficOperationResponse> create(@Valid @RequestBody CreateTrafficOperationRequest request) {
        TrafficOperation created = trafficOperationService.create(TrafficOperationMapper.toEntity(request));
        return ResponseEntity.created(URI.create("/api/v1/operations/" + created.getOperationId()))
                .body(TrafficOperationMapper.toResponse(created));
    }

    @PreAuthorize("hasAnyRole('INSPECTOR', 'SUPERVISOR', 'AUDITOR')")
    @GetMapping
    public List<TrafficOperationResponse> findAll() {
        return trafficOperationService.findAll().stream().map(TrafficOperationMapper::toResponse).toList();
    }

    @PreAuthorize("hasAnyRole('INSPECTOR', 'SUPERVISOR', 'AUDITOR')")
    @GetMapping("/{id}")
    public TrafficOperationResponse findById(@PathVariable UUID id) {
        return TrafficOperationMapper.toResponse(trafficOperationService.findById(id));
    }

    @PreAuthorize("hasRole('INSPECTOR')")
    @PatchMapping("/{id}")
    public TrafficOperationResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTrafficOperationRequest request) {
        TrafficOperation updated = trafficOperationService.update(id, TrafficOperationMapper.toEntity(request));
        return TrafficOperationMapper.toResponse(updated);
    }

    @PreAuthorize("hasRole('INSPECTOR')")
    @PostMapping("/{id}/closure")
    public TrafficOperationResponse close(@PathVariable UUID id, @Valid @RequestBody CloseOperationRequest request) {
        TrafficOperation closed = trafficOperationService.close(id, request.resolution());
        return TrafficOperationMapper.toResponse(closed);
    }
}
