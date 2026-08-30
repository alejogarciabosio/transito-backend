# Módulo 7 — Revisión de payloads de eventos

> Revisión sobre el documento "Eventos y contenido" que compartiste. En general el diseño está bien pensado (los campos "NUEVO" que agregó la IA anterior son razonables), pero hay puntos de consistencia y algunos huecos que conviene cerrar antes de darlo por definitivo.

---

## 0. Punto estructural más importante: falta el sobre común del evento

El documento del TPO exige que **todo evento** tenga como mínimo: tipo de evento, ID único, fecha/hora de generación, módulo de origen, y los datos del evento. Ninguno de los payloads que definieron muestra ese "sobre" — todos muestran directamente el contenido (`data`).

Recomendación: envolver todos los payloads así, y documentar los campos de abajo (`infractionId`, `licensePlate`, etc.) como el contenido de `data`:

```json
{
  "eventId": "uuid",
  "eventType": "infractionConfirmed",
  "occurredAt": "2026-08-24T14:30:00-03:00",
  "sourceModule": "transito",
  "data": { "...": "..." }
}
```

Esto es clave sobre todo porque el Core (Módulo 9) actúa como HUB de eventos y necesita ese sobre para enrutar, hacer trazabilidad y manejar la DLQ.

---

## 1. Eventos que publicamos

### `infractionConfirmed` (a Rentas) — bien resuelto
Los campos `debtorId`/`debtorIdType` agregados resuelven bien la ambigüedad entre persona física y organización. Sin objeciones.

### `infractionRegistered` (interno) — falta un campo
Este evento se dispara al crear el acta, pero según la funcionalidad *"Calcular importes y aplicar agravantes o reincidencia"*, el monto ya se calcula en el momento del registro — no recién en la confirmación. Sin embargo, `infractionRegistered` no trae `baseAmount`, `aggravatingFactors` ni `finalAmount`. Sugiero agregarlos acá también (aunque después no cambien en la confirmación), para que cualquier consumidor interno (por ejemplo, la notificación al ciudadano) pueda mostrar el importe desde el momento del acta, no recién cuando esté firme.

### `infractionAppealed` — revisar si `appealId` es necesario
El campo es razonable *si* van a permitir más de una apelación por infracción (reapelaciones). Pero la regla de negocio dice *"una infracción apelada no será firme hasta su resolución"* — no menciona reapelaciones. Si solo permiten **una** apelación por infracción, `infractionId` ya alcanza como identificador único y `appealId` es redundante. Definición pendiente: ¿permiten reapelar o no?

### `infractionAnulled` (interno) — está sin definir
Marcado como "A COMPLETAR". Propuesta mínima:
```
infractionId: string (UUID)
annulmentReason: string
authorizedBy: string   // quién autorizó, según la regla "las anulaciones requerirán autorización"
annulmentDate: datetime
originAppealId: string (opcional, si vino de una apelación aceptada)
```

### `streetClousureEnded` — typo de nombre
Está escrito `streetClousureEnded`, pero el resto de la familia usa `streetClosure...` (`streetClosureApproved`, `streetClosureRejected`). Corregir a `streetClosureEnded` para mantener consistencia — si ya lo usaron así en código, mejor corregirlo ahora que después.

### Falta `streetClosureActivated`
En el listado de eventos que publica el módulo (documento original y el diagrama de tu compañera) aparece `CorteCalleActivado`/`streetClosureActivated`, pero no tiene payload definido acá. Sugerido:
```
streetClosureId: string (UUID)
activationDateTime: datetime
```

### `vehicleImpounded` / `vehicleReleased` — bien resueltos
`originType`/`originId` y `paymentReference` agregados tienen buena justificación (trazabilidad de origen y de pago). Sin objeciones.

Un detalle menor: `paymentReference` en `vehicleReleased` — ¿de dónde sale ese valor? Si viene de un pago registrado por Rentas, van a necesitar el payload de `paymentRegistered` (evento que consumen) para saber qué campo mapear ahí. Ese payload todavía no está definido — lo marco también más abajo.

### `updateTicketStatus` — pendiente definir payload propio
Dijiste que "lo define Módulo 2", pero como **ustedes son quienes lo publican**, el contrato de lo que mandan sí es responsabilidad de este módulo (aunque el nombre/formato general lo haya definido M2 como estándar para todos). Como mínimo necesitarían:
```
ticketId: string
status: string (enum acordado con M2)
message: string (opcional, texto visible al ciudadano)
relatedEntityType: "infraction" | "accident" | "operation" | (opcional)
relatedEntityId: string (opcional)
```

---

## 2. Eventos que recibimos

### Falta `ticketCreated`
Solo está `ticketUpdated` en el listado de recibidos, pero según lo que definimos en esta charla, el disparador inicial de todo el flujo es cuando **Módulo 2 crea el ticket**. Sin `ticketCreated` en el catálogo, no hay ningún evento documentado que explique cómo nace un Operativo. Hay que agregarlo.

### Payloads desconocidos (marcados como "caja negra")
Confirmás que no tienen el contenido de los eventos que consumen — es válido no tenerlo *todavía*, pero hay dos que les conviene priorizar porque afectan directamente lo que ustedes publican:
- **`paymentRegistered`** (Rentas) — necesario para completar `paymentReference` en `vehicleReleased` y para saber cuándo cerrar el circuito financiero de una infracción.
- **`streetClosureRequested`** (Obras/Ambiente) — ya marcaste que necesitan el campo de módulo solicitante; sin el payload confirmado no pueden validar que ese campo realmente venga.

---

## 3. Estacionamiento medido

No requiere eventos nuevos hacia otros módulos — como definimos, reutiliza `infractionRegistered`/`infractionConfirmed` con `infractionType: "unpaid_parking"`. Sí van a necesitar definir el **modelo interno** de `ParkingSession` (no es un evento, es la entidad que persiste el registro de uso), con al menos: `sessionId`, `licensePlate`, `zoneId`, `startDateTime`, `endDateTime`, `paid`, `paymentReference`.

---

## Resumen de acciones sugeridas

1. Definir el sobre común de evento y aplicarlo a todos los payloads.
2. Agregar `baseAmount`/`aggravatingFactors`/`finalAmount` a `infractionRegistered`.
3. Confirmar si permiten reapelaciones (define si `appealId` es necesario).
4. Completar el payload de `infractionAnulled`.
5. Corregir el typo `streetClousureEnded` → `streetClosureEnded`.
6. Agregar payload de `streetClosureActivated`.
7. Definir payload propio de `updateTicketStatus`.
8. Agregar `ticketCreated` a eventos recibidos.
9. Priorizar conseguir los payloads de `paymentRegistered` y `streetClosureRequested`.
10. Documentar el modelo interno de `ParkingSession` (no es evento, pero hace falta igual).
