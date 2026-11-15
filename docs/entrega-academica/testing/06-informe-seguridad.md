# 06 - Informe de seguridad

Fecha: 15/11/2026. Estados: **CUMPLE** (hay test o evidencia ejecutada), **NO CUMPLE** (hay evidencia de incumplimiento), **NO VERIFICABLE** (sin evidencia ejecutada; incluye "PENDIENTE" cuando otro agente lo está midiendo). Ningún CUMPLE se basa en inferencia: si no hay ejecución, no se marca.
Referencias: OWASP Top 10 2025 / ASVS 5.0 y OWASP API Top 10, según la matriz del TPO.

Evidencia "corrida D1" = ejecución del grupo `bug` del 15/11; "verify" = `./mvnw verify` (285 tests, 0 fallos, `reports/i1.md`).

## 1. Matriz SEC del TPO (checkpoint 3)
| Id | Criterio | Estado | Evidencia | Defecto |
|---|---|---|---|---|
| SEC-W01 | Sin token, malformado o vencido -> 401 | CUMPLE | `AuthAndSecurityIntegrationTest.noToken`, `invalidTokens`; `SecurityEvidenceTests.expired`, `malformedHeaders` (verify). El TPO dice "falta test específico": hoy existe | |
| SEC-W02 | Rol insuficiente -> 403 y estado intacto | CUMPLE | `InfractionIntegrationTest.confirmForbidden`; `SecurityEvidenceTests.forbiddenRolesAndAnonymous` (verify) | |
| SEC-W03 | Propietario: ciudadano ajeno -> 403 | CUMPLE para infracciones, retenidos y accidentes; NO CUMPLE para sesiones de estacionamiento | `SecurityEvidenceTests.bolaInfractions`, `bolaImpoundments`, `bolaAccidents` (verify); `bolaParkingSession` FALLA (corrida D1) | DEF-15 |
| SEC-W04 | Validación de entrada -> 400 y sin persistir | CUMPLE parcial | `RequestValidationTests.*`, `SecurityEvidenceTests.injection` (verify). La no persistencia se comprueba en `injection` por conteo; falta el caso de evidencia vacía (BUG-BE-04 devuelve 201) | DEF-16 |
| SEC-W05 | Error interno sin stack trace ni SQL | CUMPLE parcial | `SecurityEvidenceTests.errorsDoNotLeak` (JSON roto, tipo inválido, id inexistente, método no permitido). No encontré un handler de último recurso `Exception` en `GlobalExceptionHandler`, por lo que una excepción no prevista dependería del manejo por defecto de Spring: no hay prueba que fuerce una excepción interna | |
| SEC-W06 | CORS: origen no autorizado sin `Allow-Origin` | CUMPLE | `AuthAndSecurityIntegrationTest.cors`; `SecurityEvidenceTests.corsEvil` (verify) | |
| SEC-I01 | `infractionConfirmed` único, sin tokens ni claves, solo contrato | CUMPLE (con publicador simulado) | `InfractionIntegrationTest.confirmPending`, `confirmInvalid`; `EventInfrastructureTest.envelope`. Contra Core real: integración manual del 15/11 (EV-5, evidencia por adjuntar) | |
| SEC-I02 | `ticketUpdated` de otra área se ignora | CUMPLE | `TicketEventListenerIntegrationTest.otherArea`, `otherUpdateTypes`; `TicketUpdatedHandlerTest.ignoresTicketThatIsNotRoutedToM7` (verify) | |
| SEC-D01 | Sin DNI, CUIT ni `debtorId` en logs | NO CUMPLE | `LoggingEventPublisher` registra `data` completo (`event/LoggingEventPublisher.java:17`); en producción con broker habilitado el bean no está activo (por confirmar la variable). No hay test de logs | DEF-05 |
| SEC-D02 | Sin secretos hardcodeados; todo por variables | NO CUMPLE completamente | Código: `StubIdentityValidator` con contraseñas fijas, activo si no se define `USERS_REST_ENABLED`. `application.properties` usa `${VAR}`. gitleaks: 0 con `.gitleaksignore`, 4 sin él (`reports/b2.md`, `reports/b2b.md`). `docker-compose.yml` en `main` tiene valores por defecto de desarrollo para `JWT_SECRET` y `DB_PASSWORD` (observado en B2; la rama del compose de A1 pasó a `${VAR:?}`) | DEF-01, DEF-12 |
| SEC-05 | Límite de intentos de login | CUMPLE | `auth/LoginRateLimitFilter` (10 fallos por minuto por IP, configurable); `SecurityEvidenceTests.bruteForce` devuelve 429. El límite por IP detrás de un proxy no fue probado (NO VERIFICABLE) | |
| SEC-09 | Sin DNI en el JWT | CUMPLE (rama `fix/s11-jwt-sin-dni`) | `SecurityEvidenceTests.jwtHasNoPii` PASA: el payload solo trae `sub`, `role`, `iat`, `exp` (+`actorType`/`citizenId` con M1); el DNI se resuelve en el servidor y se expone solo en `GET /api/v1/auth/me` | DEF-06 |

