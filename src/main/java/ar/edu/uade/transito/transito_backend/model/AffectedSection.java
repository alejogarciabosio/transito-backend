package ar.edu.uade.transito.transito_backend.model;

import ar.edu.uade.transito.transito_backend.model.enums.AffectedSectionDirection;
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
public class AffectedSection {

    @Column(name = "street", nullable = false)
    private String street;

    @Column(name = "section_from", nullable = false)
    private String from;

    @Column(name = "section_to", nullable = false)
    private String to;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private AffectedSectionDirection direction;
}
