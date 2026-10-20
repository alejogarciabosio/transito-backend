# UNIVERSIDAD ARGENTINA DE LA EMPRESA

## Facultad de Ingeniería y Ciencias Exactas

**Ingeniería en Informática**  
**Desarrollo de Aplicaciones II — Grupo 9**  
**Actividad API REST — versión vigente al 20 de octubre de 2026**

**Profesor:** Ruiz, Matías Gabriel

**Integrantes:** Abdala, Fernando · Abrego, Lautaro Elías · Fabian, Romina Betiana · García, Alejo

---

## 1. Alcance

Este documento describe la API REST que está implementada en M7 — Tránsito,
Estacionamiento y Seguridad Vial. Los controladores y DTOs del backend son la fuente de
verdad; las evoluciones pendientes se señalan de forma explícita.

- URL local: `http://localhost:8080`
- Prefijo obligatorio: `/api/v1`
- Swagger UI: `/swagger-ui/index.html`
- OpenAPI generado: `/v3/api-docs`

La API tiene JWT, autorización por rol, validaciones de entrada, controles de titularidad,
errores `application/problem+json`, SOAP configurable con Usuarios y RabbitMQ configurable
con Core.

## 2. Códigos HTTP generales

| Código | Significado |
|---|---|
| `200` | Consulta o acción exitosa. |
| `201` | Recurso creado; incluye encabezado `Location`. |
| `204` | Eliminación exitosa de zona o tarifa. |
| `400` | Request inválido, JSON mal formado o validación incumplida. |
| `401` | Credenciales incorrectas, token ausente, inválido o vencido. |
| `403` | Rol sin permiso o ciudadano que accede a un recurso ajeno. |
| `404` | Recurso inexistente. |
| `409` | Transición de estado inválida. |
| `502` | SOAP de Usuarios respondió Fault, HTTP no exitoso o XML inválido. |
| `503` | Timeout o indisponibilidad de Usuarios. |
| `500` | Error no previsto. |

No están implementados `429`, `Retry-After`, paginación, filtros, hipermedia ni
`Idempotency-Key`.

## 3. Convenciones y seguridad

- Roles existentes: `ADMINISTRADOR`, `INSPECTOR` y `CIUDADANO`.
- Token: `Authorization: Bearer <jwt>`, con vigencia de dos horas.
- Los errores usan Problem Details: `type`, `title`, `status`, `detail` e `instance`.
- `inspectorId`, quien anula una infracción y quien libera un vehículo se obtienen del JWT,
  no del body.
- Los endpoints de dominio usan `LocalDateTime` o `LocalDate`; los eventos Core usan ISO 8601
  con offset.

## 4. Endpoints

### 4.1 Autenticación

| Método | URI | Acceso | Request | Respuesta |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Público | `{ "username"?: "...", "dni"?: "...", "password": "..." }` | `200 LoginResponse`; `400`, `401`, `502`, `503` |

Si `dni` está presente se utiliza como identidad; de lo contrario se usa `username`.

### 4.2 Operativos

| Método | URI | Rol | Request |
|---|---|---|---|
| `POST` | `/api/v1/operations` | INSPECTOR | `CreateTrafficOperationRequest` |
| `GET` | `/api/v1/operations` | INSPECTOR, ADMINISTRADOR | — |
| `GET` | `/api/v1/operations/{id}` | INSPECTOR, ADMINISTRADOR | UUID path |
| `PATCH` | `/api/v1/operations/{id}` | INSPECTOR | `UpdateTrafficOperationRequest` |
| `POST` | `/api/v1/operations/{id}/closure` | INSPECTOR | `{ "resolution": "..." }` |

### 4.3 Infracciones

