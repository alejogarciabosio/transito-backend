package ar.edu.uade.transito.transito_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
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
public class Detour {

    @Column(nullable = false)
    @NotBlank(message = "La calle del desvío es obligatoria")
    private String street;

    @Column(nullable = false)
    @NotBlank(message = "La ruta alternativa es obligatoria")
    private String alternateRoute;
}
