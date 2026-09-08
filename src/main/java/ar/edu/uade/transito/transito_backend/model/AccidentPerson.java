package ar.edu.uade.transito.transito_backend.model;

import ar.edu.uade.transito.transito_backend.model.enums.InjuryLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    private String nationalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InjuryLevel injuryLevel;
}
