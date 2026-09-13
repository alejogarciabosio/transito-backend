# Módulo 7 — Revisión de payloads v2.0

> Compara el documento nuevo contra los 10 puntos pendientes de la revisión anterior (`modulo7-revision-payloads.md`) y señala hallazgos nuevos.

---

## Resueltos correctamente ✅

| # | Pendiente anterior | Estado |
|---|---|---|
| 4 | Completar `infractionAnulled` | ✅ Resuelto — `infractionId`, `appealId` (condicional), `annulmentReason`, `annulledBy`, `annulmentDateTime`. Bien pensado el `appealId` condicional (solo si la anulación vino de una apelación aceptada). |
| 5 | Typo `streetClousureEnded` | ✅ Corregido a `streetClosureEnded` en todo el documento. |
| 6 | Payload de `streetClosureActivated` | ✅ Definido — aunque ver observación nueva más abajo sobre a quién se publica. |
| 7 | Payload propio de `updateTicketStatus` | ✅ Muy completo: `updateType` (enum con 6 valores), separación clara entre `publicMessage` (visible al ciudadano) e `internalMessage` (privado), `progress`, `details` condicional. Buen diseño. |
| 9 | Payloads de `paymentRegistered` y `streetClosureRequested` | ✅ Ambos definidos con ejemplos reales (incluso mostrando el caso desde Obras y desde Ambiente por separado). |
| 8 (parcial) | Gap de `ticketCreated` | Ver hallazgo importante abajo — se resuelve de una forma distinta a la que yo había sugerido, y mejor. |

---

## Hallazgo importante: no hace falta `ticketCreated` — ya está resuelto de otra forma

Mirá el ejemplo de `ticketUpdated` que reciben: tiene `updateType: "ROUTED"` con el detalle completo del reclamo (tipo, descripción, ubicación, formData). Es decir, **la primera vez que a Tránsito le llega un ticket, no es un evento aparte llamado "creado" — es un `ticketUpdated` con `updateType: "ROUTED"`**, que trae toda la información inicial.

Esto es mejor que lo que yo había sugerido (agregar un evento `ticketCreated` separado): Módulo 2 unifica todo bajo un solo tipo de evento y diferencia el momento por `updateType`. Lo único que hay que garantizar en el código es que **`ROUTED` se trate como "nace un caso nuevo, crear Operativo"**, distinto de los demás `updateType` (que actualizan un caso ya existente). Vale la pena confirmarlo con el equipo de Módulo 2 para que no haya dudas, pero como está documentado, ya cierra.

---

## Pendiente sin resolver

### `infractionRegistered` sigue sin `baseAmount`/`aggravatingFactors`/`finalAmount`
Repito la observación anterior: si el importe se calcula al momento de labrar el acta (según la funcionalidad *"calcular importes y aplicar agravantes"*), debería estar disponible desde el registro, no recién en la confirmación. Sigue siendo un hueco.

---

## Hallazgos nuevos

### 1. Tres formatos de "sobre" de evento distintos, conviviendo en el mismo catálogo

- **Estándar** (la mayoría de los eventos): `{ eventId, eventType, occurredAt, sourceModule, data }`
- **Formato "ticket"** (`updateTicketStatus` / `ticketUpdated`): `{ specVersion, eventId, eventType, occurredAt, producer: {moduleId, service}, subject, data }` — parece inspirado en el estándar CloudEvents. Como se usa consistentemente en ambas direcciones con Módulo 2, probablemente sea un acuerdo específico para eventos de tickets — no es necesariamente un error, pero conviene **documentarlo explícitamente como una excepción intencional**, para que nadie en el equipo lo confunda con un descuido.
- **Formato "Rentas"** (`paymentRegistered`, `debtSettled`, `paymentReversed`): usa `timestamp` en vez de `occurredAt`, y `payload` en vez de `data`. Esto sí parece una inconsistencia real — o Rentas todavía no alineó su formato al estándar acordado, o es un error de transcripción en el documento. Vale la pena confirmarlo con ellos antes de programar el parser, porque si el nombre del campo cambia entre `data` y `payload`, el código de deserialización se rompe.

