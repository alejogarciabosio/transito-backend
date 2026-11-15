package ar.edu.uade.transito.transito_backend.model;

import ar.edu.uade.transito.transito_backend.model.enums.InjuryLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class AccidentPerson {

    @Column(nullable = false)
    @NotBlank(message = "El documento de la persona es obligatorio")
    @Size(max = 255, message = "No puede superar 255 caracteres")
    private String nationalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "El nivel de lesión es obligatorio")
    private InjuryLevel injuryLevel;
}
