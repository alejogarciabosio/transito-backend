package ar.edu.uade.transito.transito_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A structured street segment received from Obras or Ambiente. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreetAffectedSection {

    @Column(nullable = false)
    private String street;

    @Column(name = "from_street", nullable = false)
    private String from;

    @Column(name = "to_street", nullable = false)
    private String to;

    @Column(nullable = false)
    private String direction;
}
