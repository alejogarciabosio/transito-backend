# 05 - Registro de defectos

Actualizado: 15/11/2026 ~18:00 ART. Versión de referencia: `main` del backend en `c196d7a` (merge del PR #39, desplegado en Railway como `c196d7a`) y `main` del frontend (PR #9 mergeado).
Fuentes: código de `main` (`git grep`, cada afirmación de "cerrado" se revisó en el código), reportes de los agentes (s1, s2, s4-s10, t1, t2), `TABLERO.md` y `pruebas-modulos-15-11.md`. No se hicieron pruebas nuevas contra producción para este registro.

## Severidades usadas
No tuve a mano el PDF de la cátedra con las definiciones del profesor; usé estas definiciones de trabajo y **hay que contrastarlas con las del profesor antes de entregar**:
| Severidad | Definición de trabajo |
|---|---|
| Bloqueante | impide usar o desplegar el sistema, o corrompe datos; sin workaround |
| Crítica | riesgo grave de seguridad o de pérdida de integridad en producción, o falla de un flujo principal sin workaround |
| Mayor | incumple un requisito o criterio importante o degrada el servicio; existe workaround |
| Menor | desvío cosmético, de bajo riesgo o de documentación |

Estados: **Abierto**, **En desarrollo** (abierto, con trabajo en curso; sigue contando como abierto), **Cerrado** (corregido en `main`, desplegado y verificado con test o prueba), **Aceptado** (riesgo aceptado por decisión documentada).

## Tabla resumen
| Id | Título | Severidad | Estado |
|---|---|---|---|
| DEF-01 | Credenciales de desarrollo fijas en el código, activas en producción (stub de login) | Crítica | **Cerrado** 15/11 (PR #36, en #39) |
| DEF-02 | `/actuator/health` colgado con el broker caído | Mayor | Cerrado (PR #24, en #25) |
| DEF-03 | Adaptador REST de M1 no arrancaba (Jackson) | Mayor | Cerrado (PR #23, en #25) |
| DEF-04 | Sin registro de eventos publicados (regla 9.3 del TP) | Mayor | Abierto, en desarrollo (outbox) |
| DEF-05 | Logs con payload completo de eventos | Mayor | **Cerrado** 15/11 (PR #34, en #39) |
| DEF-06 | DNI (`nationalId`) dentro del JWT | Mayor | Corregido en PR #42 (`fix/s11-jwt-sin-dni`), pendiente de merge a `main` y deploy |
| DEF-07 | Un corte se activa antes de `startDate` | Menor | Abierto |
| DEF-08 | `causationId` nulo en `streetClosureEnded` (hallado también por Core) | Mayor (reclasificado desde Menor al confirmarlo Core) | **Cerrado** 15/11 (PR #33, en #31) |
| DEF-09 | Swagger y `/v3/api-docs` públicos en producción | Menor | Aceptado |
| DEF-10 | 200 con `auth=false` de M1 devuelve 502 | Menor | Abierto (comportamiento testeado) |
| DEF-11 | Estacionamiento impago devolvía 500 | Mayor | Cerrado (verificado el 15/11) |
| DEF-12 | Contraseña de cuentas de prueba de otro módulo (M1) en el historial de Git | Menor | Aceptado |
| DEF-13 | State no implementado como patrón en el corte de calle | Mayor (criterio académico) | **Cerrado** 15/11 (PR #35, en #39) |
| DEF-14 | Outbox transaccional no implementado | Mayor | Abierto, en desarrollo |
| DEF-15 | BOLA: un ciudadano lee y cierra la sesión de estacionamiento de otro (KI-1) | Mayor | **Cerrado** 15/11 (PR #32, en #31) |
| DEF-16 | Infracción sin evidencia se acepta (201) | Mayor | Corregido en PR #47 (`fix/s15-evidencia-o-inspector`), pendiente de merge a `main` y deploy |
| DEF-17 | `PATCH /infractions` parcial devuelve 400 en la reproducción BUG-BE-09a | Menor (probable test desactualizado) | Abierto (a confirmar) |
| DEF-18 | Pruebas de reproducción BUG-BE-02 y 14 desactualizadas | Menor (deuda de tests) | Abierto |
| DEF-19 | Importe base, reincidencia y costo de acarreo/estadía no se calculan en el servidor | Mayor | Abierto |
| DEF-20 | HU sin implementar (COR-05, OPE-03, EST-03/04, INF-04, IND) | Mayor | Abierto |
| DEF-21 | `streetClosureActivated` no se publica (RF-COR-11) | Menor | Abierto (a confirmar con el PO) |
| DEF-22 | Límite de intentos de login evadible rotando `X-Forwarded-For` (KI-2) | Mayor | **Cerrado** 15/11 (PR #29, en #31) |
| DEF-23 | 25 CVE High/Critical en dependencias del backend (8 Critical, 17 High) | Crítica | **Cerrado** 15/11 (PR #30, en #31) |
| DEF-24 | Restricción de BD `street_closures_status_check` sin `EXPIRED` (latente) | Crítica | **Cerrado** 15/11 (corrección en la BD de producción) |
| DEF-25 | `infractionDateTime` de `infractionConfirmed` sin offset horario (hallado por M5) | Mayor | **Cerrado** 15/11 (PR #40, en #39) |
| DEF-26 | 500 con textos de más de 255 caracteres o byte NUL (hallado por ZAP) | Mayor | **Cerrado** 15/11 (PR #37, en #39) |
| DEF-27 | Validación del body antes que la autorización: rol sin permiso recibe 400 y no 403 (KI-3) | Menor | Abierto |
| DEF-28 | Parámetro obligatorio faltante devuelve el JSON por defecto de Spring, no Problem Details (KI-4) | Menor | Abierto |
| DEF-29 | `unpaid-flag` acepta `baseAmount` 0 o ausente: falta `@Valid` (KI-5/5b) | Mayor | Abierto |
| DEF-30 | Falta la cabecera `Cross-Origin-Resource-Policy` (ZAP, Low) | Menor | Abierto |

## Detalle de los cerrados el 15/11
### DEF-01 Credenciales de desarrollo fijas en el código, activas en producción (Cerrado)
- **Defecto**: `StubIdentityValidator` tenía las contraseñas de los usuarios de prueba escritas en el código y ese validador es el que atiende el login en producción (el adaptador M1 no está activado).
- **Corrección** (PR #36, mergeado con squash en #39, deploy `c196d7a`): cada contraseña sale de `stub.password.<usuario>` = variable de entorno `STUB_PASSWORD_<USUARIO>`, sin valor por defecto; si falta, ese usuario queda deshabilitado (401) y se registra un WARN con el **nombre** de la variable, nunca el valor. Comparación con `MessageDigest.isEqual`. En producción las 3 variables se configuran en Railway.
- **Verificación**: `git grep` en `main`: `StubIdentityValidator.java` ya no contiene contraseñas (comentario de las líneas 20-21 y métodos que arman `stub.password.<usuario>` / `STUB_PASSWORD_<USUARIO>`); el `grep -rn` de los valores anteriores da 0 resultados (s5). Test `TC-BE-AUTH-04b` (sin variable → 401 + WARN sin el valor). Compose local: los 3 usuarios 200 con la contraseña local. Producción tras el deploy: sin WARN de `STUB_PASSWORD_*` faltante y valores verificados por hash (`TABLERO.md`).
- **Pendiente**: prueba de login real de admin1/inspector1/ciudadano1 en producción por una persona del equipo. Fuera de este defecto: el adaptador de M1 sigue sin activar (`USERS_REST_ENABLED` sin definir), decisión de integración.

### DEF-05 Logs con payload completo (Cerrado)
- **Corrección** (PR #34, en #39): `LoggingEventPublisher` registra solo `[event-published] type eventId correlationId` (línea 19 en `main`); el listener de Rabbit registra `[event-rejected] type eventId correlationId error=<Clase>` y relanza una excepción sin causa (el mensaje original podía traer PII); `CoreTokenClient` y `CorePublicationRegistrar` ya no incluyen el cuerpo de la respuesta de Core.
- **Verificación**: `EventLoggingSinPiiTest` e `IncomingEventLoggingSinPiiTest` (capturan la salida con DNI y patente de prueba; fallaban antes del fix y pasan después). CI del PR #34 verde.

### DEF-08 `causationId` nulo en `streetClosureEnded` (Cerrado)
- **Hallado** en el código (registro anterior) y **confirmado por Core** en la prueba real con M3 del 15/11 (Approved llevaba causationId; Ended salía con NULL). Se reclasificó a Mayor: incumple la trazabilidad del envelope de Core.
- **Corrección** (PR #33, en #31): los dos `streetClosureEnded` (finalización manual y vencimiento automático) usan `saved.getSourceEventId()` (`StreetClosureService.java`, líneas 122 y 138 en `main`). Además, log `[event-published]` en `RabbitMqEventPublisher`.
- **Verificación**: `TC-BE-SC-09` en `StreetClosureIntegrationTest` y `TC-BE-SC-U02` (fallaban antes del fix). Los eventos Ended de las pruebas del 15/11 con M3 y M6 son **anteriores** al deploy del fix: falta un Ended posterior con causationId confirmado por Core.

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
- **Mejora pendiente (no es defecto abierto)**: procesar cada corte vencido en su propia transacción u outbox para que un error no revierta los demás.

### DEF-25 `infractionDateTime` sin offset (hallado por M5, Cerrado)
- **Defecto**: `infractionConfirmed` publicaba `infractionDateTime` como `LocalDateTime` (sin zona); M5 lo pidió con offset (contrato v4).
- **Corrección** (PR #40, en #39): `InfractionConfirmedPayload.infractionDateTime` es `OffsetDateTime` (mismo nombre de campo), convertido con America/Argentina/Buenos_Aires.
- **Verificación**: `InfractionConfirmedOffsetTest` serializa con el conversor de producción y espera `...-03:00`. Falta que M5 lo confirme con un evento real (ver 04, sección 7).

### DEF-26 500 con textos de más de 255 caracteres o byte NUL (hallado por ZAP, Cerrado)
- **Defecto**: ZAP activo autenticado (Medium "Buffer Overflow" y Low "Server Error"): con textos >255 la columna `varchar(255)` fallaba y la API respondía 500; con `\u0000`, error de codificación de PostgreSQL → 500.
- **Corrección** (PR #37, en #39): `@Size(max = 255)` en los String de los DTOs de request y embebidos; `RejectNulCharactersWebConfig` rechaza NUL → 400 Problem Details.
- **Verificación**: `InputLimitsValidationTests` (256 → 400, 255 → 201, NUL → 400) y re-escaneo ZAP: 0 High, 0 Medium ([evidencias/zap](evidencias/zap/README.md)).

## Detalle de los abiertos
### DEF-04 Sin registro de eventos publicados — Abierto, en desarrollo
- No existe tabla de eventos publicados (`git grep -i outbox` en `src/main`: sin resultados). Mitigación parcial desde el PR #33: cada publicación deja un log `[event-published] type eventId correlationId`. **Workaround**: logs de Railway y auditoría de Core. Se resuelve junto con DEF-14 (outbox, en desarrollo).

### DEF-06 DNI en el JWT — Corregido en PR #42, pendiente de merge y deploy
- **Fix (PR #42)**: el JWT ya no lleva `nationalId`; el DNI se resuelve en el servidor y se expone solo en `GET /api/v1/auth/me`. `SecurityEvidenceTests.jwtHasNoPii` PASA en la rama.
- Situación previa: - `JwtService.java` línea 34 en `main`: `.claim("nationalId", user.nationalId())`. `SecurityEvidenceTests.jwtHasNoPii` falla. El frontend y los filtros por titular usan el claim, por eso el cambio se coordina. **Workaround**: ninguno técnico simple.

### DEF-07 Activación antes de `startDate` — Abierto
- La activación (`ApprovedState.activate`) solo valida el estado; no compara con `startDate`. **Workaround**: operativo.

### DEF-09 Swagger público — Aceptado
- `SecurityConfig` (línea 57) permite `/swagger-ui/**`, `/v3/api-docs/**` y `/openapi.yaml`. **Decisión**: documentación de API pública intencionalmente para la integración entre módulos. `application-prod.properties` apaga springdoc con el perfil `prod`, pero producción usa `SPRING_PROFILES_ACTIVE=dev`, así que hoy no tiene efecto (a propósito).

### DEF-10 `auth=false` de M1 devuelve 502 — Abierto
- Probado en `RestIdentityContextTest.loginAuthFalse`. Con el adaptador M1 sin activar no se dispara en producción.

### DEF-12 Contraseña de M1 en el historial — Aceptado
- gitleaks `--all`: 4 hallazgos documentados en `.gitleaksignore`; en HEAD están redactados. Decisión del humano del 15/11: ambiente académico, no se avisa ni se rota.

### DEF-14 Outbox no implementado — Abierto, en desarrollo
- `git grep -i outbox` en `src/main`: sin resultados en `main`. Con el broker caído, un evento publicado se pierde. **Workaround**: reenvío manual desde Core u otros módulos.

### DEF-16 Evidencia no exigida — Corregido en PR #47, pendiente de merge y deploy
- **Obtenido (antes)**: alta de infracción con `evidenceUrl=[]` devolvía 201 (`BugReproductionTest.bug04`). HU-INF-01 y RF-INF-05 exigen evidencia o validación. **Resolución (PO)**: se acepta con evidencia, o sin ella si la registra un INSPECTOR autenticado cuyo id es el `inspectorId` (regla en `InfractionValidationRule`; si no, 400 Problem Details). `bug04` se reemplazó por `InfractionEvidenceRuleTests` e `InfractionValidationRuleTest`.

### DEF-17 y DEF-18 Pruebas desactualizadas — Abierto
- `bug09a` envía `finalAmount=1` y espera 200 (recibe 400, coherente con la regla de importes); `bug02` y `bug14` fallan al preparar el corte por el contrato del handler. Acción: actualizarlas o retirarlas.

### DEF-19 Importes no calculados — Abierto
- `CreateInfractionRequest` recibe `baseAmount` y `finalAmount`; no hay tabla de importes por tipo, reincidencia ni cálculo de estadía. **Workaround**: carga manual.

### DEF-20 HU sin implementar — Abierto
- Ver la matriz 02. Se podría reclasificar como "fuera de alcance acordado" solo con decisión documentada de la PO.

### DEF-21 `streetClosureActivated` — Abierto
- El payload existe pero no se publica a Core (el catálogo registrado tiene 6 tipos). Confirmar con el PO.

### DEF-27 Validación antes que autorización (KI-3) — Abierto
- Newman (T1): un rol sin permiso que manda un body inválido recibe 400 (revela reglas de validación) en vez de 403. La autorización se sigue aplicando (no cambia estado). En `main` no hay cambio al respecto; el caso sigue marcado `[KI-3]` en la colección.

### DEF-28 Parámetro faltante sin Problem Details (KI-4) — Abierto
- `GlobalExceptionHandler` en `main` no maneja `MissingServletRequestParameterException`: la respuesta sale con el formato por defecto de Spring (`application/json`), no RFC 9457. Caso `[KI-4]` en la colección.

### DEF-29 `unpaid-flag` sin `@Valid` (KI-5/5b) — Abierto
- `ParkingSessionController.flagUnpaid` recibe `@RequestBody FlagUnpaidRequest request` sin `@Valid` (verificado en `main`), por lo que `@NotNull` y `@DecimalMin("0.01")` no se aplican: se puede crear una infracción por 0 o sin importe. En la corrida de Newman de s5 se observó además un 500 en `unpaid-flag` en un caso; probablemente el de importe ausente (no confirmado). **Workaround**: el inspector carga el importe. Arreglo de una línea, pendiente.

### DEF-30 Falta `Cross-Origin-Resource-Policy` (ZAP Low) — Abierto
- Única alerta Low que quedó en el re-escaneo ZAP. Riesgo bajo (API JSON con JWT y CORS restringido).

## Conteo por severidad y riesgo de salida
Criterio del profesor: 0 Bloqueantes, 0 Críticos, como máximo 3 Mayores abiertos (con workaround). "Abiertos" incluye los "en desarrollo"; los Aceptados se cuentan aparte.
| Severidad | Abiertos | Cerrados | Aceptados | Límite (abiertos) | ¿Cumple? |
|---|---|---|---|---|---|
| Bloqueante | 0 | 0 | 0 | 0 | Sí |
| Crítica | **0** | 3 (DEF-01, 23, 24) | 0 | 0 | Sí |
| Mayor | **7** (DEF-04, 06, 14, 16, 19, 20, 29) | 10 (DEF-02, 03, 05, 08, 11, 13, 15, 22, 25, 26) | 0 | 3 | **No** |
| Menor | **8** (DEF-07, 10, 17, 18, 21, 27, 28, 30) | 0 | 2 (DEF-09, 12) | sin límite | — |
| **Total** | **15** | **13** | **2** | | |

**Al 15/11 18:00 no se cumple la condición de salida por los Mayores (7 > 3).** No se bajó ninguna severidad para mejorar el número. Caminos defendibles para cumplirla:
1. DEF-04 y DEF-14 se cierran juntos con el outbox (en desarrollo).
2. DEF-06 (DNI fuera del JWT, corregido en PR #42; falta merge y deploy).
3. DEF-29: agregar `@Valid` y un test (cambio chico de código).
4. DEF-16: corregido en PR #47 (evidencia o inspector autenticado); falta merge y deploy.
5. DEF-19 y DEF-20 son brechas de requisito: solo bajan con una decisión formal de alcance de la PO, documentada y comunicada al profesor.
