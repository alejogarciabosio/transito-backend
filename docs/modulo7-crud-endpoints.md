# Módulo 7 — Tránsito, Estacionamiento y Seguridad Vial
## Entidades y métodos CRUD base

> Base de partida. Se ajustará a medida que se defina el modelo de datos definitivo, contratos de eventos que faltan (estacionamiento) y feedback del equipo.

---

## 1. TrafficOperation (Operativo)

Contenedor de verificación: nace de un ticket derivado por Módulo 2, de una denuncia, o de un control de rutina. Es el paso previo obligatorio antes de crear una Infracción (salvo excepciones documentadas).

**Campos principales**
`operationId`, `type` (`routine_control` | `complaint_verification` | `safety_inspection`), `zone`, `startDateTime`, `endDateTime`, `assignedInspectors[]`, `assignedUnits[]`, `ticketId` (opcional, si nace de un reclamo), `status` (`open` | `closed`), `resolution` (`infraction_created` | `accident_registered` | `no_action`)

| Método | Endpoint | Rol | Descripción | Eventos que dispara |
|---|---|---|---|---|
| POST | `/operations` | Inspector, Operador de control | Crea un operativo, manual o a partir de un ticket recibido | `trafficOperationCreated` |
| GET | `/operations` | Inspector, Supervisor, Auditor | Lista con filtros (zona, tipo, estado, fecha, ticketId) | — |
| GET | `/operations/{id}` | Inspector, Supervisor, Auditor | Detalle del operativo | — |
| PATCH | `/operations/{id}` | Inspector asignado | Actualiza datos mientras esté abierto (unidades, inspectores, notas) | — |
| POST | `/operations/{id}/close` | Inspector asignado | Cierra el operativo con su resolución; dispara la creación de Infraction o RoadAccident según corresponda | — (el evento lo dispara la entidad resultante) |

No tiene DELETE físico — un operativo cerrado queda como registro histórico/evidencia.

---

## 2. Infraction (Infracción)

**Campos principales**
`infractionId`, `licensePlate`, `infractionType` (enum, incluye `unpaid_parking` para lo de estacionamiento), `infractionDateTime`, `location {street, crossStreet, latitude, longitude}`, `evidenceUrl[]`, `inspectorId`, `offenderNationalId`, `vehicleOwnerTaxId`, `debtorId`, `debtorIdType`, `baseAmount`, `aggravatingFactors[]`, `finalAmount`, `status` (`pending_appeal` | `confirmed` | `appealed` | `annulled`), `originOperationId`, `originTicketId`

| Método | Endpoint | Rol | Descripción | Eventos que dispara |
|---|---|---|---|---|
| POST | `/infractions` | Inspector (vía cierre de operativo) | Crea el acta de infracción | `infractionRegistered` |
| GET | `/infractions` | Inspector, Supervisor, Auditor | Lista con filtros (tipo, estado, patente, fecha) | — |
| GET | `/infractions/mine` | Ciudadano | Lista solo las infracciones propias (por `offenderNationalId`) | — |
| GET | `/infractions/{id}` | Inspector, Auditor, Ciudadano dueño | Detalle del acta | — |
| PATCH | `/infractions/{id}` | Inspector asignado | Corrige datos **solo mientras no esté confirmada/cerrada** | — |
| POST | `/infractions/{id}/appeal` | Ciudadano | Presenta descargo | `infractionAppealed` |
| POST | `/infractions/{id}/confirm` | Inspector, Supervisor | Resuelve el descargo (o vencido el plazo sin descargo) confirmando la infracción | `infractionConfirmed` (a Rentas) |
| POST | `/infractions/{id}/annul` | Supervisor (requiere autorización) | Anula la infracción con motivo | `infractionAnulled` (interno) |

Reglas aplicadas: sin DELETE (regla "los inspectores no podrán modificar actas cerradas"); PATCH bloqueado una vez `confirmed` o `annulled`.

---

## 3. RoadAccident (Accidente vial)

**Campos principales**
`accidentId`, `accidentDateTime`, `location {...}`, `involvesInfrastructureDamage`, `damageDescription`, `vehicles[] {licensePlate, ownerNationalId}`, `persons[] {nationalId, injuryLevel}`, `inspectorId`, `status` (`preliminary` | `verified`), `originTicketId`

| Método | Endpoint | Rol | Descripción | Eventos que dispara |
|---|---|---|---|---|
| POST | `/accidents` | Inspector, Operador (registro preliminar desde ticket) | Crea el registro, puede empezar incompleto | `roadAccidentRegistered` |
| GET | `/accidents` | Inspector, Supervisor, Auditor | Lista con filtros | — |
| GET | `/accidents/{id}` | Inspector, Supervisor, Auditor | Detalle | — |
| PATCH | `/accidents/{id}` | Inspector asignado | Completa/corrige datos hasta que se marca `verified` | — (si el cambio es relevante para Obras, podría re-emitir el evento; a definir) |

Sin DELETE — es evidencia histórica.

---

## 4. StreetClosure (Corte de calle)

**Campos principales**
`streetClosureId`, `closureRequestId`, `requestingModule` (`Obras` | `Ambiente`), `startDate`, `endDate`, `affectedStreet`, `detours[] {street, alternateRoute}`, `conditions[]`, `status` (`requested` | `approved` | `rejected` | `active` | `finished`)

