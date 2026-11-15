# Contrato de integración M3 Obras ↔ M7 Tránsito — versión 1.1

Estado: acordado por M3 y M7 el 19 de octubre de 2026. Pendiente únicamente la validación de Core para el nombre registrado de `streetClosureEnded`.

**Cambios en v1.1 (15/11/2026):** `streetClosureEnded` informa como `causationId` el `eventId` de la solicitud original (antes `null`), a pedido de Core para mejorar la trazabilidad. El `correlationId` y el payload no cambian. Avisado a M3 y Core en el canal de POs.

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

Para `streetClosureApproved` y `streetClosureRejected`, M7 conserva el `correlationId` recibido en `streetClosureRequested` y usa el `eventId` recibido como `causationId`. Para `streetClosureEnded` (finalización manual o por vencimiento automático) también conserva el mismo `correlationId` y usa como `causationId` el `eventId` de la solicitud original (desde v1.1; en v1.0 era `null`).

## Evento recibido por M7

### `streetClosureRequested` — caso normal

```json
{
  "eventId": "2f4f6f6e-7c10-4e5f-8f89-9c3485d0c214",
  "eventType": "streetClosureRequested",
  "eventVersion": "1.0",
  "occurredAt": "2026-10-17T10:00:00-03:00",
  "sourceModule": "obras",
  "correlationId": "7fd98f1c-72a3-42fb-bbe8-047db769210e",
  "causationId": null,
  "data": {
    "closureRequestId": "1f915e75-7aa3-4320-8452-0b1b457285db",
    "sourceModule": "obras",
    "sourceRef": "OT-2401",
    "reason": "Reparación de calzada",
    "affectedSections": [{"street": "Av. Rivadavia", "from": "Av. La Plata", "to": "Muñiz", "direction": "BOTH"}],
    "requestedFrom": "2026-10-20T08:00:00-03:00",
    "requestedTo": "2026-10-20T18:00:00-03:00",
    "closureType": "PARTIAL",
    "requestedAt": "2026-10-17T10:00:00-03:00"
  }
}
```

### `streetClosureRequested` — prórroga

```json
{
  "eventId": "c8e7b24b-5945-4d91-9828-330182567eab",
  "eventType": "streetClosureRequested",
  "eventVersion": "1.0",
  "occurredAt": "2026-10-20T15:00:00-03:00",
  "sourceModule": "obras",
  "correlationId": "7fd98f1c-72a3-42fb-bbe8-047db769210e",
  "causationId": null,
  "data": {
    "closureRequestId": "c6bfb292-79fb-4f80-8ace-6cbe0aee46bc",
    "previousClosureRequestId": "1f915e75-7aa3-4320-8452-0b1b457285db",
    "sourceModule": "obras",
    "sourceRef": "OT-2401",
    "reason": "Prórroga por demora en la reparación de la calzada",
    "affectedSections": [{"street": "Av. Rivadavia", "from": "Av. La Plata", "to": "Muñiz", "direction": "BOTH"}],
    "requestedFrom": "2026-10-20T18:00:00-03:00",
    "requestedTo": "2026-10-21T02:00:00-03:00",
    "closureType": "PARTIAL",
    "requestedAt": "2026-10-20T15:00:00-03:00"
  }
}
```

`affectedSections` es obligatorio y cada elemento requiere `street`, `from`, `to` y `direction`, con `direction` en `FORWARD`, `REVERSE` o `BOTH`. En una prórroga, `previousClosureRequestId` es obligatorio.

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
  "causationId": "2f4f6f6e-7c10-4e5f-8f89-9c3485d0c214",
  "data": {
    "streetClosureId": "c819cfbc-9848-43cc-9172-e79201009126",
    "closureRequestId": "1f915e75-7aa3-4320-8452-0b1b457285db",
    "requestingModule": "obras",
    "startDate": "2026-10-20T08:00:00-03:00",
    "endDate": "2026-10-20T18:00:00-03:00",
    "affectedSections": [{"street": "Av. Rivadavia", "from": "Av. La Plata", "to": "Muñiz", "direction": "BOTH"}],
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
  "causationId": "2f4f6f6e-7c10-4e5f-8f89-9c3485d0c214",
  "data": {
    "closureRequestId": "1f915e75-7aa3-4320-8452-0b1b457285db",
    "rejectionReason": "El corte afectaría una vía principal durante horario pico.",
    "requestingModule": "obras"
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
  "causationId": "2f4f6f6e-7c10-4e5f-8f89-9c3485d0c214",
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
