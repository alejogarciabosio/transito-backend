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

import ar.edu.uade.transito.transito_backend.model.RoadAccident;
import ar.edu.uade.transito.transito_backend.service.RoadAccidentService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accidents")
@RequiredArgsConstructor
public class RoadAccidentController {

    private final RoadAccidentService roadAccidentService;

    @PostMapping
    public ResponseEntity<RoadAccident> register(@RequestBody RoadAccident accident) {
        RoadAccident created = roadAccidentService.register(accident);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<RoadAccident> findAll() {
        return roadAccidentService.findAll();
    }

    @GetMapping("/{id}")
    public RoadAccident findById(@PathVariable UUID id) {
        return roadAccidentService.findById(id);
    }

    @PatchMapping("/{id}")
    public RoadAccident update(@PathVariable UUID id, @RequestBody RoadAccident changes) {
        return roadAccidentService.update(id, changes);
    }
}
