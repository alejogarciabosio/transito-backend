# 06 - Informe de seguridad

Actualizado: 15/11/2026 ~18:45 ART (estado final), sobre `main` `57b2765` (PR #48, desplegado) y frontend `1f0187d` (PR #10). Estados: **CUMPLE** (hay test o evidencia ejecutada), **NO CUMPLE** (hay evidencia de incumplimiento), **NO VERIFICABLE** (sin evidencia ejecutada; incluye "PENDIENTE" cuando otro agente lo está midiendo). Ningún CUMPLE se basa en inferencia: si no hay ejecución, no se marca.
Referencias: OWASP Top 10 2025 / ASVS 5.0 y OWASP API Top 10, según la matriz del TPO.

Evidencia "verify" = `./mvnw -q -B verify` sobre `integration/entrega4-f` (518 tests, 0 fallos, 94,34 % de líneas, corrida I7; antes 497 sobre `57b2765`, D3 y job `test` del CI de `main`); "Newman" = job `api-tests` del CI de `main` (273 requests, 695 aserciones, 0 fallas, los 6 `[KI-n]` corregidos); "ZAP" = activo autenticado local (s8) y baseline del CI de `main`.

## 1. Matriz SEC del TPO (checkpoint 3)
| Id | Criterio | Estado | Evidencia | Defecto |
|---|---|---|---|---|
| SEC-W01 | Sin token, malformado o vencido -> 401 | CUMPLE | `AuthAndSecurityIntegrationTest.noToken`, `invalidTokens`; `SecurityEvidenceTests.expired`, `malformedHeaders` (verify). El TPO dice "falta test específico": hoy existe | |
| SEC-W02 | Rol insuficiente -> 403 y estado intacto | CUMPLE | `InfractionIntegrationTest.confirmForbidden`; `SecurityEvidenceTests.forbiddenRolesAndAnonymous` (403 exacto); `ValidationAndHeadersDefectsTest.authorizationBeforeValidation`: con body inválido el rol sin permiso recibe 403, no 400 (verify, Newman KI-3) | DEF-27 (cerrado) |
| SEC-W03 | Propietario: ciudadano ajeno -> 403/404 | CUMPLE (los 4 recursos) | `SecurityEvidenceTests.bolaInfractions`, `bolaImpoundments`, `bolaAccidents`; sesiones de estacionamiento: `ParkingIntegrationTest.Ownership` (6 casos, 404 a ciudadano ajeno sin cambiar estado) y SEC-BOLA-04 ya en el build (verify). Corregido en PR #32 | DEF-15 (cerrado) |
| SEC-W04 | Validación de entrada -> 400 y sin persistir | CUMPLE | `RequestValidationTests.*`, `SecurityEvidenceTests.injection`, `InputLimitsValidationTests` (>255 y NUL → 400); `ValidationAndHeadersDefectsTest.unpaidFlagRejectsInvalidAmount` (importe 0 o ausente → 400); `InfractionValidationRuleTest` (sin evidencia ni inspector → 400) (verify, Newman KI-5/5b) | DEF-16, DEF-26, DEF-29 (cerrados) |
| SEC-W05 | Error interno sin stack trace ni SQL | CUMPLE parcial | `SecurityEvidenceTests.errorsDoNotLeak` (JSON roto, tipo inválido, id inexistente, método no permitido); parámetro faltante y tipo inválido → Problem Details (`ValidationAndHeadersDefectsTest`, DEF-28 cerrado); Newman verifica "sin trazas" en cada respuesta de error. No encontré un handler de último recurso `Exception` en `GlobalExceptionHandler`, por lo que una excepción no prevista dependería del manejo por defecto de Spring: no hay prueba que fuerce una excepción interna | |
| SEC-W06 | CORS: origen no autorizado sin `Allow-Origin` | CUMPLE | `AuthAndSecurityIntegrationTest.cors`; `SecurityEvidenceTests.corsEvil` (verify) | |
| SEC-I01 | `infractionConfirmed` único, sin tokens ni claves, solo contrato | CUMPLE | `InfractionIntegrationTest.confirmPending`, `confirmInvalid`; `EventInfrastructureTest.envelope`; `OutboxContractTest.contratoIdentico` (mismos bytes por el outbox). Contra Core real: eventId `114ab437…` DELIVERED a `q.rentas` en 1 intento (04 sección 7) | |
| SEC-I02 | `ticketUpdated` de otra área se ignora | CUMPLE | `TicketEventListenerIntegrationTest.otherArea`, `otherUpdateTypes`; `TicketUpdatedHandlerTest.ignoresTicketThatIsNotRoutedToM7` (verify) | |
| SEC-D01 | Sin DNI, CUIT ni `debtorId` en logs | CUMPLE | Publicadores y listener registran solo `type eventId correlationId` (`LoggingEventPublisher.java:19`, `[event-rejected]` sin causa en el listener; errores de Core sin cuerpo). Tests `EventLoggingSinPiiTest` e `IncomingEventLoggingSinPiiTest` con DNI y patente de prueba (verify). PR #34 | DEF-05 (cerrado) |
| SEC-D02 | Sin secretos hardcodeados; todo por variables | CUMPLE con salvedad | Las contraseñas de los usuarios de prueba ya no están en el código: `StubIdentityValidator` las lee de `STUB_PASSWORD_<USUARIO>` sin default (en producción, variables de Railway); sin variable → 401 + WARN con el nombre, nunca el valor (`TC-BE-AUTH-04b`). PR #36. `application.properties` usa `${VAR}`. gitleaks: 0 con `.gitleaksignore` (job de CI). Salvedad: DEF-12 (riesgo aceptado) | DEF-01 (cerrado), DEF-12 |
| SEC-05 | Límite de intentos de login | CUMPLE | `auth/LoginRateLimitFilter`: 10 fallos/60 s por IP (tomada de `X-Forwarded-For` contando `trusted-proxies` desde la derecha) y además por usuario. `LoginRateLimitIntegrationTest`, `LoginRateLimitFilterTest`, `SecurityEvidenceTests.bruteForce`. Producción (15/11): 10 fallos con XFF distinto → 401 ×10 y luego 429. Riesgo residual: bloqueo temporal de 60 s de una cuenta ajena; contador en memoria | DEF-22 (KI-2, cerrado) |
| SEC-09 | Sin DNI en el JWT | CUMPLE | `SecurityEvidenceTests.jwtHasNoPii` (en el build normal) y `JwtSinDniIntegrationTest` (5): el payload solo trae `sub`, `role`, `iat`, `exp` (+`actorType`/`citizenId` con M1); el DNI se expone solo al propio usuario en `GET /api/v1/auth/me`. Producción: `/auth/me` 401 sin token; el frontend usa `/auth/me` | DEF-06 (cerrado) |

## 2. Criterios adicionales pedidos
| Tema | Estado | Evidencia | Defecto |
|---|---|---|---|
| BOLA/IDOR (API1) | CUMPLE (4 de 4 recursos) | ver SEC-W03; Newman: caso KI-1 | DEF-15 (cerrado) |
| BFLA (API5) | CUMPLE | `SecurityEvidenceTests.forbiddenRolesAndAnonymous`: cada endpoint rechaza roles no autorizados (403 exacto) y anónimos (401); también con body inválido | DEF-27 (cerrado) |
| JWT inválidos: `alg=none`, otra clave, vencido, payload alterado, rol desconocido, sin `role` | CUMPLE | `SecurityEvidenceTests.algNone`, `otherKey`, `expired`, `tamperedPayload`, `malformedHeaders`; `AuthAndSecurityIntegrationTest.unknownRoleClaim`; `JwtServiceTest.*` | |
| Mass assignment | CUMPLE | `SecurityEvidenceTests.massAssignmentInfraction`, `massAssignmentSession`, `loginRoleEscalation` | |
| Inyecciones SQL/NoSQL/XSS/JNDI en campos de texto | CUMPLE a nivel API (sin 5xx, datos íntegros) | `SecurityEvidenceTests.injection` (parametrizado). ZAP activo (15/11): sin inyecciones; los 500 con cadenas >255 o byte NUL se corrigieron (`@Size(max = 255)` + rechazo de NUL → 400, `InputLimitsValidationTests`; ver [evidencias/zap](evidencias/zap/README.md)) | |
| XXE en la fachada SOAP | CUMPLE | `SecurityEvidenceTests.xxe` (DOCTYPE con entidad externa, no filtra contenido, < 500) | |
| Fuerza bruta (429) | CUMPLE | ver SEC-05 | DEF-22 (cerrado) |
| Cabeceras de seguridad, CSP, HSTS, CORP | CUMPLE a nivel de test; producción: cabeceras CSP/HSTS presentes en el frontend (`TABLERO.md`) | `SecurityEvidenceTests.securityHeaders`, `csp`, `hsts`; `ValidationAndHeadersDefectsTest.corpHeaderPresent` (`Cross-Origin-Resource-Policy: same-site`); `SecurityConfig` configura CSP y HSTS de 1 año. En producción el backend: NO VERIFICADO | DEF-30 (cerrado) |
| Actuator: endpoints sensibles bloqueados | CUMPLE | `SecurityEvidenceTests.actuatorLocked`; solo `health` está expuesto (`management.endpoints.web.exposure.include=health`) | |
| Swagger/OpenAPI públicos | RIESGO ACEPTADO | Documentación de API pública intencionalmente para la integración entre módulos. `SecurityConfig` permite `/swagger-ui/**`, `/v3/api-docs/**`, `/openapi.yaml`; `application-prod.properties` lo apagaría con el perfil `prod`, que producción no usa (a propósito) | DEF-09 (aceptado) |
| Idempotency-Key, ETag/If-Match (412), Problem Details | CUMPLE | `SecurityEvidenceTests.idempotency`, `etag`, `notModified`, `problemDetails`; `EtagServerTest` | |
| Datos sensibles en logs | CUMPLE | ver SEC-D01 | DEF-05 (cerrado) |
| Datos sensibles en el token | CUMPLE | ver SEC-09 | DEF-06 (cerrado) |
| Datos hacia el proveedor de IA | CUMPLE con proveedor simulado | `PiiMaskerTest.masksPii`; `InfractionSuggestionServiceTest.outgoingDataIsSanitized`; `InfractionSuggestionIntegrationTest.piiMasked`. Con Groq real: PENDIENTE | |
| Prompt injection | CUMPLE con proveedor simulado | `InfractionSuggestionServiceTest.promptInjectionCannotChangeContract`; `InfractionSuggestionIntegrationTest.promptInjection`. Eval real PENDIENTE | |
| Secretos en el repositorio | CUMPLE con salvedad | gitleaks sobre HEAD y sobre el historial: 0 con `.gitleaksignore` (4 entradas con comentario). Ver DEF-12 | DEF-12 |
| Secretos en la imagen y el artefacto desplegado | NO VERIFICABLE | no se inspeccionó la imagen | |
| ZAP baseline y activo autenticado | CUMPLE (0 High, 0 Medium) | ZAP 2.17.0 contra compose local, [evidencias/zap](evidencias/zap/README.md). El Medium (500 con textos >255/NUL) se corrigió (PR #37) y el Low de `Cross-Origin-Resource-Policy` también (PR #45). Baseline del CI de `main`: 0 FAIL, 1 WARN informativo (Non-Storable Content), 67 PASS | DEF-26, DEF-30 (cerrados) |
| Dependencias (Trivy, `npm audit`, SCA) | CUMPLE | Backend: Trivy sobre el jar, 25 High/Critical (8 Critical) → 0 tras Boot 4.1.2 y amqp-client 5.34.0 (PR #30); job CI `dependencias` bloqueante. Frontend: `npm audit --omit=dev` 0 y Trivy 0 | DEF-23 (cerrado) |
| Autenticación en producción | CUMPLE con salvedad | El login de producción usa el validador de usuarios de prueba con contraseñas configuradas por variable de entorno (sin valores en el código), con límite de intentos por IP y por usuario. Login real de las 3 cuentas verificado por el equipo tras el deploy de #39. Salvedad: el adaptador de M1 está corregido pero sin activar (faltan las cuentas de M1 para probarlo) | DEF-01 (cerrado) |
| Logs de producción | CUMPLE sin PII, con ruido | los logs no traen DNI ni payload (SEC-D01); pero `spring.jpa.show-sql=true` imprime el SELECT del relay del outbox cada 2 s (con `?`, sin valores), lo que dificulta ver errores | DEF-32 (abierto, Menor) |
| Integridad del envelope hacia otros módulos | CUMPLE en M7, con incompatibilidad externa | `causationId` y `correlationId` correctos (`TC-BE-SC-09`); el consumidor v1.0 de M3 rechaza `causationId` no nulo: mitigado no aprobando cortes de M3 hasta su deploy | DEF-33 (abierto, Mayor) |
| Aislamiento de capas de red / allowlist de Core | NO VERIFICABLE | fuera del alcance de las pruebas | |
| Rotación de credenciales | No se rota por decisión | decisión del humano del 15/11; riesgo aceptado | DEF-12 (aceptado) |

## 3. Resumen
CUMPLE: W01, W02, W03 (BOLA en los 4 recursos), W04, W05 (parcial: sin handler de último recurso), W06, I01, I02, D01, D02 (con salvedad DEF-12), SEC-05, SEC-09 (JWT sin DNI), BFLA, JWT inválidos, mass assignment, inyecciones a nivel API, XXE, 429, cabeceras (incluida CORP), actuator, Idempotency/ETag, ZAP sin High/Medium, dependencias sin High/Critical, autenticación en producción (con salvedad del adaptador de M1).
NO CUMPLE: ningún criterio de la matriz SEC.
ABIERTOS relacionados (ver 05): DEF-32 logs ruidosos por `show-sql` (Menor); DEF-33 incompatibilidad del consumidor de M3 con `causationId` (Mayor, mitigado).
RIESGO ACEPTADO: Swagger público (DEF-09); historial con contraseña de prueba de M1 sin rotar (DEF-12).
NO VERIFICABLE / PENDIENTE: secretos en la imagen, cabeceras del backend en producción, aislamiento de red, eval de IA con proveedor real, adaptador de M1 contra M1 real.
