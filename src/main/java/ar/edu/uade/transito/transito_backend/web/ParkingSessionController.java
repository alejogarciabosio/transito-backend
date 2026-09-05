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

import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.ParkingSession;
import ar.edu.uade.transito.transito_backend.service.ParkingSessionService;
import ar.edu.uade.transito.transito_backend.web.dto.FlagUnpaidRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/parking-sessions")
@RequiredArgsConstructor
public class ParkingSessionController {

    private final ParkingSessionService parkingSessionService;

    @PostMapping
    public ResponseEntity<ParkingSession> start(@RequestBody ParkingSession session) {
        ParkingSession created = parkingSessionService.start(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<ParkingSession> findAll() {
        return parkingSessionService.findAll();
    }

    @GetMapping("/{id}")
    public ParkingSession findById(@PathVariable UUID id) {
        return parkingSessionService.findById(id);
    }

    @PatchMapping("/{id}/close")
    public ParkingSession close(@PathVariable UUID id) {
        return parkingSessionService.close(id);
    }

    @PostMapping("/{id}/flag-unpaid")
    public Infraction flagUnpaid(@PathVariable UUID id, @RequestBody FlagUnpaidRequest request) {
        return parkingSessionService.flagUnpaid(id, request.inspectorId(), request.baseAmount());
    }
}
