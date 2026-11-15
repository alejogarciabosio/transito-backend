# QA — Backend Tránsito (Módulo 7) · Informe de pruebas

> **Para quién es:** el dev de backend. Está escrito para que lo puedas pegar directo en Claude (u otra IA) y pedirle que arregle los bugs. Al final hay un prompt sugerido.
> **Fuente analizada:** `Backend_-_Módulo de Tránsito_-_22-9_-_Desarrollo_de_Aplicaciones_2.zip` (carpeta `transito-backend-main`)
> **Fecha:** 2026-10-05 · **QA:** Alejo García Bosio
> **Regla del trabajo:** no se tocó `src/main` ni la lógica de negocio. Los cambios están solo en `pom.xml` (dependencias de test y configuración de surefire) y en `src/test/**`.

---

## 0. Contexto antes de arreglar (leer primero)

- **Fase de pruebas:** los listeners sin broker son esperables y no se reportan como bug.
- **Tránsito se integra con otros módulos vía Core:** otros módulos se **suscriben** a nuestros eventos (Rentas, Obras, Ambiente...) y nosotros **consumimos** `ticketUpdated` y `streetClosureRequested`. Por eso:
  - Todo lo que sale en un evento es **contrato público**. BE-05 (`inspectorId`), BE-07 (`authorizedBy`), BE-06 (`vehicleReleased` sin requisitos) y BE-14 (`affectedStreet` vacío) se propagan a otros módulos: prioridad **alta**.
  - Antes de cambiar nombres o valores de un campo de evento, acordarlo con el módulo suscriptor. Las diferencias detectadas están en la sección 5: `"OBRAS"` vs `"Obras"`, `UNPAID_PARKING` vs `unpaid_parking`, `LocalDate` vs datetime.
  - **Pendientes a definir con Core** (no son bugs hoy, pero conviene diseñarlos ya):
    1. **Idempotencia:** el mismo `ticketUpdated` recibido dos veces crea dos operativos.
    2. **Publicar después del commit:** usar outbox o `@TransactionalEventListener`, para no emitir eventos de cosas que no se guardaron.
    3. **Mensajes inválidos:** un código de módulo desconocido hoy tira `IllegalArgumentException`; hay que definir reintentos o dead-letter.

---

## 1. Resumen

| Métrica (JaCoCo) | Antes | Después |
|---|---|---|
| **Líneas** | **0,0 %** (0/791) | **99,4 %** (786/791) |
| Ramas | 0 % | **100 %** (61/61) |
| Instrucciones | 0 % | 99,5 % |
| Métodos | 0 % | 99,5 % |
| Tests | 1 (fallaba) | **108 ejecuciones en verde** + **20 tests de bugs** (fallan a propósito) |
| `./mvnw verify` (gate 85 % del CI) | ❌ BUILD FAILURE | ✅ BUILD SUCCESS, "All coverage checks have been met" |

**¿Por qué estaba en 0 %?** El único test (`TransitoBackendApplicationTests.contextLoads`) levantaba el contexto contra la base **real** de Supabase con `${DB_URL}`. Sin `.env`, por ejemplo en el CI de GitHub sin secrets, falla con `Unable to determine Dialect without JDBC metadata`, y entonces el `./mvnw verify` del pipeline nunca pudo pasar.

**Las 5 líneas sin cubrir:**
- `TransitoBackendApplication.main()` (2 líneas). Es el arranque de la app y es normal que quede afuera.
- `ParkingSessionController.flagUnpaid` y `ParkingSessionService.flagUnpaid`, en la ruta exitosa (3 líneas). **No se pueden cubrir porque el bug BE-01 las rompe**: hoy esa ruta siempre termina en 500.

### Clases con menos cobertura (después)

| Clase | Líneas | Motivo |
|---|---|---|
| `TransitoBackendApplication` | 1/3 | `main()` |
| `web.ParkingSessionController` | 6/8 | BUG-BE-01 |
| `service.ParkingSessionService` | 24/25 | BUG-BE-01 |
| Todo lo demás | 100 % | — |

### Cómo correrlo

```bash
# Suite normal + reporte + gate 85% (lo mismo que corre el CI)
./mvnw verify
# Reporte HTML: target/site/jacoco/index.html

# Solo los tests que reproducen bugs (hoy FALLAN los 20 — es lo esperado)
./mvnw test -Dbugs.excluded=none -Dgroups=bug -Djacoco.skip=true
```

> En macOS, si `./mvnw` tira "operation not permitted" (por la cuarentena del zip), usar `sh mvnw ...` o `xattr -d com.apple.quarantine mvnw`.

---

## 2. Setup agregado (qué y por qué)

| Archivo | Cambio | Por qué |
|---|---|---|
| `pom.xml` | `com.h2database:h2` (scope test) | Base en memoria para los tests, así no se toca Supabase ni se consumen sus 15 conexiones. |
| `pom.xml` | `spring-boot-starter-security-test` (scope test) | Utilidades de test de Spring Security. |
| `pom.xml` | `maven-surefire-plugin` → `<excludedGroups>${bugs.excluded}</excludedGroups>` + property `bugs.excluded=bug` | Los tests `@Tag("bug")` reproducen bugs abiertos y fallan: quedan **fuera del build normal**, para no romper el CI, pero se pueden correr a pedido. |
| `src/test/resources/application.properties` | H2 en modo PostgreSQL, `ddl-auto=create-drop`, `jwt.secret` ficticio | Perfil de test autocontenido, sin `.env`. |
| `src/test/java/.../support/*` | `AbstractIntegrationTest`, `RecordingEventPublisher`, `TestEventsConfig`, `TestUsers`, `Payloads` | Base común: contexto completo + MockMvc + JWT reales + limpieza de la base + **captura de cada evento publicado** para validarlo. |