### 2. La identidad del módulo se representa de 3 formas distintas en el mismo flujo

En `streetClosureRequested`, contás con:
- `sourceModule: "obras"` (envelope, minúscula, nombre completo)
- `data.sourceModule: "M3"` (adentro del data, código corto)
- Y más adelante, cuando ustedes publican `streetClosureApproved`, necesitan `requestingModule: "Obras"` (mayúscula inicial, nombre completo)

Tres representaciones (`obras`, `M3`, `Obras`) para el mismo concepto. No es un error grave, pero sí un punto donde conviene armar una función/diccionario de mapeo único (`M3` ↔ `Obras` ↔ `obras`) en vez de tres if/else sueltos por el código — así evitan bugs de casing o de comparación de strings.

### 3. `streetClosureActivated` está marcado como "interno" — ¿es correcto?

A diferencia de `streetClosureApproved`/`Rejected`/`Ended` (que sí se publican a Obras/Ambiente), `streetClosureActivated` quedó documentado en la sección de "Eventos internos", es decir, **no se le informa a Obras/Ambiente cuándo el corte realmente se activó**, solo cuándo fue aprobado (con una fecha planificada) y cuándo terminó. Si el corte se demora en activarse respecto a lo planificado, Obras/Ambiente no se enterarían del desfasaje. Vale la pena preguntar: ¿fue una decisión deliberada, o se les pasó por alto que este evento probablemente también les interese a los módulos solicitantes?

### 4. ¿Qué hace Tránsito con los eventos de "aviso anticipado" de Obras/Ambiente?

`urbanServiceScheduled`, `treeRiskDetected`, `treePruningScheduled`, `workOrderScheduled` — todos están documentados con el detalle de qué traen, pero ninguno tiene definida **la acción de negocio** que dispara en Tránsito. `workOrderScheduled` da una pista (*"para... evaluar si requieren coordinación o un corte de calle"*), pero no hay ninguna entidad en el modelo de datos que represente ese "quedó anotado, por las dudas". ¿Es simple registro pasivo para consulta/dashboard (F7), o esperan que dispare alguna acción concreta (por ejemplo, generar una alerta interna o pre-cargar un borrador de corte de calle)? Conviene definir esto antes de programarlo, porque si es solo lectura, alcanza con persistirlo tal cual llega; si dispara una acción, hace falta una entidad y un caso de uso.

### 5. Duplicación dentro del documento mismo

`infractionRegistered`, `infractionAppealed`, `trafficOperationCreated`, `vehicleImpounded`, `vehicleReleased` aparecen documentados **dos veces** en el archivo (una vez en "Eventos que publicamos" y de nuevo en "Eventos internos" al final), con el mismo contenido. No es un error de diseño, pero sí de prolijidad del documento — probablemente por juntar el aporte de dos compañeros sin deduplicar. Vale la pena limpiarlo para que no genere confusión sobre si son eventos distintos o el mismo repetido.

---

## Resumen de acciones sugeridas

1. Agregar `baseAmount`/`aggravatingFactors`/`finalAmount` a `infractionRegistered` (pendiente de la revisión anterior).
2. Confirmar con Rentas si su envelope (`timestamp`/`payload`) es definitivo o hay que alinearlo al estándar del resto.
3. Documentar explícitamente el formato "ticket" como una excepción intencional del sobre estándar.
4. Armar un mapeo único de identidad de módulo (`M3`/`Obras`/`obras`) en vez de manejarlo suelto en el código.
5. Confirmar si `streetClosureActivated` debería publicarse también a Obras/Ambiente.
6. Definir qué acción de negocio (si alguna) disparan los eventos de aviso anticipado (`urbanServiceScheduled`, `treeRiskDetected`, `treePruningScheduled`, `workOrderScheduled`).
7. Deduplicar la sección "Eventos internos" del documento, que repite contenido ya definido antes.
