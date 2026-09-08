package ar.edu.uade.transito.transito_backend.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.transito.transito_backend.model.Detour;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.service.StreetClosureService;
import ar.edu.uade.transito.transito_backend.web.dto.ApproveClosureRequest;
import ar.edu.uade.transito.transito_backend.web.dto.RejectClosureRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/street-closures")
@RequiredArgsConstructor
public class StreetClosureController {

    private final StreetClosureService streetClosureService;

    @GetMapping
    public List<StreetClosure> findAll() {
        return streetClosureService.findAll();
    }

    @GetMapping("/{id}")
    public StreetClosure findById(@PathVariable UUID id) {
        return streetClosureService.findById(id);
    }

    @PostMapping("/{id}/approve")
    public StreetClosure approve(@PathVariable UUID id, @RequestBody ApproveClosureRequest request) {
        StreetClosure changes = StreetClosure.builder()
                .detours(request.detours() != null ? request.detours() : List.<Detour>of())
                .conditions(request.conditions() != null ? request.conditions() : List.<String>of())
                .build();
        return streetClosureService.approve(id, changes);
    }

    @PostMapping("/{id}/reject")
    public StreetClosure reject(@PathVariable UUID id, @RequestBody RejectClosureRequest request) {
        return streetClosureService.reject(id, request.reason());
    }

    @PostMapping("/{id}/activate")
    public StreetClosure activate(@PathVariable UUID id) {
        return streetClosureService.activate(id);
    }

    @PostMapping("/{id}/finish")
    public StreetClosure finish(@PathVariable UUID id) {
        return streetClosureService.finish(id);
    }
}