### Estructura de tests

```
src/test/java/ar/edu/uade/transito/transito_backend/
├── TransitoBackendApplicationTests.java      (existente, ahora pasa con H2)
├── support/                                  (infraestructura de test + modelos de prueba)
├── unit/                                     (JUnit/Mockito puros, sin Spring)
│   ├── InfractionServiceTest.java            matriz completa de transiciones de estado
│   ├── OtherServicesTest.java                cortes, retenciones, tarifas
│   ├── JwtServiceTest.java                   firma, expiración, clave débil
│   ├── AuthComponentsTest.java               StubIdentityValidator, CurrentUserProvider
│   └── EventInfrastructureTest.java          sobre de eventos, LoggingEventPublisher, RequestingModule
├── integration/                              (@SpringBootTest + MockMvc + H2 + JWT real)
│   ├── InfractionIntegrationTest.java        22 casos
│   ├── ParkingIntegrationTest.java           zonas, tarifas, sesiones
│   ├── RoadAccidentIntegrationTest.java
│   ├── TrafficOperationIntegrationTest.java
│   ├── StreetClosureIntegrationTest.java
│   ├── VehicleImpoundmentIntegrationTest.java
│   ├── AuthAndSecurityIntegrationTest.java   login de los 7 roles, JWT, CORS, health
│   └── TicketEventListenerIntegrationTest.java
└── bugs/
    └── BugReproductionTest.java              20 tests @Tag("bug") — uno por bug
```

---

## 3. Reglas de negocio detectadas

| ID | Regla | Dónde se implementa | ¿Se cumple? |
|---|---|---|---|
| RN-BE-01 | Toda infracción necesita evidencia o validación del inspector | `InfractionService.java:31` | ❌ No se valida (BUG-BE-04, BUG-BE-05) |
| RN-BE-02 | Una infracción apelada no es firme hasta resolver el descargo | `InfractionService.java:84-125` | ✅ |
| RN-BE-03 | Rentas solo recibe infracciones confirmadas (evento `infractionConfirmed`) | `InfractionService.java:100-125` | ✅ Solo se publica al confirmar |
| RN-BE-04 | Un vehículo retenido no se libera sin cumplir los requisitos | `VehicleImpoundmentService.java:70-95` | ❌ Solo valida el estado, no los requisitos (BUG-BE-06) |
| RN-BE-05 | Las actas cerradas no se modifican (infracción CONFIRMED/ANNULLED, accidente VERIFIED, operativo CLOSED, retención RELEASED) | `requireNotFinal` / `requireOpen` / `requireImpounded` | ✅ |
| RN-BE-06 | Las anulaciones requieren autorización | `InfractionController.java:97-101` | ⚠️ Solo por rol; `authorizedBy` viene del body (BUG-BE-07) |
| RN-BE-07 | Toda infracción nace `PENDING_APPEAL`; el estado del cliente se ignora | `InfractionService.java:32` | ✅ |
| RN-BE-08 | Máquina de estados de la infracción: `PENDING_APPEAL → APPEALED → CONFIRMED/ANNULLED`; `PENDING_APPEAL → CONFIRMED/ANNULLED` | `InfractionService.java` | ✅ (matriz completa testeada) |
| RN-BE-09 | Solo el ciudadano titular ve o apela su infracción | `InfractionController.java:64-85` | ✅ |
| RN-BE-10 | Una zona se da de baja lógicamente (`INACTIVE`) | `ParkingZoneService.java:52` | ✅ |
| RN-BE-11 | Una tarifa no se borra si hay sesiones abiertas en la zona | `ParkingTariffService.java:54` | ✅ |
| RN-BE-12 | Una sesión nace impaga y abierta; no se cierra dos veces | `ParkingSessionService.java:27-52` | ✅ |
| RN-BE-13 | La falta de pago genera una Infraction `UNPAID_PARKING` (reusa el circuito) | `ParkingSessionService.java:58-75` | ❌ Siempre falla (BUG-BE-01) |
| RN-BE-14 | Corte de calle: `REQUESTED → APPROVED/REJECTED`, `APPROVED → ACTIVE → FINISHED` | `StreetClosureService.java` | ✅ |
| RN-BE-15 | Un corte nace del evento `streetClosureRequested` (M3=Obras, M6=Ambiente) | `StreetClosureEventListener.java` | ✅ (pero BUG-BE-14) |
| RN-BE-16 | Un `ticketUpdated` ROUTED a M7 crea un operativo `COMPLAINT_VERIFICATION` | `TicketEventListener.java` | ✅ |
| RN-BE-17 | Un operativo cerrado sin `endDateTime` toma "ahora" | `TrafficOperationService.java:75` | ✅ |
| RN-BE-18 | Un accidente pasa a `VERIFIED` y queda cerrado | `RoadAccidentService.java:57-70` | ✅ |
| RN-BE-19 | Seguridad: sin token o token inválido → 403; rol insuficiente → 403; login inválido → 401 | `SecurityConfig`, `GlobalExceptionHandler` | ✅ (salvo BUG-BE-03 y BUG-BE-13) |

---

## 4. Plan de casos de prueba

Leyenda de tipo: **U** = unitario, **I** = integración (HTTP + seguridad + JPA + eventos). Estado: ✅ PASA · ❌ FALLA (bug).

