# Guía de integración histórica — Backend Tránsito para el Frontend

> **Documento archivado:** conserva el contrato anterior sin versionado para trazabilidad. El contrato vigente y la tabla final de endpoints se encuentran en `docs/actividad-api-rest-entrega.md` y usan el prefijo `/api/v1`.

---

## 0. CORS

El backend ya tiene CORS configurado para el origen **`http://localhost:5173`** (el de Vite en local). Cualquier otro origen recibe `403`. Ojo con dos detalles: `http://127.0.0.1:5173` cuenta como un origen **distinto** a `localhost`, y si Vite corre en otro puerto (por ejemplo `5174` porque el `5173` estaba ocupado) tampoco va a estar permitido.

Para el frontend **desplegado**, o para otro origen local, avisen la URL exacta (sin barra final): se agrega con la variable de entorno `CORS_ALLOWED_ORIGINS` (varias URLs separadas por coma), sin tocar código.

## 1. URLs base

| Ambiente | URL |
|---|---|
| Local (Docker) | `http://localhost:8080` |
| Test (Railway, rama `develop`) | la del environment `test` en Railway — confirmar en el dashboard |
| Producción (Railway, rama `main`) | `https://transito-backend-production-28d6.up.railway.app` |

Todos los endpoints de este documento son relativos a esa URL base (ej. `POST /auth/login` = `POST http://localhost:8080/auth/login`).

---

## 2. Autenticación

### 2.1 Login

```
POST /auth/login
Content-Type: application/json
```

**Body:**
```json
{ "username": "string", "password": "string" }
```

**Response (200):**
```json
{ "token": "string (JWT)", "role": "INSPECTOR", "expiresInSeconds": 7200 }
```

Guardar el `token` (ej. en memoria o `localStorage`) y el `role` (para mostrar/ocultar botones según lo que ese usuario puede hacer). El token expira en `expiresInSeconds` segundos — hoy no hay refresh token, así que al expirar hay que loguear de nuevo.

**Usuarios de prueba disponibles hoy** (hardcodeados en el backend, van a cambiar cuando se conecte el módulo de Usuarios real):

| username | password | role |
|---|---|---|
| `inspector1` | `inspector123` | `INSPECTOR` |
| `supervisor1` | `supervisor123` | `SUPERVISOR` |
| `auditor1` | `auditor123` | `AUDITOR` |
| `operador1` | `operador123` | `OPERADOR_CONTROL` |
| `deposito1` | `deposito123` | `PERSONAL_DEPOSITO` |
| `admin1` | `admin123` | `ADMIN_MUNICIPAL` |
| `ciudadano1` | `ciudadano123` | `CIUDADANO` |

### 2.2 Cómo mandar el token

En **todos** los demás endpoints (salvo los marcados como públicos más abajo), agregar el header:

```
Authorization: Bearer <token>
```

Si falta o es inválido → `403`. Si el rol del usuario no alcanza para ese endpoint → también `403`.

### 2.3 Endpoints públicos (no requieren login)

- `GET /actuator/health`
- `GET /parking-zones` y `GET /parking-zones/{id}`
- `GET /parking-tariffs` y `GET /parking-tariffs/{id}`

---

## 3. Formato de error

Cualquier error (404, 409, 400, 401, 403) devuelve este mismo formato — útil para mostrar mensajes genéricos de error en el frontend:

```json
{
  "timestamp": "2026-09-18T19:24:37Z",
  "status": 404,
  "error": "Not Found",
  "message": "Infraction no encontrada: <uuid>"
}
```

| Código | Cuándo pasa |
|---|---|
| `400` | El JSON está mal formado, o un valor no coincide con ningún valor de un enum. |
| `401` | Login con usuario/contraseña incorrectos. |
| `403` | Sin token, token inválido/vencido, o rol sin permiso para ese endpoint. |
| `404` | El recurso pedido no existe. |
| `409` | Se intentó una transición de estado inválida (ej. confirmar una infracción ya anulada). |

---

## 4. Enums usados en la API

