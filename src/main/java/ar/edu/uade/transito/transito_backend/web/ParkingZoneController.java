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
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.transito.transito_backend.model.ParkingZone;
import ar.edu.uade.transito.transito_backend.service.ParkingZoneService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/parking-zones")
@RequiredArgsConstructor
public class ParkingZoneController {

    private final ParkingZoneService parkingZoneService;

    @PostMapping
    public ResponseEntity<ParkingZone> create(@RequestBody ParkingZone zone) {
        ParkingZone created = parkingZoneService.create(zone);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<ParkingZone> findAll() {
        return parkingZoneService.findAll();
    }

    @GetMapping("/{id}")
    public ParkingZone findById(@PathVariable UUID id) {
        return parkingZoneService.findById(id);
    }

    @PutMapping("/{id}")
    public ParkingZone update(@PathVariable UUID id, @RequestBody ParkingZone changes) {
        return parkingZoneService.update(id, changes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        parkingZoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