### 4.1 Infracciones — `InfractionIntegrationTest`, `InfractionServiceTest`

| ID | Rol | Precondición | Pasos / datos | Resultado esperado | Tipo | Estado |
|---|---|---|---|---|---|---|
| TC-BE-INF-01 | INSPECTOR | — | `POST /infractions` con `Payloads.infraction` | 201, `PENDING_APPEAL`, evento `infractionRegistered` con sobre y payload completos | I | ✅ |
| TC-BE-INF-02 | INSPECTOR | — | POST con `"status":"CONFIRMED"` | Igual nace `PENDING_APPEAL` | I | ✅ |
| TC-BE-INF-03 | INSPECTOR | — | Registrar | **No** se publica `infractionConfirmed` (RN-BE-03) | I | ✅ |
| TC-BE-INF-04 | Todos menos INSPECTOR / sin token | — | POST | 403, sin evento, sin fila | I | ✅ |
| TC-BE-INF-05 | INSP/SUP/AUD vs CIUDADANO | 2 infracciones | `GET /infractions` | 200 con 2 / 403 | I | ✅ |
| TC-BE-INF-06 | CIUDADANO | 2 propias + 1 ajena | `GET /infractions/mine` | Solo las propias; INSPECTOR → 403 | I | ✅ |
| TC-BE-INF-07 | CIUDADANO / otro / INSP / AUD | 1 infracción | `GET /infractions/{id}` | 200 / 403 con ErrorResponse / 200 / 200 | I | ✅ |
| TC-BE-INF-08 | INSPECTOR | — | GET con UUID inexistente | 404 con `timestamp,status,error,message` | I | ✅ |
| TC-BE-INF-09 | INSPECTOR | — | GET `/infractions/no-es-un-uuid` | 400 | I | ✅ |
| TC-BE-INF-10 | INSPECTOR | PENDING_APPEAL | `PATCH` completo | 200 con los cambios | I | ✅ |
| TC-BE-INF-11 | INSPECTOR | CONFIRMED / ANNULLED | PATCH | 409 (RN-BE-05) | I | ✅ |
| TC-BE-INF-12 | SUPERVISOR | — | PATCH | 403 | I | ✅ |
| TC-BE-INF-13 | CIUDADANO titular | PENDING_APPEAL | `POST /{id}/appeal` | `APPEALED` + evento `infractionAppealed` con motivo | I | ✅ |
| TC-BE-INF-14 | CIUDADANO | APPEALED / CONFIRMED | apelar | 409 | I | ✅ |
| TC-BE-INF-15 | Otro ciudadano / INSPECTOR | — | apelar | 403, sin evento | I | ✅ |
| TC-BE-INF-16 | SUPERVISOR | PENDING_APPEAL | `POST /{id}/confirm` | `CONFIRMED` + `infractionConfirmed` con deudor, monto, inspector, ubicación | I | ✅ |
| TC-BE-INF-17 | INSPECTOR | APPEALED | confirmar (apelación rechazada) | `CONFIRMED`, 1 evento | I | ✅ |
| TC-BE-INF-18 | SUPERVISOR | CONFIRMED / ANNULLED | confirmar | 409, el evento no se republica | I | ✅ |
| TC-BE-INF-19 | AUDITOR / CIUDADANO | — | confirmar | 403 | I | ✅ |
| TC-BE-INF-20 | SUPERVISOR | PENDING_APPEAL | `POST /{id}/annul` | `ANNULLED` + `infractionAnulled` con motivo, autorizante y fecha | I | ✅ |
| TC-BE-INF-21 | SUPERVISOR | APPEALED | anular (descargo aceptado) | `ANNULLED` | I | ✅ |
| TC-BE-INF-22 | INSPECTOR / SUPERVISOR | PENDING / CONFIRMED | anular | 403 / 409, sin evento | I | ✅ |
| TC-BE-INF-U01 | — | repo vacío | `findById` | `ResourceNotFoundException` | U | ✅ |
| TC-BE-INF-U02..04 | — | cada uno de los 4 estados | appeal / confirm / annul / update | Matriz completa de transiciones válidas e inválidas | U | ✅ |

### 4.2 Estacionamiento — `ParkingIntegrationTest`, `OtherServicesTest`

| ID | Rol | Pasos | Esperado | Tipo | Estado |
|---|---|---|---|---|---|
| TC-BE-PZ-01 | ADMIN | `POST /parking-zones` | 201, `ACTIVE` | I | ✅ |
| TC-BE-PZ-02 | INSPECTOR / CIUDADANO | POST / PUT / DELETE zona | 403 | I | ✅ |
| TC-BE-PZ-03 | sin token | `GET /parking-zones`, `/{id}`, id inexistente | 200 / 200 / 404 | I | ✅ |
| TC-BE-PZ-04 | ADMIN | `PUT` completo | 200 con cambios | I | ✅ |
| TC-BE-PZ-05 | ADMIN | DELETE | 204 y queda `INACTIVE`; inexistente → 404 | I | ✅ |
| TC-BE-PT-01 | ADMIN / público | crear 2 tarifas en zonas distintas, GET por zona e id | Filtra por zona; 404 si no existe | I | ✅ |
| TC-BE-PT-02 | público | `GET /parking-tariffs` sin `zoneId` | 400 | I | ✅ |
| TC-BE-PT-03 | ADMIN / OPERADOR | PUT | 200 / 403 | I | ✅ |
| TC-BE-PT-04 | ADMIN | DELETE con la sesión de la zona cerrada | 204, después 404 | I | ✅ |
| TC-BE-PT-05 | ADMIN | DELETE con una sesión abierta | 409 "sesiones activas" | I | ✅ |
| TC-BE-PT-U01 | — | sesiones cerradas vs abiertas | solo bloquea si hay una abierta | U | ✅ |
| TC-BE-PS-01 | CIUDADANO / OPERADOR / INSPECTOR | `POST /parking-sessions` con `paid:true` | 201, `paid=false`, `endDateTime=null` / INSPECTOR 403 | I | ✅ |
| TC-BE-PS-02 | varios | listado y detalle por rol | 200 / 403 / 404 | I | ✅ |
| TC-BE-PS-03 | OPERADOR / CIUDADANO | `PATCH /{id}/close` dos veces | 200 con `endDateTime` / 409 | I | ✅ |
| TC-BE-PS-04 | INSPECTOR | `POST /{id}/flag-unpaid` | Infraction `UNPAID_PARKING` + `infractionRegistered` | I | ❌ BUG-BE-01 |
| TC-BE-PS-05 | OPERADOR / INSPECTOR | flag-unpaid; sesión ya paga | 403 / 409, sin evento | I | ✅ |

