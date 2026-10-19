package ar.edu.uade.transito.transito_backend.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.transito.transito_backend.model.ParkingZone;
import ar.edu.uade.transito.transito_backend.service.ParkingZoneService;
import ar.edu.uade.transito.transito_backend.web.dto.CreateParkingZoneRequest;
import ar.edu.uade.transito.transito_backend.web.dto.ParkingZoneResponse;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateParkingZoneRequest;
import ar.edu.uade.transito.transito_backend.web.mapper.ParkingZoneMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/parking-zones")
@RequiredArgsConstructor
public class ParkingZoneController {

    private final ParkingZoneService parkingZoneService;

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping
    public ResponseEntity<ParkingZoneResponse> create(@Valid @RequestBody CreateParkingZoneRequest request) {
        ParkingZone created = parkingZoneService.create(ParkingZoneMapper.toEntity(request));
        return ResponseEntity.created(URI.create("/api/v1/parking-zones/" + created.getZoneId()))
                .body(ParkingZoneMapper.toResponse(created));
    }

    // GET público (consulta ciudadana) -- permitAll ya configurado en SecurityConfig, sin @PreAuthorize.
    @GetMapping
    public List<ParkingZoneResponse> findAll() {
        return parkingZoneService.findAll().stream().map(ParkingZoneMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ParkingZoneResponse findById(@PathVariable UUID id) {
        return ParkingZoneMapper.toResponse(parkingZoneService.findById(id));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}")
    public ParkingZoneResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateParkingZoneRequest request) {
        ParkingZone updated = parkingZoneService.update(id, ParkingZoneMapper.toEntity(request));
        return ParkingZoneMapper.toResponse(updated);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        parkingZoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
