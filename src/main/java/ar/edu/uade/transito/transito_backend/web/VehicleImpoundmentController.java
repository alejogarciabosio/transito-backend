package ar.edu.uade.transito.transito_backend.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.CurrentUserProvider;
import ar.edu.uade.transito.transito_backend.model.VehicleImpoundment;
import ar.edu.uade.transito.transito_backend.model.enums.Role;
import ar.edu.uade.transito.transito_backend.service.VehicleImpoundmentService;
import ar.edu.uade.transito.transito_backend.web.dto.CreateVehicleImpoundmentRequest;
import ar.edu.uade.transito.transito_backend.web.dto.ReleaseImpoundmentRequest;
import ar.edu.uade.transito.transito_backend.web.dto.UpdateVehicleImpoundmentRequest;
import ar.edu.uade.transito.transito_backend.web.dto.VehicleImpoundmentResponse;
import ar.edu.uade.transito.transito_backend.web.mapper.VehicleImpoundmentMapper;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/impoundments")
@RequiredArgsConstructor
public class VehicleImpoundmentController {

    private final VehicleImpoundmentService vehicleImpoundmentService;
    private final CurrentUserProvider currentUserProvider;

    @PreAuthorize("hasAnyRole('INSPECTOR', 'PERSONAL_DEPOSITO')")
    @PostMapping
    public ResponseEntity<VehicleImpoundmentResponse> register(@RequestBody CreateVehicleImpoundmentRequest request) {
        VehicleImpoundment created = vehicleImpoundmentService.register(VehicleImpoundmentMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(VehicleImpoundmentMapper.toResponse(created));
    }

    @PreAuthorize("hasAnyRole('INSPECTOR', 'PERSONAL_DEPOSITO', 'SUPERVISOR')")
    @GetMapping
    public List<VehicleImpoundmentResponse> findAll() {
        return vehicleImpoundmentService.findAll().stream().map(VehicleImpoundmentMapper::toResponse).toList();
    }

    @PreAuthorize("hasAnyRole('INSPECTOR', 'PERSONAL_DEPOSITO', 'CIUDADANO')")
    @GetMapping("/{id}")
    public VehicleImpoundmentResponse findById(@PathVariable UUID id) {
        VehicleImpoundment impoundment = vehicleImpoundmentService.findById(id);
        AuthenticatedUser currentUser = currentUserProvider.get();
        if (currentUser.role() == Role.CIUDADANO
                && !currentUser.nationalId().equals(impoundment.getOwnerNationalId())) {
            throw new AccessDeniedException("El vehículo retenido no pertenece al ciudadano autenticado");
        }
        return VehicleImpoundmentMapper.toResponse(impoundment);
    }

    @PreAuthorize("hasRole('PERSONAL_DEPOSITO')")
    @PatchMapping("/{id}")
    public VehicleImpoundmentResponse update(@PathVariable UUID id, @RequestBody UpdateVehicleImpoundmentRequest request) {
        VehicleImpoundment updated = vehicleImpoundmentService.update(id, VehicleImpoundmentMapper.toEntity(request));
        return VehicleImpoundmentMapper.toResponse(updated);
    }

    @PreAuthorize("hasRole('PERSONAL_DEPOSITO')")
    @PostMapping("/{id}/release")
    public VehicleImpoundmentResponse release(@PathVariable UUID id, @RequestBody ReleaseImpoundmentRequest request) {
        String releasedBy = currentUserProvider.get().userId();
        VehicleImpoundment released = vehicleImpoundmentService.release(
                id, request.verifiedRequirements(), request.towingAndStorageFee(), request.paymentReference(), releasedBy);
        return VehicleImpoundmentMapper.toResponse(released);
    }
}
