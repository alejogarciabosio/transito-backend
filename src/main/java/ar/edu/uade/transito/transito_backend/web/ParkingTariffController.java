package ar.edu.uade.transito.transito_backend.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/parking-tariffs")
@RequiredArgsConstructor
public class ParkingTariffController {

    private final ParkingTariffService parkingTariffService;

    @PostMapping
    public ResponseEntity<ParkingTariff> create(@RequestBody ParkingTariff tariff) {
        ParkingTariff created = parkingTariffService.create(tariff);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<ParkingTariff> findByZoneId(@RequestParam UUID zoneId) {
        return parkingTariffService.findByZoneId(zoneId);
    }

    @GetMapping("/{id}")
    public ParkingTariff findById(@PathVariable UUID id) {
        return parkingTariffService.findById(id);
    }

    @PutMapping("/{id}")
    public ParkingTariff update(@PathVariable UUID id, @RequestBody ParkingTariff changes) {
        return parkingTariffService.update(id, changes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        parkingTariffService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