| Método | Endpoint | Rol | Descripción | Eventos que dispara |
|---|---|---|---|---|
| — | (creación) | — | Se crea al **consumir** `streetClosureRequested` (M3/M6), no vía POST del FE | — |
| GET | `/street-closures` | Inspector, Operador, Supervisor | Lista con filtros (módulo solicitante, estado, fecha) | — |
| GET | `/street-closures/{id}` | Inspector, Operador, Supervisor | Detalle | — |
| POST | `/street-closures/{id}/approve` | Inspector, Supervisor | Aprueba y define desvíos/condiciones | `streetClosureApproved` |
| POST | `/street-closures/{id}/reject` | Inspector, Supervisor | Rechaza con motivo | `streetClosureRejected` |
| POST | `/street-closures/{id}/activate` | Operador de control | Marca el corte como activo en el momento pactado | `streetClosureActivated` *(agregar al catálogo si no está)* |
| POST | `/street-closures/{id}/finish` | Operador de control | Cierra el corte | `streetClosureEnded` |

---

## 5. VehicleImpoundment (Vehículo retenido)

**Campos principales**
`impoundmentId`, `licensePlate`, `ownerNationalId`, `reason`, `depotLocation`, `entryDateTime`, `originType` (`infraction` | `operation`), `originId`, `status` (`impounded` | `released`)

| Método | Endpoint | Rol | Descripción | Eventos que dispara |
|---|---|---|---|---|
| POST | `/impoundments` | Inspector, Personal de depósito | Registra el ingreso de un vehículo retenido | `vehicleImpounded` |
| GET | `/impoundments` | Inspector, Personal de depósito, Supervisor | Lista con filtros | — |
| GET | `/impoundments/{id}` | Inspector, Personal de depósito, Ciudadano dueño | Detalle | — |
| PATCH | `/impoundments/{id}` | Personal de depósito | Corrige datos mientras siga retenido | — |
| POST | `/impoundments/{id}/release` | Personal de depósito | Valida requisitos y pago, libera el vehículo | `vehicleReleased` |

Sin DELETE — historial de retenciones.

---

## 6. Estacionamiento medido (dominio nuevo)

### 6.1 ParkingZone

**Campos**: `zoneId`, `name`, `boundaries` (o referencia geográfica), `allowedHours`, `status` (`active` | `inactive`)

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| POST | `/parking-zones` | Admin municipal | Alta de zona |
| GET | `/parking-zones` | Todos los roles internos, Ciudadano (consulta pública) | Listado |
| GET | `/parking-zones/{id}` | — | Detalle |
| PUT | `/parking-zones/{id}` | Admin municipal | Modificación completa |
| DELETE | `/parking-zones/{id}` | Admin municipal | Baja (lógica, no física, si hay sesiones históricas asociadas) |

### 6.2 ParkingTariff

**Campos**: `tariffId`, `zoneId`, `pricePerHour`, `validFrom`, `validTo`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| POST | `/parking-tariffs` | Admin municipal | Alta de tarifa |
| GET | `/parking-tariffs` | Todos, Ciudadano | Listado (filtro por zona) |
| GET | `/parking-tariffs/{id}` | — | Detalle |
| PUT | `/parking-tariffs/{id}` | Admin municipal | Modificación |
| DELETE | `/parking-tariffs/{id}` | Admin municipal | Baja (solo si no hay sesiones activas dependientes) |

### 6.3 ParkingSession

**Campos**: `sessionId`, `licensePlate`, `zoneId`, `startDateTime`, `endDateTime`, `paid` (bool), `paymentReference`

| Método | Endpoint | Rol | Descripción | Eventos que dispara |
|---|---|---|---|---|
| POST | `/parking-sessions` | Ciudadano (app), Operador | Inicia una sesión de estacionamiento | — |
| GET | `/parking-sessions` | Inspector, Operador, Supervisor | Lista con filtros (zona, patente, pagada/no pagada) | — |
| GET | `/parking-sessions/{id}` | Inspector, Ciudadano dueño | Detalle | — |
| PATCH | `/parking-sessions/{id}/close` | Ciudadano, Operador | Finaliza la sesión | — |
| POST | `/parking-sessions/{id}/flag-unpaid` | Inspector | Detecta falta de pago; crea una `Infraction` con `infractionType = "unpaid_parking"` referenciando `sessionId` | `infractionRegistered` (reutiliza el circuito de Infracciones) |

---

## Notas generales de diseño

- **Ninguna entidad tiene DELETE físico salvo el dominio de estacionamiento** (zonas/tarifas de configuración administrativa). Todo lo que es evidencia legal (infracciones, accidentes, operativos, retenciones) se **anula o cierra**, nunca se borra — trazabilidad y auditoría.
- **Control de acceso por rol** debe aplicarse en la capa de presentación (qué botones/endpoints ve cada rol) y **validarse también en la capa de negocio** (nunca confiar solo en el FE).
- Los endpoints marcados con eventos deben publicar **después** de persistir el cambio en la capa de acceso a datos (para no publicar un evento de algo que después falla al guardarse).
- Pendiente de definición conjunta: el evento `streetClosureActivated` no estaba en el catálogo original que compartiste — lo agrego acá como necesario para el ciclo de vida completo del corte, a confirmar con Obras/Ambiente si les interesa consumirlo.
