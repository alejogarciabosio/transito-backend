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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.service.InfractionService;
import ar.edu.uade.transito.transito_backend.web.dto.AnnulRequest;
import ar.edu.uade.transito.transito_backend.web.dto.AppealRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/infractions")
@RequiredArgsConstructor
public class InfractionController {

    private final InfractionService infractionService;

    @PostMapping
    public ResponseEntity<Infraction> register(@RequestBody Infraction infraction) {
        Infraction created = infractionService.register(infraction);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<Infraction> findAll() {
        return infractionService.findAll();
    }

    // TODO: reemplazar el query param por el nationalId del ciudadano autenticado una vez esté el JWT.
    @GetMapping("/mine")
    public List<Infraction> findMine(@RequestParam String offenderNationalId) {
        return infractionService.findByOffenderNationalId(offenderNationalId);
    }

    @GetMapping("/{id}")
    public Infraction findById(@PathVariable UUID id) {
        return infractionService.findById(id);
    }

    @PatchMapping("/{id}")
    public Infraction update(@PathVariable UUID id, @RequestBody Infraction changes) {
        return infractionService.update(id, changes);
    }

    @PostMapping("/{id}/appeal")
    public Infraction appeal(@PathVariable UUID id, @RequestBody AppealRequest request) {
        return infractionService.appeal(id, request.reason());
    }

    @PostMapping("/{id}/confirm")
    public Infraction confirm(@PathVariable UUID id) {
        return infractionService.confirm(id);
    }

    @PostMapping("/{id}/annul")
    public Infraction annul(@PathVariable UUID id, @RequestBody AnnulRequest request) {
        return infractionService.annul(id, request.annulmentReason(), request.authorizedBy());
    }
}
