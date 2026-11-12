package ar.edu.uade.transito.transito_backend.event;

import java.util.List;

/** Events that M7's V4.1 integration contract declares as external Core publications. */
final class CorePublishedEventTypes {

    static final List<String> ALL = List.of(
            "infractionConfirmed",
            "streetClosureApproved",
            "streetClosureRejected",
            "streetClosureEnded",
            "roadAccidentRegistered",
            "updateTicketStatus");

    private CorePublishedEventTypes() {
    }

    static boolean contains(String eventType) {
        return ALL.contains(eventType);
    }
}
