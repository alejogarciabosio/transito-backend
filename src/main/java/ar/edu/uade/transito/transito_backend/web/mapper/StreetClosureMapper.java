package ar.edu.uade.transito.transito_backend.web.mapper;

import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.web.dto.StreetClosureResponse;

public final class StreetClosureMapper {

    private StreetClosureMapper() {
    }

    public static StreetClosureResponse toResponse(StreetClosure closure) {
        return new StreetClosureResponse(
                closure.getStreetClosureId(),
                closure.getClosureRequestId(),
                closure.getRequestingModule(),
                closure.getStartDate(),
                closure.getEndDate(),
                closure.getAffectedStreet(),
                closure.getDetours(),
                closure.getConditions(),
                closure.getStatus());
    }
}