| Método | URI | Rol | Request |
|---|---|---|---|
| `POST` | `/api/v1/infractions` | INSPECTOR | `CreateInfractionRequest` |
| `GET` | `/api/v1/infractions` | INSPECTOR, ADMINISTRADOR | — |
| `GET` | `/api/v1/infractions/mine` | CIUDADANO | — |
| `GET` | `/api/v1/infractions/{id}` | INSPECTOR, ADMINISTRADOR o titular | UUID path |
| `PATCH` | `/api/v1/infractions/{id}` | INSPECTOR | `UpdateInfractionRequest` |
| `POST` | `/api/v1/infractions/{id}/appeal` | CIUDADANO titular | `{ "reason": "..." }` |
| `POST` | `/api/v1/infractions/{id}/confirmation` | ADMINISTRADOR | sin body |
| `POST` | `/api/v1/infractions/{id}/annulment` | ADMINISTRADOR | `{ "annulmentReason": "..." }` |

`confirmation` sólo admite infracciones `PENDING_APPEAL` o `APPEALED`. Cambia el estado a
`CONFIRMED` y emite `infractionConfirmed` para Rentas.

### 4.4 Accidentes viales

| Método | URI | Rol | Request |
|---|---|---|---|
| `POST` | `/api/v1/accidents` | INSPECTOR | `CreateRoadAccidentRequest` |
| `GET` | `/api/v1/accidents` | INSPECTOR, ADMINISTRADOR | — |
| `GET` | `/api/v1/accidents/mine` | CIUDADANO | — |
| `GET` | `/api/v1/accidents/{id}` | INSPECTOR, ADMINISTRADOR | UUID path |
| `PATCH` | `/api/v1/accidents/{id}` | INSPECTOR | `UpdateRoadAccidentRequest` |

Cuando `involvesInfrastructureDamage` vale `true`, `damageDescription` es obligatorio. Sólo
en ese caso se publica `roadAccidentRegistered` a Obras.

### 4.5 Vehículos retenidos

| Método | URI | Rol | Request |
|---|---|---|---|
| `POST` | `/api/v1/impoundments` | INSPECTOR | `CreateVehicleImpoundmentRequest` |
| `GET` | `/api/v1/impoundments` | INSPECTOR, ADMINISTRADOR | — |
| `GET` | `/api/v1/impoundments/mine` | CIUDADANO | — |
| `GET` | `/api/v1/impoundments/{id}` | INSPECTOR, ADMINISTRADOR o titular | UUID path |
| `PATCH` | `/api/v1/impoundments/{id}` | ADMINISTRADOR | `UpdateVehicleImpoundmentRequest` |
| `POST` | `/api/v1/impoundments/{id}/release` | ADMINISTRADOR | `ReleaseImpoundmentRequest` |

Liberación: `verifiedRequirements` no puede ser vacío; `towingAndStorageFee` es obligatorio;
si el costo es mayor a cero, también lo es `paymentReference`.

### 4.6 Cortes de calle

Los cortes se crean sólo a partir de `streetClosureRequested` de Obras/Ambiente, no mediante
un endpoint REST de alta.

| Método | URI | Rol | Request |
|---|---|---|---|
| `GET` | `/api/v1/street-closures` | INSPECTOR, ADMINISTRADOR | — |
| `GET` | `/api/v1/street-closures/current` | Público | — |
| `GET` | `/api/v1/street-closures/{id}` | INSPECTOR, ADMINISTRADOR | UUID path |
| `POST` | `/api/v1/street-closures/{id}/approval` | ADMINISTRADOR | `ApproveClosureRequest` |
| `POST` | `/api/v1/street-closures/{id}/rejection` | ADMINISTRADOR | `{ "rejectionReason": "..." }` |
| `POST` | `/api/v1/street-closures/{id}/activation` | INSPECTOR | sin body |
| `POST` | `/api/v1/street-closures/{id}/completion` | INSPECTOR | `{ "notes": "..." }` |

En aprobación, `detours` y `conditions` son obligatorios; `authorizedFrom` y `authorizedTo`
son opcionales. El fin autorizado gobierna el cierre automático. Un corte `ACTIVE` publica
`streetClosureEnded` al finalizar; uno aprobado que nunca se activó pasa a `EXPIRED` sin
publicar finalización. `streetClosureActivated` es interno de M7.

### 4.7 Estacionamiento medido

