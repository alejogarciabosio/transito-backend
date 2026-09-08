package ar.edu.uade.transito.transito_backend.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.transito.transito_backend.model.VehicleImpoundment;
import ar.edu.uade.transito.transito_backend.service.VehicleImpoundmentService;
import ar.edu.uade.transito.transito_backend.web.dto.ReleaseImpoundmentRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/impoundments")
@RequiredArgsConstructor
public class VehicleImpoundmentController {

    private final VehicleImpoundmentService vehicleImpoundmentService;

    @PostMapping
    public ResponseEntity<VehicleImpoundment> register(@RequestBody VehicleImpoundment impoundment) {
        VehicleImpoundment created = vehicleImpoundmentService.register(impoundment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<VehicleImpoundment> findAll() {
        return vehicleImpoundmentService.findAll();
    }

    @GetMapping("/{id}")
    public VehicleImpoundment findById(@PathVariable UUID id) {
        return vehicleImpoundmentService.findById(id);
    }

    @PatchMapping("/{id}")
    public VehicleImpoundment update(@PathVariable UUID id, @RequestBody VehicleImpoundment changes) {
        return vehicleImpoundmentService.update(id, changes);
    }

    @PostMapping("/{id}/release")
    public VehicleImpoundment release(@PathVariable UUID id, @RequestBody ReleaseImpoundmentRequest request) {
        return vehicleImpoundmentService.release(id, request.paymentReference());
    }
}
