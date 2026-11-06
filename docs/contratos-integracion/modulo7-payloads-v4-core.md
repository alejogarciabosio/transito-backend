# Módulo 7 — catálogo de payloads Core v4

Estado: vigente desde el 19 de octubre de 2026. Este documento reemplaza a `modulo7-payloads-v3-pulidos.md` para integraciones externas; ese archivo queda como historial de definiciones previas.

## Envelope Core obligatorio

Todo evento externo de M7 usa exactamente este sobre:

```json
{
  "eventId": "UUID",
  "eventType": "camelCase",
  "eventVersion": "1.0",
  "occurredAt": "2026-10-20T18:00:00-03:00",
  "sourceModule": "transito",
  "correlationId": "UUID",
  "causationId": "UUID o null",
  "data": {}
}
```

- Fechas: ISO 8601 con offset.
- Enums: MAYÚSCULA, salvo una especificación externa versionada que indique lo contrario.
- `sourceModule`: identificador técnico en minúscula (`transito`, `obras`, `ambiente`, `rentas`).
- `correlationId`: conserva la correlación de origen cuando el evento responde a otro evento.
- `causationId`: contiene el `eventId` que causó la respuesta; es `null` para eventos sin evento causal.

## Catálogo externo de M7

| Dirección | Evento | Destino/origen | Contrato de `data` |
| --- | --- | --- | --- |
| Publica | `infractionConfirmed` | M5 Rentas | `infractionId`, `debtorId`, `debtorIdType`, `licensePlate`, `infractionType`, `infractionDateTime`, `baseAmount`, `aggravatingFactors`, `finalAmount`, `inspectorId`, `location` |
| Publica | `streetClosureApproved` | M3 Obras / M6 Ambiente | `streetClosureId`, `closureRequestId`, `requestingModule` técnico, `startDate`, `endDate`, `affectedSections`, `detours`, `conditions` |
| Publica | `streetClosureRejected` | M3 Obras / M6 Ambiente | `closureRequestId`, `rejectionReason`, `requestingModule` técnico |
| Publica | `streetClosureEnded` | M3 Obras / M6 Ambiente | `streetClosureId`, `closureRequestId`, `completionDateTime`, `notes` |
| Publica | `roadAccidentRegistered` | M3 Obras | `accidentId`, `accidentDateTime`, `location`, `involvesInfrastructureDamage`, `damageDescription`, `vehicles`, `persons`, `inspectorId`; sólo se publica si hay daño de infraestructura |
| Recibe | `streetClosureRequested` | M3 Obras / M6 Ambiente | `closureRequestId`, `previousClosureRequestId` opcional, `sourceModule`, `sourceRef`, `reason`, `affectedSections`, `requestedFrom`, `requestedTo`, `closureType`, `requestedAt` |
| Recibe | `paymentRegistered` | M5 Rentas | `paymentId`, `debtId`, `originType: "TRAFFIC_FINE"`, `originId`, `infractionId`, `amountApplied`, `paidAt`, `remainingBalance` |
| Recibe | `debtSettled` | M5 Rentas | `debtId`, `citizenId` opcional, `conceptCode`, `originType: "TRAFFIC_FINE"`, `originId`, `infractionId`, `settledAt`, `remainingBalance`, `outstandingBalance` |
| Recibe | `paymentReversed` | M5 Rentas | `paymentId`, `debtId`, `originType: "TRAFFIC_FINE"`, `originId`, `infractionId`, `reversedAmount`, `reversedAt`, `remainingBalance`, `reason` |
| Recibe | `ticketUpdated` | M2 Atención Ciudadana | Payload versionado por M2; M7 procesa únicamente los dirigidos al área de Tránsito |

## Cortes de calle: schema acordado M3/M7

### Convención de origen M3/M6

El envelope identifica al productor ante Core con el nombre técnico en minúscula, mientras que `data.sourceModule` identifica al módulo solicitante:

| Módulo solicitante | `sourceModule` del envelope | `data.sourceModule` | `requestingModule` en respuesta M7 |
| --- | --- | --- | --- |
| M3 Obras | `obras` | `M3` | `obras` |
| M6 Ambiente | `ambiente` | `M6` | `ambiente` |

M7 determina quién solicitó el corte leyendo `data.sourceModule`. Las respuestas se correlacionan por `closureRequestId` y conservan el `correlationId` del evento recibido. El envelope de toda respuesta emitida por M7 usa `sourceModule: "transito"`.

`affectedSections` es siempre una lista estructurada; no se usa la variante histórica `affectedStreet` textual:

```json
{
  "street": "Av. Rivadavia",
  "from": "Av. La Plata",
  "to": "Muñiz",
  "direction": "FORWARD | REVERSE | BOTH"
}
```

En `streetClosureRequested`, una prórroga se identifica con `previousClosureRequestId`. Al aprobar o rechazar, M7 conserva el `correlationId` de la solicitud y usa el `eventId` de ésta como `causationId`. En `streetClosureApproved`, `endDate` es el fin operativo autorizado.

Un corte ACTIVE se cierra automáticamente al vencer `endDate` y publica `streetClosureEnded`. Uno APPROVED que no se activó pasa a `EXPIRED` sin publicar finalización. `streetClosureActivated` es interno y no se publica a M3/M6.

Los ejemplos JSON completos —normal, prórroga y respuestas— están en [contrato-m3-m7-v1.md](contrato-m3-m7-v1.md).

### Eventos de Ambiente aún no consumidos por M7

Los catálogos `ServiceOrigin`, `TreeHealthStatus`, `TreeInterventionType` y el campo opcional `location.neighborhoodId` corresponden a eventos de servicios y arbolado de M6 que M7 no consume actualmente. Por lo tanto, sus valores no modifican el contrato de `streetClosureRequested` ni bloquean la integración M3/M6–M7.

## Versionado

- El contrato vigente de Obras/M7 es `eventVersion: "1.0"`.
- Si un campo cambia de tipo, se publica una nueva versión mayor del evento afectado.
- Core debe registrar el nombre correcto `streetClosureEnded`; `streetClousureEnded` es un error histórico.
