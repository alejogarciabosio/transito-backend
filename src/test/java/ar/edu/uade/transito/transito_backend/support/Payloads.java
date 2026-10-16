package ar.edu.uade.transito.transito_backend.support;

/** Modelos de prueba (JSON) reutilizados por los tests de integración. */
public final class Payloads {

    private Payloads() {
    }

    public static String infraction(String offenderNationalId) {
        return """
                {
                  "licensePlate": "AB123CD",
                  "infractionType": "SPEEDING",
                  "infractionDateTime": "2026-09-20T10:15:00",
                  "location": {"street": "Av. Corrientes", "crossStreet": "Callao", "latitude": -34.604, "longitude": -58.392},
                  "evidenceUrl": ["https://evidencias/foto1.jpg"],
                  "inspectorId": "u-1001",
                  "offenderNationalId": "%s",
                  "vehicleOwnerTaxId": "20-30777888-1",
                  "debtorId": "%s",
                  "debtorIdType": "DNI",
                  "baseAmount": 50000,
                  "aggravatingFactors": ["zona escolar"],
                  "finalAmount": 75000,
                  "originOperationId": null,
                  "originTicketId": "TCK-1",
                  "originParkingSessionId": null
                }
                """.formatted(offenderNationalId, offenderNationalId);
    }

    public static String infractionUpdate() {
        return """
                {
                  "licensePlate": "ZZ999ZZ",
                  "location": {"street": "Av. Santa Fe", "crossStreet": "Pueyrredón"},
                  "evidenceUrl": ["https://evidencias/foto2.jpg"],
                  "offenderNationalId": "30777888",
                  "vehicleOwnerTaxId": "20-30777888-1",
                  "debtorId": "30777888",
                  "debtorIdType": "DNI",
                  "baseAmount": 60000,
                  "aggravatingFactors": [],
                  "finalAmount": 60000
                }
                """;
    }

    public static String zone(String name) {
        return """
                {"name": "%s", "boundaries": "POLYGON((0 0,1 1,1 0,0 0))", "allowedHours": "08:00-20:00"}
                """.formatted(name);
    }

    public static String tariff(String zoneId) {
        return """
                {"zoneId": "%s", "pricePerHour": 1500, "validFrom": "2026-01-01", "validTo": "2026-12-31"}
                """.formatted(zoneId);
    }

    public static String session(String zoneId) {
        return """
                {"licensePlate": "AB123CD", "zoneId": "%s", "startDateTime": "2026-09-20T09:00:00"}
                """.formatted(zoneId);
    }

    public static String accident() {
        return """
                {
                  "accidentDateTime": "2026-09-20T18:30:00",
                  "location": {"street": "Av. 9 de Julio", "crossStreet": "Belgrano"},
                  "involvesInfrastructureDamage": true,
                  "damageDescription": "Semáforo derribado",
                  "vehicles": [{"licensePlate": "AB123CD", "ownerNationalId": "30777888"}],
                  "persons": [{"nationalId": "30777888", "injuryLevel": "MINOR"}],
                  "inspectorId": "u-1001",
                  "originTicketId": "TCK-9"
                }
                """;
    }

    public static String accidentUpdate(String status) {
        return """
                {
                  "accidentDateTime": "2026-09-20T18:45:00",
                  "location": {"street": "Av. 9 de Julio", "crossStreet": "Moreno"},
                  "involvesInfrastructureDamage": false,
                  "damageDescription": "Sin daños",
                  "vehicles": [],
                  "persons": [],
                  "status": %s
                }
                """.formatted(status == null ? "null" : "\"" + status + "\"");
    }

    public static String operation() {
        return """
                {
                  "type": "ROUTINE_CONTROL",
                  "zone": "Microcentro",
                  "startDateTime": "2026-09-20T08:00:00",
                  "endDateTime": null,
                  "assignedInspectors": ["u-1001"],
                  "assignedUnits": ["MOV-12"],
                  "ticketId": "TCK-5"
                }
                """;
    }

    public static String impoundment(String ownerNationalId) {
        return """
                {
                  "licensePlate": "AB123CD",
                  "ownerNationalId": "%s",
                  "reason": "Alcoholemia positiva",
                  "depotLocation": "Playa Sur",
                  "entryDateTime": "2026-09-20T23:00:00",
                  "originType": "OPERATION",
                  "originId": "5a51284e-8fbe-4ab2-8db4-823d37c27d12"
                }
                """.formatted(ownerNationalId);
    }

    public static String release() {
        return """
                {"verifiedRequirements": ["Licencia vigente", "Seguro vigente"], "towingAndStorageFee": 25000, "paymentReference": "PAY-777"}
                """;
    }
}