El frontend tiene que mandar/mostrar estos valores **exactamente así** (mayúscula, con guion bajo):

| Enum | Valores |
|---|---|
| `Role` | `INSPECTOR`, `SUPERVISOR`, `AUDITOR`, `OPERADOR_CONTROL`, `PERSONAL_DEPOSITO`, `ADMIN_MUNICIPAL`, `CIUDADANO` |
| `OperationType` | `ROUTINE_CONTROL`, `COMPLAINT_VERIFICATION`, `SAFETY_INSPECTION` |
| `OperationStatus` | `OPEN`, `CLOSED` |
| `OperationResolution` | `INFRACTION_CREATED`, `ACCIDENT_REGISTERED`, `NO_ACTION` |
| `InfractionType` | `UNPAID_PARKING`, `SPEEDING`, `RED_LIGHT`, `ILLEGAL_PARKING`, `NO_LICENSE`, `RECKLESS_DRIVING`, `OTHER` |
| `InfractionStatus` | `PENDING_APPEAL`, `CONFIRMED`, `APPEALED`, `ANNULLED` |
| `DebtorIdType` | `DNI`, `CUIT` |
| `AccidentStatus` | `PRELIMINARY`, `VERIFIED` |
| `InjuryLevel` | `NONE`, `MINOR`, `SEVERE` |
| `ImpoundmentOriginType` | `INFRACTION`, `OPERATION` |
| `ImpoundmentStatus` | `IMPOUNDED`, `RELEASED` |
| `RequestingModule` | `OBRAS`, `AMBIENTE` |
| `StreetClosureStatus` | `REQUESTED`, `APPROVED`, `REJECTED`, `ACTIVE`, `FINISHED` |
| `ParkingZoneStatus` | `ACTIVE`, `INACTIVE` |

**Objetos que se repiten en varios lugares:**
```ts
Location = { street: string, crossStreet?: string, latitude?: number, longitude?: number }
Detour = { street: string, alternateRoute: string }
```

---

## 5. Endpoints por entidad

> Formato: `Método Path` — Rol requerido — qué manda / qué devuelve. Los campos con `?` son opcionales/pueden venir `null`.

### 5.1 Operativos (`/operations`)

| | |
|---|---|
| `POST /operations` | `INSPECTOR`, `OPERADOR_CONTROL` |
| `GET /operations` | `INSPECTOR`, `SUPERVISOR`, `AUDITOR` |
| `GET /operations/{id}` | `INSPECTOR`, `SUPERVISOR`, `AUDITOR` |
| `PATCH /operations/{id}` | `INSPECTOR` (solo si sigue `OPEN`) |
| `POST /operations/{id}/close` | `INSPECTOR` (solo si sigue `OPEN`) |

**Body `POST /operations`:** `{ type, zone, startDateTime, endDateTime?, assignedInspectors: string[], assignedUnits: string[], ticketId? }`
**Body `PATCH /operations/{id}`:** `{ zone, endDateTime?, assignedInspectors, assignedUnits }`
**Body `POST /operations/{id}/close`:** `{ resolution }`
**Response:** todo lo anterior + `operationId`, `status`, `resolution`.

> Nota: cerrar un operativo **no** crea automáticamente la infracción/accidente resultante — hoy son 2 pasos separados.

### 5.2 Infracciones (`/infractions`)

| | |
|---|---|
| `POST /infractions` | `INSPECTOR` |
| `GET /infractions` | `INSPECTOR`, `SUPERVISOR`, `AUDITOR` |
| `GET /infractions/mine` | `CIUDADANO` (trae solo las del usuario logueado, sin parámetros) |
| `GET /infractions/{id}` | `INSPECTOR`, `AUDITOR`, o el `CIUDADANO` dueño |
| `PATCH /infractions/{id}` | `INSPECTOR` (solo si no está `CONFIRMED`/`ANNULLED`) |
| `POST /infractions/{id}/appeal` | `CIUDADANO` dueño |
| `POST /infractions/{id}/confirm` | `INSPECTOR`, `SUPERVISOR` |
| `POST /infractions/{id}/annul` | `SUPERVISOR` |

