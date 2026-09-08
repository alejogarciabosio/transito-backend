package ar.edu.uade.transito.transito_backend.web;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.transito.transito_backend.model.ParkingTariff;
import ar.edu.uade.transito.transito_backend.service.ParkingTariffService;
import ar.edu.uade.transito.transito_backend.web.dto.CreateParkingTariffRequest;
import ar.edu.uade.transito.transito_backend.web.dto.ParkingTariffResponse;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateParkingTariffRequest;
import ar.edu.uade.transito.transito_backend.web.mapper.ParkingTariffMapper;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/parking-tariffs")
@RequiredArgsConstructor
public class ParkingTariffController {

    private final ParkingTariffService parkingTariffService;

    @PreAuthorize("hasRole('ADMIN_MUNICIPAL')")
    @PostMapping
    public ResponseEntity<ParkingTariffResponse> create(@RequestBody CreateParkingTariffRequest request) {
        ParkingTariff created = parkingTariffService.create(ParkingTariffMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ParkingTariffMapper.toResponse(created));
    }

    // GET público (consulta ciudadana) -- permitAll ya configurado en SecurityConfig, sin @PreAuthorize.
    @GetMapping
    public List<ParkingTariffResponse> findByZoneId(@RequestParam UUID zoneId) {
        return parkingTariffService.findByZoneId(zoneId).stream().map(ParkingTariffMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ParkingTariffResponse findById(@PathVariable UUID id) {
        return ParkingTariffMapper.toResponse(parkingTariffService.findById(id));
    }

    @PreAuthorize("hasRole('ADMIN_MUNICIPAL')")
    @PutMapping("/{id}")
    public ParkingTariffResponse update(@PathVariable UUID id, @RequestBody UpdateParkingTariffRequest request) {
        ParkingTariff updated = parkingTariffService.update(id, ParkingTariffMapper.toEntity(request));
        return ParkingTariffMapper.toResponse(updated);
    }

    @PreAuthorize("hasRole('ADMIN_MUNICIPAL')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        parkingTariffService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
