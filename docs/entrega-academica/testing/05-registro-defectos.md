# 05 - Registro de defectos

Actualizado: 15/11/2026 ~21:30 ART (cierre de DEF-33 con la confirmación de Core y M3 de las 20:51/20:54; DEF-42 cerrado con el PR #57 `fix/s22-activacion-en-ventana`, que se mergea y despliega al pasar el CI; con ese PR `./mvnw -q -B verify` da **532 tests, 0 fallos, 94,45 % de líneas**; frontend `main` `0279950`: 65 tests, 94,04 %). Estado anterior: (integración `integration/entrega4-f`: PR #55, #54 y #53 sobre `main` `b3b990f`; se despliega inmediatamente después del CI; `./mvnw -q -B verify` **518 tests, 0 fallos, 94,34 % de líneas**). Frontend en producción: `main` `6040e60` (PR #12). Estado anterior: Versión de referencia: `main` del backend en `57b2765` (merge del PR #48, desplegado en Railway y verificado: health UP, Core conectado, 6 eventos registrados, tabla `outbox_events` creada y relay activo) y `main` del frontend en `1f0187d` (PR #10).
Fuentes: código de `main` (`git grep`; cada "cerrado" se revisó en el código), `./mvnw verify` sobre `57b2765` (497 tests, 0 fallos, 93,74 % de líneas), CI de `main` (Newman, ZAP baseline, Trivy, Sonar, gitleaks en verde), reportes de los agentes (s1-s15, i3-i5, d2, t1, t2), `TABLERO.md` y `pruebas-modulos-15-11.md`. No se hicieron pruebas nuevas contra producción para este registro.

## Severidades usadas
No tuve a mano el PDF de la cátedra con las definiciones del profesor; usé estas definiciones de trabajo y **hay que contrastarlas con las del profesor antes de entregar**:
| Severidad | Definición de trabajo |
|---|---|
| Bloqueante | impide usar o desplegar el sistema, o corrompe datos; sin workaround |
| Crítica | riesgo grave de seguridad o de pérdida de integridad en producción, o falla de un flujo principal sin workaround |
| Mayor | incumple un requisito o criterio importante o degrada el servicio; existe workaround |
| Menor | desvío cosmético, de bajo riesgo o de documentación |

Estados: **Abierto**, **Cerrado** (corregido en `main`, desplegado y verificado con test o prueba), **Aceptado** (riesgo aceptado por decisión documentada).

## Tabla resumen
| Id | Título | Severidad | Estado |
|---|---|---|---|
| DEF-01 | Credenciales de desarrollo fijas en el código, activas en producción (login local) | Crítica | Cerrado 15/11 (PR #36, en #39) |
| DEF-02 | `/actuator/health` colgado con el broker caído | Mayor | Cerrado (PR #24, en #25) |
| DEF-03 | Adaptador REST de M1 no arrancaba (Jackson) | Mayor | Cerrado (PR #23, en #25) |
| DEF-04 | Sin registro de eventos publicados (regla 9.3 del TP) | Mayor | **Cerrado 15/11** (PR #46, en #48) |
| DEF-05 | Logs con payload completo de eventos (ampliado: cuerpo de mensajes AMQP inválidos en el log) | Mayor | Cerrado 15/11 (PR #34, en #39); ampliación cerrada en PR #50 (en `main` por #52) |
| DEF-06 | DNI (`nationalId`) dentro del JWT | Mayor | **Cerrado 15/11** (PR #42 + frontend #10, en #48) |
| DEF-07 | Un corte se activa antes de `startDate` | Menor | **Duplicado de DEF-42** (mismo defecto, confirmado por Core en producción; se sigue y se cuenta como DEF-42) |
| DEF-08 | `causationId` nulo en `streetClosureEnded` (hallado también por Core) | Mayor (reclasificado desde Menor al confirmarlo Core) | Cerrado 15/11 (PR #33, en #31) |
| DEF-09 | Swagger y `/v3/api-docs` públicos en producción | Menor | Aceptado |
| DEF-10 | 200 con `auth=false` de M1 devuelve 502 | Menor | Abierto (comportamiento testeado) |
| DEF-11 | Estacionamiento impago devolvía 500 | Mayor | Cerrado (verificado el 15/11) |
| DEF-12 | Contraseña de cuentas de prueba de otro módulo (M1) en el historial de Git | Menor | Aceptado |
| DEF-13 | State no implementado como patrón en el corte de calle | Mayor (criterio académico) | Cerrado 15/11 (PR #35, en #39) |
| DEF-14 | Outbox transaccional no implementado (pérdida de eventos con el broker caído) | Mayor | **Cerrado 15/11** (PR #46, en #48) |
| DEF-15 | BOLA: un ciudadano lee y cierra la sesión de estacionamiento de otro (KI-1) | Mayor | Cerrado 15/11 (PR #32, en #31) |
| DEF-16 | Infracción sin evidencia ni validación de inspector se acepta | Mayor | **Cerrado 15/11** (PR #47, en #48) |
| DEF-17 | `PATCH /infractions` parcial devuelve 400 en la reproducción BUG-BE-09a | Menor (probable test desactualizado) | Abierto (a confirmar) |
| DEF-18 | Pruebas de reproducción BUG-BE-02 y 14 desactualizadas | Menor (deuda de tests) | Abierto |
| DEF-19 | Importe base, reincidencia y costo de acarreo/estadía no se calculan en el servidor | Mayor | Abierto |
| DEF-20 | HU sin implementar (COR-05, OPE-03, EST-03/04, parte de INF-04, IND) | Mayor | Abierto (con workaround operativo documentado, PR #51) |
| DEF-21 | `streetClosureActivated` no se publica (RF-COR-11) | Menor | Abierto (a confirmar con la PO) |
| DEF-22 | Límite de intentos de login evadible rotando `X-Forwarded-For` (KI-2) | Mayor | Cerrado 15/11 (PR #29, en #31) |
| DEF-23 | 25 CVE High/Critical en dependencias del backend (8 Critical, 17 High) | Crítica | Cerrado 15/11 (PR #30, en #31) |
| DEF-24 | Restricción de BD `street_closures_status_check` sin `EXPIRED` (latente) | Crítica | Cerrado 15/11 (corrección en la BD de producción) |
| DEF-25 | `infractionDateTime` de `infractionConfirmed` sin offset horario (hallado por M5) | Mayor | Cerrado 15/11 (PR #40, en #39) |
| DEF-26 | 500 con textos de más de 255 caracteres o byte NUL (hallado por ZAP) | Mayor | Cerrado 15/11 (PR #37, en #39) |
| DEF-27 | Validación del body antes que la autorización: rol sin permiso recibe 400 y no 403 (KI-3) | Menor | **Cerrado 15/11** (PR #45, en #48) |
| DEF-28 | Parámetro obligatorio faltante devuelve el JSON por defecto de Spring, no Problem Details (KI-4) | Menor | **Cerrado 15/11** (PR #45, en #48) |
| DEF-29 | `unpaid-flag` acepta `baseAmount` 0 o ausente: falta `@Valid` (KI-5/5b) | Mayor | **Cerrado 15/11** (PR #45, en #48) |
| DEF-30 | Falta la cabecera `Cross-Origin-Resource-Policy` (ZAP, Low) | Menor | **Cerrado 15/11** (PR #45, en #48) |
| DEF-31 | `updateTicketStatus` no cumplía el esquema de Core (M2 v1.78) al cerrar el operativo | Mayor | **Cerrado 15/11** (PR #44, en #48) |
| DEF-32 | `spring.jpa.show-sql=true` en producción: el relay del outbox imprime un SELECT cada 2 s | Menor | **Cerrado** (PR #50, en `main` por #52; se despliega con `integration/entrega4-f`) |
| DEF-33 | Incompatibilidad temporal con el consumidor de M3: rechaza `streetClosureEnded` con `causationId` no nulo | Mayor | **Cerrado 15/11** (circuito v1.1 validado por Core 20:51 y M3 20:54) |
| DEF-34 | Frontend: los adjuntos del descargo no se envían (el input acepta archivos que se descartan sin aviso) | Mayor | **Cerrado 15/11** (PR frontend #12, desplegado `6040e60`: adjuntos ocultos con aviso "próximamente"; la carga de adjuntos sigue en DEF-20 / HU-INF-04) |
| DEF-35 | Frontend: el panel no muestra el texto del descargo | Menor | **Cerrado — desplegado 15/11** (PR frontend #12 `6040e60` + DEF-38 en `integration/entrega4-f`) |
| DEF-36 | Frontend: el botón "Marcar impago" sigue visible tras marcar la sesión (un segundo clic da 409) | Menor | **Cerrado 15/11** (PR frontend #12, desplegado `6040e60`) |
| DEF-37 | Frontend: borrar una tarifa con sesiones activas muestra un error genérico (409 sin traducir) | Menor | **Cerrado 15/11** (PR frontend #12, desplegado `6040e60`) |
| DEF-38 | Backend: el texto del descargo no se persistía (se recibía en `AppealRequest.reason` y se perdía) | Mayor | **Cerrado — desplegado 15/11** (PR #54, en `integration/entrega4-f`); vinculado a DEF-35 |
| DEF-40 | Listado de infracciones degradado con muchos datos: N+1 de colecciones y open-in-view agotan el pool (p95 10 s, 25 % de HTTP 500 a 50 VU con 5 950 infracciones) | Mayor | **Cerrado — desplegado 15/11** (PR #55, en `integration/entrega4-f`) |
| DEF-41 | Spring Boot genera e imprime al arrancar "Using generated security password" (usuario en memoria que no se usa) | Menor | **Cerrado — desplegado 15/11** (PR #55, en `integration/entrega4-f`) |
| DEF-42 | Se podía activar un corte antes del inicio de la ventana autorizada (hallado por Core en producción) | Mayor | **Cerrado — desplegado 15/11** (PR #57 `fix/s22-activacion-en-ventana`) |

Nota de numeración: los PR #54 y #55 usaron ambos "DEF-38"; se dejó DEF-38 para el descargo y se renumeraron los de #55 a DEF-40 y DEF-41. **DEF-39 no se usa** (quedó libre al renumerar; no hay ningún defecto con ese número).

## Detalle de los cerrados en el deploy final (PR #48, `57b2765`)
### DEF-04 y DEF-14 Registro de eventos publicados y Transactional Outbox (Cerrados)
- **Defecto**: M7 publicaba directo al broker, sin registro propio (regla 9.3) y con pérdida del evento si el broker estaba caído.
- **Corrección** (PR #46, en #48): `OutboxEventPublisher` es la única implementación de `EventPublisher` y guarda el sobre en `outbox_events` dentro de la misma transacción de negocio; `OutboxRelay` publica cada 2 s con backoff exponencial (2 a 60 s), FAILED tras 15 intentos por errores propios del evento, PENDING sin límite con el broker inalcanzable, y lease para evitar duplicados entre relays. Los bytes enviados son los mismos que antes (`OutboxContractTest.contratoIdentico`). Registro consultable en `GET /api/v1/admin/outbox` (ADMINISTRADOR/AUDITOR, sin payload). El vencimiento de cortes procesa cada corte en su propia transacción.
- **Verificación**: `OutboxIntegrationTest` (7: confirmar y publicar, broker caído y vuelve, FAILED sin bloquear, rollback sin evento, scheduler, sin duplicados en paralelo, endpoint) y `OutboxContractTest` (4), en EV-1. Compose local (s12, i5): con RabbitMQ detenido la confirmación queda PENDING y al levantarlo pasa a PUBLISHED. Producción tras el deploy: tabla creada y relay activo.
- **Riesgo residual**: entrega "al menos una vez" (los consumidores deduplican por `eventId`); los FAILED no se reintentan solos.

### DEF-06 DNI en el JWT (Cerrado)
- **Corrección** (PR #42 + frontend #10, en #48): el JWT ya no lleva `nationalId` (solo `sub`, `role`, `iat`, `exp`); el DNI se resuelve en el servidor y se expone solo al propio usuario en `GET /api/v1/auth/me`; el frontend toma el DNI de `/auth/me`.
- **Verificación**: `SecurityEvidenceTests.jwtHasNoPii` (ya en el build normal) y `JwtSinDniIntegrationTest` (5); frontend 60 tests y E2E 7/7. Producción: `/auth/me` responde 401 sin token, el bundle usa `/auth/me` y el login real de las 3 cuentas funciona.
- **Riesgo residual**: el registro de DNI es en memoria; con el adaptador de M1 activo, un reinicio obligaría a los ciudadanos a reloguearse (hoy no aplica: M1 sin activar).

### DEF-16 Evidencia o validación del inspector (Cerrado)
- **Obtenido (antes)**: alta de infracción con `evidenceUrl=[]` devolvía 201 sin ninguna validación explícita. HU-INF-01 y la regla del enunciado exigen "evidencia o validación del inspector".
- **Resolución (decisión de la PO, regla del enunciado)**: se acepta con al menos una evidencia, o sin evidencia si la registra un INSPECTOR autenticado cuyo id es el `inspectorId`; si no, 400 Problem Details "La infracción requiere evidencia o la validación de un inspector" (`InfractionValidationRule`). Editar la infracción luego y quitar la evidencia es **comportamiento aceptado por la PO** (la edición solo la hace un INSPECTOR).
- **Verificación**: `InfractionEvidenceRuleTests` (5, HTTP) e `InfractionValidationRuleTest` (8, unitario); `bug04` se retiró porque contradecía la regla de la PO.

### DEF-27, DEF-28, DEF-29 y DEF-30 Hallazgos de Newman y ZAP (Cerrados)
- **Corrección** (PR #45, en #48): `@Valid` en `ParkingSessionController.flagUnpaid` (DEF-29); si falla la validación del body se evalúa antes el `@PreAuthorize` del método y un rol sin permiso recibe 403 (DEF-27); `MissingServletRequestParameter`, `ServletRequestBinding` y `MethodArgumentTypeMismatch` responden `application/problem+json` (DEF-28); cabecera `Cross-Origin-Resource-Policy: same-site` (DEF-30).
- **Verificación**: `ValidationAndHeadersDefectsTest` (7 métodos, escritos antes del fix y en rojo); BFLA estricto (403 exacto) en `SecurityEvidenceTests`. Newman en el CI de `main`: los 6 casos `[KI-1]`..`[KI-5b]` informan "corregido" (0 omitidos, 0 fallas). ZAP baseline del CI de `main`: 0 FAIL; ya no aparece la alerta de CORP.

### DEF-31 `updateTicketStatus` fuera del esquema de Core (Cerrado)
- **Defecto** (hallado al contrastar con el esquema de Core de M2 v1.78): al cerrar el operativo se publicaba `details={operationId, resolution:"<ENUM>"}`, inválido con `additionalProperties:false`, y `NO_ACTION` salía como `REJECTED` sin `cancellation.reasonCode`.
- **Corrección** (PR #44, en #48): siempre `RESOLVED` con `details={"resolution":{"type":X}}` (`ACTION_COMPLETED` o `NO_FURTHER_ACTION_REQUIRED`); `operationId` pasa a `internalMessage`; fecha con offset. Nombres de evento y campos sin cambios.
- **Verificación**: `CoreSchemaContractTest` (5) valida con los esquemas de Core en `src/test/resources/contracts/core/` serializando con el conversor de producción; el formato viejo es rechazado. Prueba real con M2: PENDIENTE (falta que M2 publique su `ticketUpdated`).

## Detalle de los cerrados antes del deploy final
### DEF-01 Credenciales de desarrollo fijas en el código, activas en producción (Cerrado)
- **Defecto**: `StubIdentityValidator` tenía las contraseñas de los usuarios de prueba escritas en el código y ese validador es el que atiende el login en producción (el adaptador M1 no está activado).
- **Corrección** (PR #36, mergeado con squash en #39, deploy `c196d7a`): cada contraseña sale de `stub.password.<usuario>` = variable de entorno `STUB_PASSWORD_<USUARIO>`, sin valor por defecto; si falta, ese usuario queda deshabilitado (401) y se registra un WARN con el **nombre** de la variable, nunca el valor. Comparación con `MessageDigest.isEqual`. En producción las 3 variables se configuran en Railway.
- **Verificación**: `git grep` en `main`: `StubIdentityValidator.java` ya no contiene contraseñas (comentario de las líneas 20-21 y métodos que arman `stub.password.<usuario>` / `STUB_PASSWORD_<USUARIO>`); el `grep -rn` de los valores anteriores da 0 resultados (s5). Test `TC-BE-AUTH-04b` (sin variable → 401 + WARN sin el valor). Compose local: los 3 usuarios 200 con la contraseña local. Producción tras el deploy: sin WARN de `STUB_PASSWORD_*` faltante y valores verificados por hash (`TABLERO.md`).
- **Verificación en producción**: login real de las 3 cuentas de prueba hecho por el equipo tras el deploy de #39 (OK). Fuera de este defecto: el adaptador de M1 sigue sin activar (`USERS_REST_ENABLED` sin definir), decisión de integración.

### DEF-05 Logs con payload completo (Cerrado)
- **Corrección** (PR #34, en #39): `LoggingEventPublisher` registra solo `[event-published] type eventId correlationId` (línea 19 en `main`); el listener de Rabbit registra `[event-rejected] type eventId correlationId error=<Clase>` y relanza una excepción sin causa (el mensaje original podía traer PII); `CoreTokenClient` y `CorePublicationRegistrar` ya no incluyen el cuerpo de la respuesta de Core.
- **Verificación**: `EventLoggingSinPiiTest` e `IncomingEventLoggingSinPiiTest` (capturan la salida con DNI y patente de prueba; fallaban antes del fix y pasan después). CI del PR #34 verde.
- **Ampliación (cerrada, PR #50)**: el log de Spring AMQP al rechazar un mensaje inválido (JSON roto o payload inválido) incluía el cuerpo del mensaje, que puede traer DNI o patente. `SafeRejectingErrorHandler` rechaza sin requeue (va a la DLQ) y registra solo el tipo de error, sin el cuerpo. Verificado con `TC-BE-TC-05` (`ConsumerRabbitContainerIntegrationTest`, RabbitMQ real por Testcontainers: el mensaje termina en la DLQ y el log capturado no contiene el DNI ni la patente).

### DEF-08 `causationId` nulo en `streetClosureEnded` (Cerrado)
- **Hallado** en el código (registro anterior) y **confirmado por Core** en la prueba real con M3 del 15/11 (Approved llevaba causationId; Ended salía con NULL). Se reclasificó a Mayor: incumple la trazabilidad del envelope de Core.
- **Corrección** (PR #33, en #31): los dos `streetClosureEnded` (finalización manual y vencimiento automático) usan `saved.getSourceEventId()` (`StreetClosureService.java`, líneas 122 y 138 en `main`). Además, log `[event-published]` en `RabbitMqEventPublisher`.
- **Verificación**: `TC-BE-SC-09` en `StreetClosureIntegrationTest` y `TC-BE-SC-U02` (fallaban antes del fix). Los eventos Ended de las pruebas del 15/11 con M3 y M6 son **anteriores** al deploy del fix. La re-prueba con M3 quedó frenada porque su consumidor v1.0 rechaza un `causationId` no nulo: ver DEF-33.

### DEF-13 State no implementado como patrón (Cerrado)
- **Corrección** (PR #35, en #39): `model/state/` (15 clases en `main`): `StreetClosureState` con una clase por estado y `InfractionState`; los servicios delegan en el estado. Las transiciones inválidas siguen respondiendo 409 con el mismo mensaje.
- **Verificación**: `StreetClosureStateTest` parametrizado (6 estados × 5 operaciones = 30 casos, 24 inválidos) e `InfractionStateTest`. Documento `docs/arquitectura/patron-state.md`.

### DEF-15 BOLA en sesiones de estacionamiento (Cerrado)
- **Corrección** (PR #32, en #31): `ParkingSession.ownerUsername` (columna nullable `owner_username`, no expuesta en el DTO). Un CIUDADANO que no es dueño recibe 404 (mismo mensaje que un id inexistente) en lectura y cierre; la pertenencia se valida antes que el estado. INSPECTOR/ADMIN/AUDITOR sin cambios.
- **Verificación**: 6 tests en `ParkingIntegrationTest.Ownership` (dueño 200; otro ciudadano 404 sin cambiar estado ni publicar eventos; sesión ajena cerrada 404; sesión sin dueño 404 para ciudadano y 200 para inspector; dueño no seteable desde el body); `SecurityEvidenceTests` SEC-BOLA-04 dejó de ser `@Tag("bug")`. Migración probada en compose local y aplicada por Hibernate en producción (`alter table parking_sessions add column owner_username`), con 0 filas antes y después.
- **Efecto visible**: sesiones creadas antes del deploy o por un inspector no las puede cerrar un ciudadano (lo hace un inspector).

### DEF-22 Límite de login evadible con `X-Forwarded-For` (KI-2, Cerrado)
- **Defecto** (hallado por Newman, T1): el filtro tomaba la IP del cliente de `X-Forwarded-For` tal cual llegaba; rotando el header se esquivaba el 429.
- **Corrección** (PR #29, en #31): la IP se toma contando `login.rate-limit.trusted-proxies` (por defecto 1) entradas desde la derecha; se agregó un contador por usuario (`max-attempts-per-username`, 10 en 60 s). También se corrigió una excepción con `X-Forwarded-For: ,`.
- **Verificación**: `LoginRateLimitIntegrationTest` (5) y `LoginRateLimitFilterTest` (4). Sonda en producción tras el deploy `70e9afe1`: 10 logins fallidos con `X-Forwarded-For` distinto → 401 ×10 y luego 429.
- **Riesgo residual**: bloqueo temporal (60 s) de una cuenta ajena fallando 10 veces; contador en memoria (una instancia).

### DEF-23 CVE High/Critical en dependencias (Cerrado)
- **Defecto** (T2): Trivy sobre el jar: 25 CVE High/Critical (8 Critical, 17 High) en spring-webmvc, tomcat-embed-core, jackson 2 y 3, amqp-client, postgresql y netty.
- **Corrección** (PR #30, en #31): parent Spring Boot 4.1.0 → 4.1.2 y `amqp-client` 5.34.0 por property. Job de CI `dependencias` (Trivy fijado por SHA, HIGH/CRITICAL, falla el build).
- **Verificación**: Trivy 0 High/Critical sobre el jar construido; job de CI en verde. Compose local con AMQP OK y deploy en producción conectado a Core. Frontend: `npm audit --omit=dev` 0 y Trivy 0.

### DEF-24 Restricción de BD sin `EXPIRED` (latente, Cerrado)
- **Defecto**: `street_closures_status_check` en la base de producción no incluía `EXPIRED` (el enum sí). Causa raíz: `spring.jpa.hibernate.ddl-auto=update` no recrea los check constraints de tablas existentes cuando se agrega un valor al enum. Un corte APPROVED vencido sin activar hacía fallar `expireDueClosures` (una sola transacción) → rollback de los ACTIVE→FINISHED cuyos eventos ya se habían publicado → `streetClosureEnded` repetido en cada corrida del scheduler. **Nunca se disparó** (no había cortes APPROVED). Severidad Crítica por el riesgo de integridad (eventos duplicados hacia otros módulos, sin workaround).
- **Corrección** (15/11, ejecutada por Alejo en Supabase, probada antes en Postgres local): constraint recreado con `EXPIRED`. No hubo cambio de código.
- **Verificación**: constraint nuevo presente; filas intactas (2 REQUESTED, 2 FINISHED).
- **Mejora posterior (PR #46, en #48)**: el vencimiento procesa cada corte en su propia transacción y los eventos salen por el outbox, así que un corte que falla ya no revierte a los demás ni deja eventos de una transacción revertida.

### DEF-25 `infractionDateTime` sin offset (hallado por M5, Cerrado)
- **Defecto**: `infractionConfirmed` publicaba `infractionDateTime` como `LocalDateTime` (sin zona); M5 lo pidió con offset (contrato v4).
- **Corrección** (PR #40, en #39): `InfractionConfirmedPayload.infractionDateTime` es `OffsetDateTime` (mismo nombre de campo), convertido con America/Argentina/Buenos_Aires.
- **Verificación**: `InfractionConfirmedOffsetTest` serializa con el conversor de producción y espera `...-03:00`. Falta que M5 lo confirme con un evento real (ver 04, sección 7).

### DEF-26 500 con textos de más de 255 caracteres o byte NUL (hallado por ZAP, Cerrado)
- **Defecto**: ZAP activo autenticado (Medium "Buffer Overflow" y Low "Server Error"): con textos >255 la columna `varchar(255)` fallaba y la API respondía 500; con `\u0000`, error de codificación de PostgreSQL → 500.
- **Corrección** (PR #37, en #39): `@Size(max = 255)` en los String de los DTOs de request y embebidos; `RejectNulCharactersWebConfig` rechaza NUL → 400 Problem Details.
- **Verificación**: `InputLimitsValidationTests` (256 → 400, 255 → 201, NUL → 400) y re-escaneo ZAP: 0 High, 0 Medium ([evidencias/zap](evidencias/zap/README.md)).

## Detalle de los abiertos y aceptados
### DEF-42 Activación de un corte antes del inicio de la ventana autorizada — Cerrado (Mayor)
- **Hallado por Core** en producción el 15/11, en la re-prueba del circuito v1.1 con M3 (DEF-33): un corte APPROVED con ventana autorizada `startDate` 19:46:41 se activó a las 19:45:51, 50 s antes del inicio. **M3 confirmó** que no es válido: solo se puede activar dentro de la ventana autorizada `[startDate, endDate)`. Es el mismo defecto que DEF-07 (registrado antes desde el código como Menor, sin workaround técnico); DEF-07 queda como duplicado.
- **Causa raíz**: `ApprovedState.activate` solo validaba el estado; no comparaba la hora actual con la ventana. El script de los circuitos aprueba con inicio = ahora + 1 min y activa enseguida, por eso se disparó.
- **Por qué Mayor**: incumple una regla de negocio acordada con los módulos de origen y produce un comportamiento visible para otros módulos (el corte figura ACTIVE en la consulta ciudadana antes de lo autorizado). No es Crítico: no hay pérdida de datos ni evento erróneo (la activación no se publica) y hay workaround (activar recién al llegar `startDate`).
- **Corrección** (PR `fix/s22-activacion-en-ventana`): `StreetClosureState.activate(closure, now)`; `ApprovedState` rechaza `now < startDate` o `now >= endDate` con `InvalidStateTransitionException` → **409 Problem Details** "El corte solo puede activarse dentro de la ventana autorizada (desde … hasta …)". La hora sale de un `java.time.Clock` inyectable (bean nuevo `ClockConfig`, reloj del sistema). Además la aprobación valida la ventana: inicio ≥ fin, o un fin nuevo ya vencido → **400** (un inicio pasado con fin futuro sigue permitido). La **finalización manual** de un ACTIVE se mantiene en cualquier momento, también antes de `endDate` (la obra puede terminar antes; decisión documentada en `StreetClosureState.finish`). Sin cambios de rutas, DTOs, eventos ni esquema.
- **Verificación**: `StreetClosureStateTest` (antes del inicio con el caso real 19:45:51 → 409; en `startDate` → ACTIVE; en `endDate` y después → 409), `StreetClosureActivationWindowTest` (reloj fijo: 409 sin guardar ni publicar; cierre anticipado permitido; aprobación con ventana inválida → 400) y `TC-BE-SC-DEF42-01..04` en `StreetClosureIntegrationTest` (reloj de test movible). Los tests fallaban antes del fix. **Cerrado — desplegado 15/11** (PR #57).

### DEF-33 Incompatibilidad temporal con el consumidor de M3 por `causationId` — Cerrado (Mayor)
- **Situación**: desde DEF-08, `streetClosureEnded` (y `streetClosureRejected`) llevan `causationId` = `eventId` de la solicitud, como exige el envelope de Core. El consumidor v1.0 de M3 rechazaba `streetClosureEnded` con `causationId` no nulo (aviso de M3 del 15/11 18:08); M3 lo adaptó (v1.1) para exigir `causationId` = `eventId` del `streetClosureRequested`. `eventVersion` sigue en "1.0" (el contrato v1.1 es documental).
- **Por qué fue Mayor**: bloqueaba un flujo principal con uno de los dos módulos de origen; había workaround (no aprobar cortes de M3 hasta su deploy).
- **Re-prueba del 15/11 19:45–19:48**: circuito v1.1 completo; `streetClosureApproved` `614222ce-715d-44e7-8e63-89250488ea7b` y `streetClosureEnded` `7f098173-82c6-4831-849f-47a3b5ec0cb4`.
- **Cierre (15/11 20:51 y 20:54)**: **Core** validó el circuito: Approved y Ended ROUTED y DELIVERED a `q.obras` y `q.ambiente` en 1 intento, correlationId `3f2e4d0b-…` y causationId `7f371af0-…` correctos, 0 dead letters. **M3** confirmó ambos PROCESSED y el corte en estado ENDED. En esa misma prueba Core detectó la activación anticipada (DEF-42).
- **Causa raíz**: del lado del consumidor de M3; se registró porque afectaba el comportamiento visible de M7 y fue un cambio nuestro el que lo destapó.

### DEF-32 `show-sql` activo en producción — Cerrado (Menor)
- **Situación anterior**: `spring.jpa.show-sql=true` en `application.properties`; el relay del outbox imprimía un SELECT cada 2 s en los logs de Railway (sin valores: Hibernate usa `?`).
- **Corrección** (PR #50): `spring.jpa.show-sql=false` en `application.properties` (línea 7). No renombra variables ni toca el esquema. En `main` desde #52; en producción se cierra con el deploy de `integration/entrega4-f`.

### DEF-34 Adjuntos del descargo no se envían — Cerrado (Mayor)
- **Hallado** al escribir el manual de usuario (S18): la pantalla de descargo del ciudadano tiene un selector de archivos, pero `AppealRequest` del backend solo recibe `reason`; los archivos elegidos se descartan sin aviso.
- **Por qué Mayor**: el ciudadano cree que presentó documentación que el sistema nunca recibió (pérdida silenciosa de datos aportados por el ciudadano en un trámite con plazos). Hay workaround (presentar la documentación por mesa de entradas), así que no es Crítico.
- **Corrección**: PR frontend #12 (`fix/s19-fallas-ui`): se quita el input y se muestra "Adjuntos: próximamente; enviá la documentación por mesa de entradas". **Cerrado 15/11**: PR #12 mergeado y desplegado en producción (`6040e60`); el input ya no aparece y se ve el aviso "próximamente". La **carga** de adjuntos sigue siendo parte de la HU pendiente HU-INF-04 (DEF-20, abierto), con workaround en `docs/entrega-academica/hu-pendientes.md`.

### DEF-35, DEF-36 y DEF-37 Fallas de UI del frontend — Cerrados (Menores)
- **Hallados** al escribir el manual de usuario (S18). Corrección en PR frontend #12 (`fix/s19-fallas-ui`), mergeado y desplegado en producción (`6040e60`).
- **DEF-35** Texto del descargo: `InfractionResponse` no devuelve el motivo (el backend no lo guarda; solo viaja en `infractionAppealed`). El front ya muestra `appealReason` si llega y explica el pendiente. La causa raíz es DEF-38 (el equipo aprobó la columna nueva); DEF-38 entra en `integration/entrega4-f` y el front ya lo muestra: **cerrado — desplegado 15/11** (el texto del descargo se persiste y se muestra).
- **DEF-36** "Marcar impago": el botón seguía visible y el segundo intento daba 409. El PR oculta el botón con `originParkingSessionId` y muestra el `detail` del Problem Details si igual llega 409. Menor: el backend ya protege la regla.
- **DEF-37** Borrar tarifa con sesiones activas: el 409 se mostraba como error genérico. El PR lo traduce a un mensaje claro. Menor: solo mensaje.

### DEF-40 Listado de infracciones degradado con muchos datos — Cerrado (Mayor)
- **Hallado** con k6 (PR #53, `load.js` 50 VU con la base poblada) y reproducido localmente: con 5 950 infracciones, p95 10,1 s y 24,6-25,6 % de HTTP 500 (`Connection is not available` del pool Hikari de 3 conexiones).
- **Causa raíz** (log de sentencias de PostgreSQL): cada `GET /infractions` ejecutaba 11 901 consultas (1 + una por fila para `evidenceUrl` y otra para `aggravatingFactors`), en la vista gracias a `spring.jpa.open-in-view=true`, que además retenía la conexión hasta terminar de serializar 3,6 MB.
- **Por qué Mayor**: degrada el servicio y arrastra a otros endpoints (cortes vigentes p95 7,8 s); workaround: volumen real bajo. No es Crítico: no hay pérdida de datos.
- **Corrección** (PR backend `perf/s21-listado-infracciones`): `open-in-view=false`; colecciones inicializadas dentro de la transacción de solo lectura del servicio; listado completo con 2 JOIN FETCH; `hibernate.default_batch_fetch_size=500` para el resto (cortes, operativos, accidentes, acarreos). Contrato REST, entidades y esquema sin cambios; pool sigue en 3.
- **Verificación**: `ListadoConDatosRegressionTest` (40 filas en ≤ 3 consultas) y toda la suite con open-in-view apagado. k6 50 VU x 5 min: p95 global 309 ms, listado 424 ms, 0 HTTP 500 (`evidencias/k6/con-datos.md`). En la integración se agregó `listadoConDescargoSinNMasUno`: 40 infracciones apeladas, el listado trae `appealReason` (columna simple de la entidad) en ≤ 3 consultas. **Cerrado — desplegado 15/11** (PR #55 en `integration/entrega4-f`).

### DEF-41 Password generado en el log de arranque — Cerrado (Menor)
- `UserDetailsServiceAutoConfiguration` creaba un usuario en memoria (que no se usa: la autenticación es JWT) e imprimía su contraseña al arrancar, también en Railway. Corrección (mismo PR): se excluye esa autoconfiguración en `TransitoBackendApplication`; verificado: el log de arranque ya no la muestra, el login JWT sigue igual (suite completa y k6) y un test comprueba que no hay `UserDetailsService`. **Cerrado — desplegado 15/11** (PR #55 en `integration/entrega4-f`).

### DEF-38 El texto del descargo no se persistía — Cerrado (Mayor)
- **Hallado** al analizar DEF-35: `POST /api/v1/infractions/{id}/appeal` valida `reason` (`@NotBlank`, `@Size(max = 2000)`) y lo publica en `infractionAppealed`, pero `Infraction` no tenía dónde guardarlo: el descargo no quedaba en la base y nadie de Tránsito (inspector, administrador, auditor) podía leerlo después para resolver la apelación.
- **Por qué Mayor**: pérdida silenciosa del dato central de un trámite del ciudadano. No es Crítico porque el texto sí llegaba a Core en el evento.
- **Corrección** (PR #54, `fix/s20-persistir-descargo`, en `integration/entrega4-f`; **cerrado — desplegado 15/11**): columna nueva `infractions.appeal_reason` (`varchar(2000)`, nullable, sin default; la crea `ddl-auto=update`, igual que `owner_username`), se guarda en la transición PENDING_APPEAL → APPEALED y se expone como campo **opcional** `appealReason` en `InfractionResponse` (aditivo; mismas reglas de acceso que el GET: titular y personal). No se agrega a ningún evento nuevo (`infractionConfirmed`/`infractionAnulled` no lo llevan) ni se loguea. `infractionAppealed` mantiene su campo `reason` como antes (contrato congelado).
- **Verificación**: tests TC-BE-INF-15b/15c/15d en `InfractionIntegrationTest`; migración probada en el compose local (fila apelada en `main` queda con `appeal_reason` null, la app arranca UP sobre la misma base y una apelación nueva guarda el texto).

### DEF-07 Activación antes de `startDate` — Duplicado de DEF-42
- Registrado desde el código (Menor). Core lo encontró en producción el 15/11 y se registró como DEF-42 (Mayor); se corrige y se cuenta allí.

### DEF-09 Swagger público — Aceptado
- `SecurityConfig` permite `/swagger-ui/**`, `/v3/api-docs/**` y `/openapi.yaml`. **Decisión**: documentación de API pública intencionalmente para la integración entre módulos. `application-prod.properties` apaga springdoc con el perfil `prod`, pero producción usa `SPRING_PROFILES_ACTIVE=dev`, así que hoy no tiene efecto (a propósito).

### DEF-10 `auth=false` de M1 devuelve 502 — Abierto
- Probado en `RestIdentityContextTest.loginAuthFalse`. Con el adaptador M1 sin activar no se dispara en producción.

### DEF-12 Contraseña de M1 en el historial — Aceptado
- gitleaks `--all`: 4 hallazgos documentados en `.gitleaksignore`; en HEAD están redactados. Decisión del humano del 15/11: ambiente académico, no se avisa ni se rota.

### DEF-17 y DEF-18 Pruebas desactualizadas — Abierto
- Corrida del grupo `bug` sobre `57b2765`: `bug09a` envía `finalAmount=1` y espera 200 (recibe 400, coherente con la regla de importes); `bug02` y `bug14` fallan al preparar el corte ("affectedSections es obligatorio", contrato actual del handler). Acción: actualizarlas o retirarlas. No afectan el build normal.

### DEF-19 Importes no calculados — Abierto (Mayor)
- `CreateInfractionRequest` recibe `baseAmount` y `finalAmount`; no hay tabla de importes por tipo, reincidencia ni cálculo de estadía. **Workaround**: carga manual del importe por el inspector o el administrador (validada: importes positivos, final ≥ base).

### DEF-20 HU sin implementar — Abierto (Mayor)
- HU-COR-05, HU-OPE-03 (controles), HU-EST-03/04 (detección y descarte de incumplimientos), adjuntos y `appealId` de HU-INF-04, filtros e indicadores del backend (HU-IND-01/02). Ver la matriz 02.
- **Workaround operativo documentado** (PR #51): `docs/entrega-academica/hu-pendientes.md` detalla, para cada uno de los siete criterios de las seis HU, qué falta (verificado en el código) y cómo lo resuelve hoy el usuario usando solo endpoints existentes (por ejemplo, adjuntos por mesa de entradas). No es un workaround técnico: la función no existe; sigue Abierto como Mayor y conviene que la PO confirme el alcance.

### DEF-21 `streetClosureActivated` — Abierto
- El payload existe pero no se publica a Core (el catálogo registrado tiene 6 tipos). Confirmar con la PO.

## Conteo por severidad y riesgo de salida
Criterio del profesor: 0 Bloqueantes, 0 Críticos, como máximo 3 Mayores abiertos (con workaround). Los Aceptados y los duplicados se cuentan aparte. Total registrado: **41 defectos** (DEF-01 a DEF-42; DEF-39 sin uso). Sin números duplicados (verificado con `grep`). DEF-42 figura cerrado porque el PR #57 se mergea y despliega apenas pasa el CI.
| Severidad | Abiertos | Cerrados | Aceptados | Duplicados | Límite (abiertos) | ¿Cumple? |
|---|---|---|---|---|---|---|
| Bloqueante | 0 | 0 | 0 | 0 | 0 | Sí |
| Crítica | **0** | 3 (DEF-01, 23, 24) | 0 | 0 | 0 | Sí |
| Mayor | **2** (DEF-19, 20) | 21 (DEF-02, 03, 04, 05, 06, 08, 11, 13, 14, 15, 16, 22, 25, 26, 29, 31, 33, 34, 38, 40, 42) | 0 | 0 | 3 | Sí |
| Menor | **4** (DEF-10, 17, 18, 21) | 8 (DEF-27, 28, 30, 32, 35, 36, 37, 41) | 2 (DEF-09, 12) | 1 (DEF-07 → DEF-42) | sin límite | — |
| **Total** | **6** | **32** | **2** | **1** | | |

**Riesgo de salida (sin maquillar)**: 0 Bloqueantes, 0 Críticos y 2 Mayores abiertos (DEF-19 y DEF-20, ambos con workaround), uno por debajo del límite. No se bajó ninguna severidad para llegar al número. Salvedades:
1. DEF-38, DEF-40, DEF-41 (y DEF-32 y la ampliación de DEF-05, ya en `main`) figuran "cerrado — desplegado 15/11" porque `integration/entrega4-f` se mergea y despliega inmediatamente después del CI; **si ese deploy no ocurre, vuelven a "abierto" y hay 5 Mayores abiertos** (no cumple).
2. DEF-20 tiene workaround operativo documentado (`hu-pendientes.md`), no técnico; conviene que la PO confirme la decisión de alcance.
3. DEF-33 quedó cerrado (Core y M3 validaron el circuito v1.1 el 15/11 20:51/20:54) y DEF-42 (nuevo, Mayor) figura cerrado con el PR #57; si ese deploy no ocurriera, DEF-42 volvería a abierto y habría 3 Mayores (en el límite).
4. Cualquier Mayor nuevo deja el criterio sin cumplir.