**Body `POST /infractions`:** `{ licensePlate, infractionType, infractionDateTime, location, evidenceUrl?: string[], inspectorId, offenderNationalId?, vehicleOwnerTaxId?, debtorId?, debtorIdType?, baseAmount, aggravatingFactors?: string[], finalAmount, originOperationId?, originTicketId?, originParkingSessionId? }`
**Body `PATCH /infractions/{id}`:** igual, sin `infractionType`/`inspectorId`/orígenes (no se pueden cambiar).
**Body `.../appeal`:** `{ reason }`
**Body `.../annul`:** `{ annulmentReason, authorizedBy }`
**Body `.../confirm`:** sin body.
**Response:** todos los campos + `infractionId`, `status`.

### 5.3 Accidentes (`/accidents`)

| | |
|---|---|
| `POST /accidents` | `INSPECTOR`, `OPERADOR_CONTROL` |
| `GET /accidents` | `INSPECTOR`, `SUPERVISOR`, `AUDITOR` |
| `GET /accidents/{id}` | `INSPECTOR`, `SUPERVISOR`, `AUDITOR` |
| `PATCH /accidents/{id}` | `INSPECTOR` (solo si no está `VERIFIED`) |

**Body `POST/PATCH`:** `{ accidentDateTime, location, involvesInfrastructureDamage: boolean, damageDescription?, vehicles: {licensePlate, ownerNationalId?}[], persons: {nationalId, injuryLevel}[], inspectorId, originTicketId? }` (`PATCH` además puede mandar `status: "VERIFIED"` para cerrarlo).
**Response:** todo lo anterior + `accidentId`, `status`.

### 5.4 Vehículos retenidos (`/impoundments`)

| | |
|---|---|
| `POST /impoundments` | `INSPECTOR`, `PERSONAL_DEPOSITO` |
| `GET /impoundments` | `INSPECTOR`, `PERSONAL_DEPOSITO`, `SUPERVISOR` |
| `GET /impoundments/{id}` | `INSPECTOR`, `PERSONAL_DEPOSITO`, o el `CIUDADANO` dueño |
| `PATCH /impoundments/{id}` | `PERSONAL_DEPOSITO` (solo si sigue `IMPOUNDED`) |
| `POST /impoundments/{id}/release` | `PERSONAL_DEPOSITO` |

**Body `POST`:** `{ licensePlate, ownerNationalId, reason, depotLocation, entryDateTime, originType, originId }`
**Body `PATCH`:** `{ reason, depotLocation, ownerNationalId }`
**Body `.../release`:** `{ verifiedRequirements: string[], towingAndStorageFee, paymentReference? }` — `releasedBy` **no** se manda, lo toma el backend del usuario logueado.
**Response:** todo lo anterior + `impoundmentId`, `status`, `verifiedRequirements`, `towingAndStorageFee`, `releasedBy`.

### 5.5 Cortes de calle (`/street-closures`)

**No tiene `POST` de creación** — nace de un evento que consume Tránsito (todavía no conectado), no de una pantalla.

| | |
|---|---|
| `GET /street-closures` | `INSPECTOR`, `OPERADOR_CONTROL`, `SUPERVISOR` |
| `GET /street-closures/{id}` | `INSPECTOR`, `OPERADOR_CONTROL`, `SUPERVISOR` |
| `POST /street-closures/{id}/approve` | `INSPECTOR`, `SUPERVISOR` (solo si `REQUESTED`) |
| `POST /street-closures/{id}/reject` | `INSPECTOR`, `SUPERVISOR` (solo si `REQUESTED`) |
| `POST /street-closures/{id}/activate` | `OPERADOR_CONTROL` (solo si `APPROVED`) |
| `POST /street-closures/{id}/finish` | `OPERADOR_CONTROL` (solo si `ACTIVE`) |