### 4.3 Siniestros — `RoadAccidentIntegrationTest`

| ID | Pasos | Esperado | Estado |
|---|---|---|---|
| TC-BE-ACC-01 | OPERADOR registra | `PRELIMINARY` + `roadAccidentRegistered` con vehículos, personas, daño, ubicación | ✅ |
| TC-BE-ACC-02 | permisos por rol | SUPERVISOR no registra, CIUDADANO no consulta, OPERADOR no edita; 404 | ✅ |
| TC-BE-ACC-03 | PATCH sin status | sigue `PRELIMINARY` con los cambios | ✅ |
| TC-BE-ACC-04 | PATCH `VERIFIED` y después otro PATCH | `VERIFIED` → 409 | ✅ |
| TC-BE-ACC-05 | PATCH con `PRELIMINARY` | sin efecto especial | ✅ |
| TC-BE-ACC-06 | enum inválido (`injuryLevel: MUERTO`) | 400 | ✅ |

### 4.4 Operativos — `TrafficOperationIntegrationTest` + `TicketEventListenerIntegrationTest`

| ID | Pasos | Esperado | Estado |
|---|---|---|---|
| TC-BE-OP-01 | crear con `status:CLOSED` | `OPEN`, `resolution=null`, evento `trafficOperationCreated` | ✅ |
| TC-BE-OP-02 | permisos | SUPERVISOR no crea, OPERADOR no lista ni edita, SUPERVISOR no cierra; 404 | ✅ |
| TC-BE-OP-03 | PATCH completo | 200 con zona, inspectores y fin | ✅ |
| TC-BE-OP-04 | cerrar sin `endDateTime` | `CLOSED` + resolución + `endDateTime` = ahora | ✅ |
| TC-BE-OP-05 | cerrar con `endDateTime` | lo respeta | ✅ |
| TC-BE-OP-06 | editar / cerrar uno cerrado | 409 | ✅ |
| TC-BE-EVT-01 | `ticketUpdated` M7 + ROUTED | crea operativo `COMPLAINT_VERIFICATION` `OPEN` con ticketId + evento | ✅ |
| TC-BE-EVT-02 | ticket de otra área / área null | se ignora | ✅ |
| TC-BE-EVT-03 | updateType PROGRESS / RETURNED | no crea nada | ✅ |

### 4.5 Cortes de calle — `StreetClosureIntegrationTest`, `OtherServicesTest`

| ID | Pasos | Esperado | Estado |
|---|---|---|---|
| TC-BE-SC-01 | evento `streetClosureRequested` M3 y M6 | `REQUESTED`, módulo OBRAS/AMBIENTE, sin eventos salientes | ✅ |
| TC-BE-SC-02 | código de módulo `M9` | `IllegalArgumentException`, no se crea nada | ✅ |
| TC-BE-SC-03 | aprobar con desvíos | `APPROVED` + `streetClosureApproved` | ✅ |
| TC-BE-SC-04 | aprobar con body `{}` | 200 con listas vacías | ❌ BUG-BE-02 |
| TC-BE-SC-05 | rechazar | `REJECTED` + `streetClosureRejected`; después no se puede aprobar ni activar (409) | ✅ |
| TC-BE-SC-06 | ciclo completo | eventos en orden: approved → activated (`sourceModule=M3`, `activatedBy` del token) → ended | ✅ |
| TC-BE-SC-07 | transiciones inválidas | 409 | ✅ |
| TC-BE-SC-08 | permisos | 403 / 404 | ✅ |
| TC-BE-SC-U01 | approve/reject desde cada estado ≠ REQUESTED | 409, sin evento | ✅ |

### 4.6 Retenciones — `VehicleImpoundmentIntegrationTest`

| ID | Pasos | Esperado | Estado |
|---|---|---|---|
| TC-BE-IMP-01 | DEPOSITO registra con `status:RELEASED` + `paymentReference` | `IMPOUNDED`, ref null, evento `vehicleImpounded` | ✅ |
| TC-BE-IMP-02 | consultas | ciudadano titular 200, ajeno 403, listado ciudadano 403, alta ciudadano 403 | ✅ |
| TC-BE-IMP-03 | DEPOSITO edita / INSPECTOR edita | 200 / 403 | ✅ |
| TC-BE-IMP-04 | liberar con requisitos | `RELEASED`, `releasedBy` = usuario del token, evento `vehicleReleased` | ✅ |
| TC-BE-IMP-05 | liberar dos veces / editar liberado / INSPECTOR libera | 409 / 409 / 403; 1 solo evento | ✅ |
| TC-BE-IMP-U01 | liberar ya RELEASED (unit) | 409 sin evento | ✅ |