| Método | URI | Rol | Request |
|---|---|---|---|
| `POST` | `/api/v1/parking-zones` | ADMINISTRADOR | `CreateParkingZoneRequest` |
| `GET` | `/api/v1/parking-zones` | Público | — |
| `GET` | `/api/v1/parking-zones/{id}` | Público | UUID path |
| `PUT` | `/api/v1/parking-zones/{id}` | ADMINISTRADOR | `UpdateParkingZoneRequest` |
| `DELETE` | `/api/v1/parking-zones/{id}` | ADMINISTRADOR | — |
| `POST` | `/api/v1/parking-tariffs` | ADMINISTRADOR | `CreateParkingTariffRequest` |
| `GET` | `/api/v1/parking-tariffs?zoneId={uuid}` | Público | `zoneId` query |
| `GET` | `/api/v1/parking-tariffs/{id}` | Público | UUID path |
| `PUT` | `/api/v1/parking-tariffs/{id}` | ADMINISTRADOR | `UpdateParkingTariffRequest` |
| `DELETE` | `/api/v1/parking-tariffs/{id}` | ADMINISTRADOR | — |
| `POST` | `/api/v1/parking-sessions` | CIUDADANO, INSPECTOR | `CreateParkingSessionRequest` |
| `GET` | `/api/v1/parking-sessions` | INSPECTOR, ADMINISTRADOR | — |
| `GET` | `/api/v1/parking-sessions/{id}` | CIUDADANO, INSPECTOR | UUID path |
| `POST` | `/api/v1/parking-sessions/{id}/closure` | CIUDADANO, INSPECTOR | sin body |
| `POST` | `/api/v1/parking-sessions/{id}/unpaid-flag` | INSPECTOR | `{ "baseAmount": 0 }` |

## 5. Ejemplos JSON

### Registro de infracción

```json
{
  "licensePlate": "AB123CD",
  "infractionType": "ILLEGAL_PARKING",
  "infractionDateTime": "2026-10-20T14:30:00",
  "location": {"street": "Av. Cabildo", "crossStreet": "Juramento", "latitude": -34.5621, "longitude": -58.4563},
  "evidenceUrl": ["https://evidencias.example/acta-123.jpg"],
  "offenderNationalId": "40123456",
  "debtorId": "40123456",
  "debtorIdType": "DNI",
  "baseAmount": 50000,
  "aggravatingFactors": ["REINCIDENCE"],
  "finalAmount": 75000,
  "originOperationId": null,
  "originTicketId": null,
  "originParkingSessionId": null
}
```

### Error de transición

```json
{
  "type": "https://transito.uade.edu.ar/problems/invalid-state-transition",
  "title": "Conflict",
  "status": 409,
  "detail": "No se puede confirmar una infracción en estado ANNULLED",
  "instance": "/api/v1/infractions/93fd4382-2343-4f3f-a127-9328b29fc520/confirmation"
}
```

## 6. Integraciones

### Usuarios — SOAP síncrono

`SoapIdentityValidator` implementa `validateUser` con `dni`, `password`, `user_id`, `role`
y `auth`, con timeout de cinco segundos. Se activa con `USERS_SOAP_ENABLED=true` y
`USERS_SOAP_URL`. La URL final y la prueba bilateral con M1 aún son externas; mientras tanto
se utiliza el proveedor local de desarrollo.

### Core/Rentas — mensajería asíncrona

Al confirmar una infracción, M7 publica `infractionConfirmed` en el envelope Core `1.0` al
exchange `muni.inbox` cuando RabbitMQ está habilitado. Faltan credenciales, allowlist de IP y
la prueba M7 → Core → M5. Core/M5 también deben confirmar publicación y suscripción activas.

## 7. Estado y hoja de ruta

- Implementado: API `/api/v1`, JWT, roles, autorizaciones, validaciones, Problem Details,
  SOAP configurable, RabbitMQ configurable, eventos y deduplicación de mensajes entrantes.
- Pendiente externo: URL/prueba SOAP con M1; contraseña e IP para Core; corrección del registro
  `streetClousureEnded` a `streetClosureEnded`; prueba bilateral con M5.
- Evolución no implementada: Transactional Outbox, `Idempotency-Key`, rate limiting,
  paginación/filtros e hipermedia.
