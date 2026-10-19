# Contrato de integración M3 Obras ↔ M7 Tránsito — versión 1.0

Estado: propuesto para congelar con Obras el 19 de octubre de 2026.

## Convenciones comunes

Todos los mensajes usan el envelope Core:

```json
{
  "eventId": "UUID",
  "eventType": "nombreDelEvento",
  "eventVersion": "1.0",
  "occurredAt": "2026-10-20T18:00:00-03:00",
  "sourceModule": "transito",
  "correlationId": "UUID",
  "causationId": "UUID o null",
  "data": {}
}
```

Las fechas y horas de los eventos usan ISO 8601 con offset. Los enums se serializan en MAYÚSCULA. El módulo de origen usa minúscula: `obras` para M3 y `transito` para M7.

En la implementación actual de M7, `correlationId` se genera como UUID al publicar cada evento y `causationId` se emite como `null`. Ambos campos están siempre presentes, conforme al envelope Core.

## Eventos publicados por M7

El alcance de M3 en esta versión es: recibe el resultado de cada solicitud de corte (`streetClosureApproved`, `streetClosureRejected` y `streetClosureEnded`) y los accidentes con daño de infraestructura (`roadAccidentRegistered`). `streetClosureActivated` queda como evento interno de M7 y no se distribuye a Obras en la versión `1.0`.

### `streetClosureApproved`

```json
{
  "eventId": "78cc0f45-62d4-47e7-bf33-37e92447fd52",
  "eventType": "streetClosureApproved",
  "eventVersion": "1.0",
  "occurredAt": "2026-10-20T08:05:00-03:00",
  "sourceModule": "transito",
  "correlationId": "7fd98f1c-72a3-42fb-bbe8-047db769210e",
  "causationId": null,
  "data": {
    "streetClosureId": "c819cfbc-9848-43cc-9172-e79201009126",
    "closureRequestId": "1f915e75-7aa3-4320-8452-0b1b457285db",
    "requestingModule": "Obras",
    "startDate": "2026-10-20T08:00:00-03:00",
    "endDate": "2026-10-20T18:00:00-03:00",
    "affectedStreet": ["Av. Rivadavia entre Av. La Plata y Muñiz (BOTH)"],
    "detours": [{"street": "Av. Rivadavia", "alternateRoute": "Av. Independencia"}],
    "conditions": ["Mantener acceso para emergencias"]
  }
}
```

### `streetClosureRejected`

```json
{
  "eventId": "9b3aa2f7-506b-4974-9194-4a0e2cb1c364",
  "eventType": "streetClosureRejected",
  "eventVersion": "1.0",
  "occurredAt": "2026-10-20T08:05:00-03:00",
  "sourceModule": "transito",
  "correlationId": "7fd98f1c-72a3-42fb-bbe8-047db769210e",
  "causationId": null,
  "data": {
    "closureRequestId": "1f915e75-7aa3-4320-8452-0b1b457285db",
    "rejectionReason": "El corte afectaría una vía principal durante horario pico.",
    "requestingModule": "Obras"
  }
}
```

### `streetClosureEnded`

```json
{
  "eventId": "b6c54f5a-00e8-4327-aa32-fa2aeef7f0ef",
  "eventType": "streetClosureEnded",
  "eventVersion": "1.0",
  "occurredAt": "2026-10-20T18:02:00-03:00",
  "sourceModule": "transito",
  "correlationId": "7fd98f1c-72a3-42fb-bbe8-047db769210e",
  "causationId": null,
  "data": {
    "streetClosureId": "c819cfbc-9848-43cc-9172-e79201009126",
    "closureRequestId": "1f915e75-7aa3-4320-8452-0b1b457285db",
    "completionDateTime": "2026-10-20T18:00:00-03:00",
    "notes": "La circulación fue restablecida normalmente."
  }
}
```

### `roadAccidentRegistered`

M7 lo publica a Obras **únicamente** si `involvesInfrastructureDamage` es `true`. En ese caso `damageDescription` es obligatorio.

```json
{
  "eventId": "5e0e1434-2d0a-411f-aac1-0e6a8b2e42b3",
  "eventType": "roadAccidentRegistered",
  "eventVersion": "1.0",
  "occurredAt": "2026-10-20T18:45:00-03:00",
  "sourceModule": "transito",
  "correlationId": "5e0e1434-2d0a-411f-aac1-0e6a8b2e42b3",
  "causationId": null,
  "data": {
    "accidentId": "8f8d5a28-26d9-431f-9d31-ded3fc114ec6",
    "accidentDateTime": "2026-10-20T18:30:00-03:00",
    "location": {"street": "Av. Rivadavia", "crossStreet": "Medrano", "latitude": -34.6118, "longitude": -58.4214},
    "involvesInfrastructureDamage": true,
    "damageDescription": "Semáforo dañado por impacto.",
    "vehicles": [{"licensePlate": "AC456EF", "ownerNationalId": "38765432"}],
    "persons": [{"nationalId": "38765432", "injuryLevel": "MINOR"}],
    "inspectorId": "INS-124"
  }
}
```

En los mensajes enviados a Obras, `damageDescription` siempre es obligatorio porque `involvesInfrastructureDamage` siempre vale `true`.