### 4.7 Auth y seguridad — `AuthAndSecurityIntegrationTest`, `JwtServiceTest`, `AuthComponentsTest`

| ID | Pasos | Esperado | Estado |
|---|---|---|---|
| TC-BE-AUTH-01 (×7) | login de los 7 usuarios stub | 200, rol correcto, `expiresInSeconds=7200`; el token funciona | ✅ |
| TC-BE-AUTH-02 (×3) | password mal / usuario inexistente / mayúsculas | 401 con ErrorResponse | ✅ |
| TC-BE-AUTH-03 | login `{}` | 401 | ❌ BUG-BE-03 |
| TC-BE-AUTH-04/05 | StubIdentityValidator, CurrentUserProvider | — | ✅ |
| TC-BE-JWT-01..05 | ida y vuelta de claims, expiración, token vencido, otra clave, clave < 256 bits | — | ✅ |
| TC-BE-SEC-01 | `/actuator/health` sin token | 200 UP | ✅ |
| TC-BE-SEC-02 | endpoint protegido sin token | 403 (como dice la guía) | ✅ |
| TC-BE-SEC-03 | token basura / adulterado / sin "Bearer" | 403; el válido da 200 | ✅ |
| TC-BE-SEC-04 | token con `role: HACKER` | 403 | ✅ |
| TC-BE-SEC-05 | CORS preflight desde `localhost:5173` / `evil.com` | header Allow-Origin / 403 | ✅ |
| TC-BE-EVT-04..06 | EventEnvelope, LoggingEventPublisher, RequestingModule M3/M6 | — | ✅ |

---

## 5. Validación de eventos

Cada evento se capturó con `RecordingEventPublisher`, que reemplaza a `LoggingEventPublisher` en los tests. Se validó el **sobre** (`eventId` UUID único, `eventType`, `occurredAt`, `sourceModule = "transito"`) y el **payload** campo por campo.

| Evento | Lo dispara | Test que lo valida | Validado | Observación vs. contrato (`docs/historial-payloads`) |
|---|---|---|---|---|
| `infractionRegistered` | `POST /infractions` | TC-BE-INF-01 | ✅ | — |
| `infractionAppealed` | `POST /infractions/{id}/appeal` | TC-BE-INF-13 | ✅ | — |
| `infractionConfirmed` (→ Rentas) | `POST /infractions/{id}/confirm` | TC-BE-INF-16/17/18, U03 | ✅ Solo al confirmar, una vez | — |
| `infractionAnulled` | `POST /infractions/{id}/annul` | TC-BE-INF-20/22 | ✅ | `authorizedBy` viene del body (BUG-BE-07) |
| `infractionRegistered` (UNPAID_PARKING) | `POST /parking-sessions/{id}/flag-unpaid` | TC-BE-PS-04 | ❌ | No se publica nunca (BUG-BE-01). El contrato usa `"unpaid_parking"` en minúscula y el enum serializa `UNPAID_PARKING` |
| `roadAccidentRegistered` | `POST /accidents` | TC-BE-ACC-01 | ✅ | `inspectorId` viene del body (BUG-BE-05b) |
| `trafficOperationCreated` | `POST /operations` y `ticketUpdated` ROUTED | TC-BE-OP-01, TC-BE-EVT-01 | ✅ | — |
| `streetClosureApproved` | `POST /street-closures/{id}/approve` | TC-BE-SC-03/06 | ⚠️ | `affectedStreet` siempre `[]` (BUG-BE-14). El contrato pide `"Obras"`/`"Ambiente"` y se envía `"OBRAS"`/`"AMBIENTE"`. El contrato pide datetime en start/endDate y se envía `LocalDate` |
| `streetClosureRejected` | `POST /street-closures/{id}/reject` | TC-BE-SC-05 | ⚠️ | Mismo desfasaje Obras/OBRAS |
| `streetClosureActivated` | `POST /street-closures/{id}/activate` | TC-BE-SC-06 | ✅ | Usa `sourceModule: "M3"` (código M), inconsistente con approved/rejected que usan el enum |
| `streetClosureEnded` | `POST /street-closures/{id}/finish` | TC-BE-SC-06 | ✅ | — |
| `vehicleImpounded` | `POST /impoundments` | TC-BE-IMP-01 | ✅ | — |
| `vehicleReleased` | `POST /impoundments/{id}/release` | TC-BE-IMP-04/05 | ✅ | Se publica aunque no haya requisitos (BUG-BE-06) |
| **Entrante** `ticketUpdated` | Core / M2 | TC-BE-EVT-01..03 | ✅ | Hoy nada lo invoca (no hay broker) |
| **Entrante** `streetClosureRequested` | Core / M3, M6 | TC-BE-SC-01/02 | ✅ | El payload no trae `affectedStreet` |

**Negativos validados:** ningún 403, 404 ni 409 publica eventos, y registrar o apelar no publica `infractionConfirmed`.

---

## 6. Bugs encontrados

> No se arregló ninguno. Cada uno tiene un test en `bugs/BugReproductionTest.java` que **hoy falla** y va a pasar cuando se corrija.
> Correrlos: `./mvnw test -Dbugs.excluded=none -Dgroups=bug -Djacoco.skip=true`

