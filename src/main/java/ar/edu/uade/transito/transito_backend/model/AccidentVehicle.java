package ar.edu.uade.transito.transito_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccidentVehicle {

    @Column(nullable = false)
    @NotBlank(message = "La patente del vehículo es obligatoria")
    @Size(max = 255, message = "No puede superar 255 caracteres")
    private String licensePlate;

    @Size(max = 255, message = "No puede superar 255 caracteres")
    private String ownerNationalId;
}
