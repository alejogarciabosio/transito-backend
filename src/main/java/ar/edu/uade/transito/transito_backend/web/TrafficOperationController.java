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

import ar.edu.uade.transito.transito_backend.model.TrafficOperation;
import ar.edu.uade.transito.transito_backend.service.TrafficOperationService;
import ar.edu.uade.transito.transito_backend.web.dto.CloseOperationRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
public class TrafficOperationController {

    private final TrafficOperationService trafficOperationService;

    @PostMapping
    public ResponseEntity<TrafficOperation> create(@RequestBody TrafficOperation operation) {
        TrafficOperation created = trafficOperationService.create(operation);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<TrafficOperation> findAll() {
        return trafficOperationService.findAll();
    }

    @GetMapping("/{id}")
    public TrafficOperation findById(@PathVariable UUID id) {
        return trafficOperationService.findById(id);
    }

    @PatchMapping("/{id}")
    public TrafficOperation update(@PathVariable UUID id, @RequestBody TrafficOperation changes) {
        return trafficOperationService.update(id, changes);
    }

    @PostMapping("/{id}/close")
    public TrafficOperation close(@PathVariable UUID id, @RequestBody CloseOperationRequest request) {
        return trafficOperationService.close(id, request.resolution());
    }
}