### Resumen

| ID | Severidad | Archivo:línea | Título |
|---|---|---|---|
| BUG-BE-01 | 🔴 Crítica | `service/ParkingSessionService.java:64-74` | `flag-unpaid` siempre responde 500: el circuito de estacionamiento impago no funciona |
| BUG-BE-SEC | 🔴 Crítica | `.env` (en el zip) y `CLAUDE.md:29-30` | Credenciales reales de la base y secreto JWT distribuidos en el zip |
| BUG-BE-02 | 🟠 Alta | `web/StreetClosureController.java:49-50` | Aprobar un corte sin desvíos/condiciones → 500 |
| BUG-BE-04 | 🟠 Alta | `service/InfractionService.java:31` | Se registran infracciones sin evidencia (viola RN-BE-01) |
| BUG-BE-05 | 🟠 Alta | `web/mapper/InfractionMapper.java:21`, `web/mapper/RoadAccidentMapper.java:21` | `inspectorId` sale del body: un inspector puede firmar a nombre de otro |
| BUG-BE-06 | 🟠 Alta | `service/VehicleImpoundmentService.java:70-83` | Se libera un vehículo sin requisitos ni pago (viola RN-BE-04) |
| BUG-BE-07 | 🟠 Alta | `web/InfractionController.java:100` | `authorizedBy` de la anulación sale del body y puede ser null o falso (RN-BE-06) |
| BUG-BE-03 | 🟡 Media | `auth/StubIdentityValidator.java:28` | Login sin `username` → 500 (NPE) |
| BUG-BE-08 | 🟡 Media | `web/GlobalExceptionHandler.java` (falta handler) | Si faltan campos obligatorios responde 500 en vez de 400 |
| BUG-BE-09 | 🟡 Media | `InfractionService.java:70`, `TrafficOperationService.java:61`, `VehicleImpoundmentService.java:63`, `RoadAccidentService.java:62` | Los PATCH pisan con `null` lo que no se envía → 500 o pérdida de datos |
| BUG-BE-10 | 🟡 Media | `service/ParkingSessionService.java:27-31` | Se puede estacionar en una zona inexistente o dada de baja |
| BUG-BE-11 | 🟡 Media | `service/ParkingTariffService.java:24-25` | Tarifas con precio negativo o vigencia invertida |
| BUG-BE-12 | 🟡 Media | `web/GlobalExceptionHandler.java` | Un JSON mal formado no devuelve el formato `ErrorResponse` prometido en la guía |
| BUG-BE-14 | 🟡 Media | `listener/StreetClosureEventListener.java:24`, `listener/dto/StreetClosureRequestedPayload.java` | `streetClosureApproved` siempre envía `affectedStreet: []` (campo requerido) |
| BUG-BE-15 | 🟡 Media | `service/ParkingSessionService.java:60` | La misma sesión se puede multar varias veces (bloqueado hoy por BE-01) |
| BUG-BE-13 | 🟢 Baja | `auth/JwtAuthenticationFilter.java:48` + `auth/JwtService.java:63` | JWT firmado sin claim `role` → 500 en vez de 403 |
| BUG-BE-16 | 🟢 Baja | `web/InfractionController.java:60` | SUPERVISOR confirma y lista, pero no puede ver el detalle (403) |
| OBS-BE-01 | 🟢 Baja | `.github/workflows/ci.yml:5` | El CI corre solo en PR a `main`; `CLAUDE.md` dice que también corre en `develop` |
| OBS-BE-02 | 🟢 Baja | `web/ParkingSessionController.java:55,61` | Cualquier ciudadano ve **y cierra** la sesión de otro (conocido para GET, no para close) |
| OBS-BE-03 | 🟢 Baja | `model/ParkingSession.java` | No existe endpoint de pago: `paid` nunca pasa a `true` |
| OBS-BE-04 | 🟢 Baja | `src/main/resources/application.properties:6-7` | `ddl-auto=update` y `show-sql=true` también en producción |

### Detalle

#### BUG-BE-01 · 🔴 Crítica · `flag-unpaid` siempre responde 500
- **Archivo:** `src/main/java/.../service/ParkingSessionService.java:64-74`
- **Descripción:** `flagUnpaid` arma la `Infraction` sin `location`. `Infraction.location` es un `@Embedded` cuyo `street` es `nullable=false`, así que Hibernate la rechaza con `DataIntegrityViolationException: not-null property references a null or transient value ... Infraction.location`. Por eso **nunca** se genera la multa por estacionamiento impago (RN-BE-13).
- **Reproducir (manual):**
  1. `POST /auth/login` con admin1 (contraseña `STUB_PASSWORD_ADMIN1` del `.env`) → `POST /parking-zones {"name":"Z","allowedHours":"8-20"}`.
  2. ciudadano1 → `POST /parking-sessions {"licensePlate":"AB123CD","zoneId":"<zona>","startDateTime":"2026-09-20T09:00:00"}`.
  3. inspector1 → `POST /parking-sessions/<id>/flag-unpaid {"baseAmount":12000}`.
- **Actual:** 500. **Esperado:** 200 con una infracción `UNPAID_PARKING` en `PENDING_APPEAL` y el evento `infractionRegistered`.
- **Test:** `BugReproductionTest.bug01_flagUnpaidAlways500`
- **Sugerencia:** tomar la ubicación de la zona o hacer `location` opcional para `UNPAID_PARKING`.