**Body `.../approve`:** `{ detours: Detour[], conditions: string[] }`
**Body `.../reject`:** `{ rejectionReason }`
**Body `.../finish`:** `{ notes }`
**Body `.../activate`:** sin body (`activatedBy` sale del usuario logueado).
**Response:** `streetClosureId`, `closureRequestId`, `requestingModule`, `startDate`, `endDate`, `affectedStreet: string[]`, `detours`, `conditions`, `status`.

### 5.6 Zonas de estacionamiento (`/parking-zones`)

| | |
|---|---|
| `POST /parking-zones` | `ADMIN_MUNICIPAL` |
| `GET /parking-zones`, `GET /parking-zones/{id}` | **Público**, sin login |
| `PUT /parking-zones/{id}` | `ADMIN_MUNICIPAL` |
| `DELETE /parking-zones/{id}` | `ADMIN_MUNICIPAL` (baja lógica, pasa a `INACTIVE`) |

**Body `POST`:** `{ name, boundaries?, allowedHours }`
**Body `PUT`:** `{ name, boundaries?, allowedHours, status }`
**Response:** `zoneId`, `name`, `boundaries`, `allowedHours`, `status`.

### 5.7 Tarifas de estacionamiento (`/parking-tariffs`)

| | |
|---|---|
| `POST /parking-tariffs` | `ADMIN_MUNICIPAL` |
| `GET /parking-tariffs?zoneId={uuid}` | **Público** — nota: el `GET` de listado **requiere** el query param `zoneId`, no lista todas |
| `GET /parking-tariffs/{id}` | **Público** |
| `PUT /parking-tariffs/{id}` | `ADMIN_MUNICIPAL` |
| `DELETE /parking-tariffs/{id}` | `ADMIN_MUNICIPAL` (bloqueado si hay sesiones sin cerrar en esa zona → `409`) |

**Body `POST`:** `{ zoneId, pricePerHour, validFrom, validTo? }`
**Body `PUT`:** `{ pricePerHour, validFrom, validTo? }`
**Response:** `tariffId`, `zoneId`, `pricePerHour`, `validFrom`, `validTo`.

### 5.8 Sesiones de estacionamiento (`/parking-sessions`)

| | |
|---|---|
| `POST /parking-sessions` | `CIUDADANO`, `OPERADOR_CONTROL` |
| `GET /parking-sessions` | `INSPECTOR`, `OPERADOR_CONTROL`, `SUPERVISOR` |
| `GET /parking-sessions/{id}` | `INSPECTOR`, `CIUDADANO` (⚠️ hoy **cualquier** ciudadano logueado puede ver cualquier sesión — no hay forma de validar dueño todavía, ver el `.md` de arquitectura) |
| `PATCH /parking-sessions/{id}/close` | `CIUDADANO`, `OPERADOR_CONTROL` |
| `POST /parking-sessions/{id}/flag-unpaid` | `INSPECTOR` — devuelve una `InfractionResponse` (crea la infracción por no pago), no una sesión |

**Body `POST`:** `{ licensePlate, zoneId, startDateTime }`
**Body `.../flag-unpaid`:** `{ baseAmount }`
**Body `.../close`:** sin body.
**Response (sesión):** `sessionId`, `licensePlate`, `zoneId`, `startDateTime`, `endDateTime`, `paid: boolean`, `paymentReference`.

---

## 6. Resumen para armar el login del frontend

1. Pantalla de login → `POST /auth/login` → guardar `token` + `role`.
2. Guardar el `token` y mandarlo en `Authorization: Bearer <token>` en **todos** los pedidos siguientes (salvo los públicos de la sección 2.3).
3. Usar el `role` guardado para decidir qué botones/pantallas mostrar — comparando contra la tabla de roles de cada endpoint arriba. Igual, aunque el frontend oculte un botón, el backend lo va a rechazar con `403` si el rol no corresponde — es una doble validación, no hace falta que el frontend sea perfecto en esto.
4. Si cualquier pedido devuelve `403`, lo más probable es que el token haya vencido (`expiresInSeconds`) → volver a loguear.