## 2. Criterios adicionales pedidos
| Tema | Estado | Evidencia | Defecto |
|---|---|---|---|
| BOLA/IDOR (API1) | NO CUMPLE (3 de 4 recursos bien; sesiones de estacionamiento no) | ver SEC-W03 | DEF-15 |
| BFLA (API5) | CUMPLE | `SecurityEvidenceTests.forbiddenRolesAndAnonymous`: cada endpoint rechaza roles no autorizados y anónimos | |
| JWT inválidos: `alg=none`, otra clave, vencido, payload alterado, rol desconocido, sin `role` | CUMPLE | `SecurityEvidenceTests.algNone`, `otherKey`, `expired`, `tamperedPayload`, `malformedHeaders`; `AuthAndSecurityIntegrationTest.unknownRoleClaim`; `JwtServiceTest.*` | |
| Mass assignment | CUMPLE | `SecurityEvidenceTests.massAssignmentInfraction`, `massAssignmentSession`, `loginRoleEscalation` | |
| Inyecciones SQL/NoSQL/XSS/JNDI en campos de texto | CUMPLE a nivel API (sin 5xx, datos íntegros) | `SecurityEvidenceTests.injection` (parametrizado). ZAP activo (15/11): sin inyecciones; los 500 con cadenas >255 o byte NUL se corrigieron (`@Size(max = 255)` + rechazo de NUL → 400, `InputLimitsValidationTests`; ver [evidencias/zap](evidencias/zap/README.md)) | |
| XXE en la fachada SOAP | CUMPLE | `SecurityEvidenceTests.xxe` (DOCTYPE con entidad externa, no filtra contenido, < 500) | |
| Fuerza bruta (429) | CUMPLE | `SecurityEvidenceTests.bruteForce` | |
| Cabeceras de seguridad, CSP, HSTS | CUMPLE a nivel de test; producción: cabeceras CSP/HSTS presentes en el frontend (`TABLERO.md`) | `SecurityEvidenceTests.securityHeaders`, `csp`, `hsts`; `SecurityConfig` configura CSP y HSTS de 1 año. En producción el backend: NO VERIFICADO | |
| Actuator: endpoints sensibles bloqueados | CUMPLE | `SecurityEvidenceTests.actuatorLocked`; solo `health` está expuesto (`management.endpoints.web.exposure.include=health`) | |
| Swagger/OpenAPI públicos | NO CUMPLE como endurecimiento (es público por diseño) | `SecurityConfig` permite `/swagger-ui/**`, `/v3/api-docs/**`, `/openapi.yaml` | DEF-09 |
| Idempotency-Key, ETag/If-Match (412), Problem Details | CUMPLE | `SecurityEvidenceTests.idempotency`, `etag`, `notModified`, `problemDetails`; `EtagServerTest` | |
| Datos sensibles en logs | NO CUMPLE | ver SEC-D01 | DEF-05 |
| Datos sensibles en el token | NO CUMPLE | ver SEC-09 | DEF-06 |
| Datos hacia el proveedor de IA | CUMPLE con proveedor simulado | `PiiMaskerTest.masksPii`; `InfractionSuggestionServiceTest.outgoingDataIsSanitized`; `InfractionSuggestionIntegrationTest.piiMasked`. Con Groq real: PENDIENTE | |
| Prompt injection | CUMPLE con proveedor simulado | `InfractionSuggestionServiceTest.promptInjectionCannotChangeContract`; `InfractionSuggestionIntegrationTest.promptInjection`. Eval real PENDIENTE | |
| Secretos en el repositorio | CUMPLE con salvedad | gitleaks sobre HEAD y sobre el historial: 0 con `.gitleaksignore` (4 entradas con comentario). Ver DEF-12 | DEF-12 |
| Secretos en la imagen y el artefacto desplegado | NO VERIFICABLE | no se inspeccionó la imagen | |
| ZAP baseline y activo autenticado | CUMPLE (0 High, 0 Medium tras corrección; 1 Low, 3 Info) | ZAP 2.17.0 contra compose local, [evidencias/zap](evidencias/zap/README.md); job CI `zap-baseline` | |
| Dependencias (Trivy, `npm audit`, SCA) | NO VERIFICABLE (PENDIENTE) | otro agente | |
| Autenticación en producción | NO CUMPLE | stub con credenciales fijas posiblemente activo | DEF-01 |
| Aislamiento de capas de red / allowlist de Core | NO VERIFICABLE | fuera del alcance de las pruebas | |
| Rotación de credenciales expuestas | NO CUMPLE por decisión | decisión del 15/11: no rotar; riesgo aceptado | DEF-12 |

## 3. Resumen
CUMPLE: W01, W02, W04 (parcial), W05 (parcial), W06, I01, I02, SEC-05, BFLA, JWT, mass assignment, inyecciones a nivel API, XXE, 429, cabeceras, actuator, Idempotency/ETag.
NO CUMPLE: W03 (estacionamiento), D01, D02, SEC-09, autenticación en producción, Swagger público.
NO VERIFICABLE / PENDIENTE: dependencias, secretos en artefacto, proxy y límite de login, cabeceras del backend en producción.