#### BUG-BE-SEC · 🔴 Crítica · Secretos en el zip
- **Archivos:** `transito-backend-main/.env` (incluido en el zip entregado: `DB_URL`, `DB_USER`, **`DB_PASSWORD`**, **`JWT_SECRET`**), y `CLAUDE.md:29-30` (host y usuario de Supabase).
- **Riesgo:** quien tenga el zip tiene acceso de escritura a la base de producción y puede **firmar JWT válidos con cualquier rol**.
- **Esperado:** el `.env` nunca se distribuye.
- **Acción sugerida:** rotar la contraseña de Supabase y el `JWT_SECRET` ya mismo, y regenerar el zip sin `.env`.

#### BUG-BE-02 · 🟠 Alta · Aprobar un corte sin desvíos → 500
- **Archivo:** `web/StreetClosureController.java:49-50`
- **Descripción:** cuando `detours` o `conditions` vienen null, se asigna `List.of()`, que es **inmutable**. Al persistir la `@ElementCollection`, Hibernate intenta modificarla y tira `UnsupportedOperationException`.
- **Reproducir:** con un corte en `REQUESTED`, supervisor1 → `POST /street-closures/{id}/approve` con body `{}`.
- **Actual:** 500. **Esperado:** 200, `APPROVED` con listas vacías.
- **Test:** `bug02_approveWithoutDetours`
- **Sugerencia:** usar `new ArrayList<>()`.

#### BUG-BE-03 · 🟡 Media · Login sin username → 500
- **Archivo:** `auth/StubIdentityValidator.java:28` (`USERS.get(username)` con un `Map.of` no admite null).
- **Reproducir:** `POST /auth/login` con `{}` o `{"password":"x"}`.
- **Actual:** 500 (NPE). **Esperado:** 401 "Usuario o contraseña inválidos".
- **Test:** `bug03_loginWithoutUsername`

#### BUG-BE-04 · 🟠 Alta · Infracción sin evidencia
- **Archivo:** `service/InfractionService.java:31` (no hay ninguna validación antes de `save`).
- **Reproducir:** inspector1 → `POST /infractions` con `"evidenceUrl": []`.
- **Actual:** 201. **Esperado:** 400 (RN-BE-01: "toda infracción necesita evidencia o validación del inspector").
- **Test:** `bug04_infractionWithoutEvidence`

#### BUG-BE-05 · 🟠 Alta · `inspectorId` falsificable
- **Archivos:** `web/mapper/InfractionMapper.java:21`, `web/mapper/RoadAccidentMapper.java:21`
- **Descripción:** el `inspectorId` se toma del body, no del JWT. En `flag-unpaid`, `activate` y `release` sí se usa `currentUserProvider`, así que es inconsistente.
- **Reproducir:** inspector1 (u-1001) → `POST /infractions` con `"inspectorId":"u-OTRO"`.
- **Actual:** se guarda `u-OTRO` y viaja en `infractionConfirmed` hacia Rentas. **Esperado:** `u-1001`.
- **Tests:** `bug05_inspectorIdSpoofing`, `bug05b_accidentInspectorIdSpoofing`

#### BUG-BE-06 · 🟠 Alta · Liberación sin requisitos
- **Archivo:** `service/VehicleImpoundmentService.java:70-83`
- **Reproducir:** deposito1 → `POST /impoundments/{id}/release` con `{}`.
- **Actual:** 200, `RELEASED` y evento `vehicleReleased` con `verifiedRequirements=null` y `paymentReference=null`. **Esperado:** 400/409 (RN-BE-04).
- **Test:** `bug06_releaseWithoutRequirements`

#### BUG-BE-07 · 🟠 Alta · `authorizedBy` de la anulación viene del body
- **Archivo:** `web/InfractionController.java:100`
- **Reproducir:** supervisor1 → `POST /infractions/{id}/annul {"annulmentReason":"x"}` (sin `authorizedBy`).
- **Actual:** anula y publica `infractionAnulled` con `authorizedBy=null`; también se puede mandar el id de otra persona. **Esperado:** `authorizedBy` = id del supervisor autenticado (u-1002).
- **Test:** `bug07_annulAuthorizedByFromBody`

#### BUG-BE-08 · 🟡 Media · Campos obligatorios → 500
- **Archivo:** `web/GlobalExceptionHandler.java`. No hay `@Valid` en los DTOs ni handler para `DataIntegrityViolationException`.
- **Reproducir:** admin1 → `POST /parking-zones {}`; inspector1 → `POST /infractions {"licensePlate":"AA"}`.
- **Actual:** 500. **Esperado:** 400 con `ErrorResponse` indicando el campo.
- **Test:** `bug08_missingRequiredFieldsGive500`

#### BUG-BE-09 · 🟡 Media · PATCH pisa con null
- **Archivos:** `InfractionService.java:70-79`, `TrafficOperationService.java:61-64`, `VehicleImpoundmentService.java:63-65`, `RoadAccidentService.java:62-67` (y `ParkingZoneService.java:41-44` en PUT).
- **Descripción:** se copian todos los campos sin chequear null, así que un PATCH parcial borra datos o rompe la restricción NOT NULL.
- **Reproducir y resultado actual:**
  - `PATCH /infractions/{id} {"finalAmount":1}` → 500.
  - `PATCH /operations/{id} {"zone":"Once"}` → borra `assignedInspectors`.
  - `PATCH /impoundments/{id} {"reason":"x"}` → 500.
  - `PATCH /accidents/{id} {"damageDescription":"x"}` → 400 (el `boolean` primitivo es obligatorio en Jackson 3) y, si se envía, el resto queda en null → 500.
- **Esperado:** se actualiza solo lo enviado.
- **Tests:** `bug09a..d`

#### BUG-BE-10 · 🟡 Media · Sesión en zona inválida
- **Archivo:** `service/ParkingSessionService.java:27-31`
- **Reproducir:** ciudadano1 → `POST /parking-sessions` con un `zoneId` aleatorio, o con el de una zona dada de baja.
- **Actual:** 201. **Esperado:** 404 / 409.
- **Test:** `bug10_sessionInInvalidZone`

#### BUG-BE-11 · 🟡 Media · Tarifa inválida
- **Archivo:** `service/ParkingTariffService.java:24-25`
- **Reproducir:** `POST /parking-tariffs {"pricePerHour":-10,"validFrom":"2026-12-31","validTo":"2026-01-01",...}`
- **Actual:** 201. **Esperado:** 400.
- **Test:** `bug11_invalidTariff`

#### BUG-BE-12 · 🟡 Media · El 400 no respeta el formato `ErrorResponse`
- **Archivo:** `web/GlobalExceptionHandler.java` (no maneja `HttpMessageNotReadableException` ni `MethodArgumentTypeMismatchException`).
- **Reproducir:** `POST /infractions` con body `{esto no es json`.
- **Actual:** 400 con el body por defecto de Spring, sin `message`. **Esperado:** `{timestamp,status,error,message}`, como promete la guía §3. El frontend usa `data.message`.
- **Test:** `bug12_malformedJsonFormat`

#### BUG-BE-13 · 🟢 Baja · JWT sin `role` → 500
- **Archivos:** `auth/JwtService.java:63` (`Role.valueOf(null)` → NPE) y `auth/JwtAuthenticationFilter.java:48` (solo captura `JwtException | IllegalArgumentException`).
- **Reproducir:** firmar un token con el secreto y sin el claim `role`. Solo es explotable si alguien conoce el secreto (ver BUG-BE-SEC).
- **Esperado:** 403.
- **Test:** `bug13_tokenWithoutRole`

#### BUG-BE-14 · 🟡 Media · `affectedStreet` vacío en `streetClosureApproved`
- **Archivos:** `listener/dto/StreetClosureRequestedPayload.java` (no tiene el campo) y `listener/StreetClosureEventListener.java:24`.
- **Descripción:** el contrato marca `affectedStreet` como requerido y siempre se publica `[]`.
- **Test:** `bug14_affectedStreetAlwaysEmpty`

#### BUG-BE-15 · 🟡 Media · Multa duplicada por la misma sesión
- **Archivo:** `service/ParkingSessionService.java:60` (solo chequea `isPaid()`, no si ya existe una infracción con ese `originParkingSessionId`).
- **Reproducir:** `flag-unpaid` dos veces sobre la misma sesión, una vez corregido BUG-BE-01.
- **Esperado:** 409 la segunda vez.
- **Test:** `bug15_flagUnpaidTwice`

#### BUG-BE-16 · 🟢 Baja · SUPERVISOR no ve el detalle
- **Archivo:** `web/InfractionController.java:60` (`hasAnyRole('INSPECTOR','AUDITOR','CIUDADANO')`).
- **Reproducir:** supervisor1 → `GET /infractions/{id}`.
- **Actual:** 403, pero ese mismo rol puede listar, confirmar y anular. **Esperado:** 200.
- **Test:** `bug16_supervisorCannotSeeDetail`

---

## 7. Riesgos y limitaciones

- Los tests usan **H2 en modo PostgreSQL**, no Postgres real. La lógica y las restricciones NOT NULL se comportan igual (los errores de BE-01, 08 y 09 los tira Hibernate, no la base), pero no se probó el pooler de Supabase.
- Los listeners de eventos entrantes no tienen broker: se testean invocando el método directamente.
- `StubIdentityValidator` tiene las contraseñas hardcodeadas, y es así por diseño hasta tener el contrato con Usuarios.
- No hay endpoint de pago de sesiones (OBS-BE-03), así que el flujo "pagó → no se multa" solo se pudo probar seteando `paid=true` en la base.

---

## 8. Prompt sugerido para Claude

```
Sos dev backend del proyecto transito-backend (Spring Boot 4.1, Java 17).
Te paso QA-TESTS-BACKEND.md con 16 bugs confirmados. Cada uno tiene un test que hoy falla en
src/test/java/ar/edu/uade/transito/transito_backend/bugs/BugReproductionTest.java.

Objetivo: arreglá los bugs en src/main sin romper la suite normal.
1. Empezá por BUG-BE-01, 02, 03, 05, 06 y 07 (críticos y altos), después el resto.
2. No modifiques los tests de bugs salvo para quitarles @Tag("bug") cuando pasen
   y moverlos a la suite de integración correspondiente.
3. Después de cada fix corré:
   ./mvnw test -Dbugs.excluded=none -Dgroups=bug -Djacoco.skip=true   (deben ir pasando)
   ./mvnw verify                                                     (debe seguir en verde y >= 85%)
4. Para BUG-BE-08/12 agregá spring-boot-starter-validation, @Valid en los controllers y handlers en
   GlobalExceptionHandler que devuelvan ErrorResponse con status 400.
5. Para BUG-BE-09 implementá PATCH parcial (solo copiar campos != null).
6. Para BUG-BE-SEC no hay código: recordame rotar DB_PASSWORD y JWT_SECRET en Supabase/Railway/GitHub.
Explicame cada cambio en una línea al final.
```
