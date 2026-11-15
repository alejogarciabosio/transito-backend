# Correcciones necesarias en el Google Doc del TPO

Cada punto se verificó contra el código (`git grep` sobre `main`, commit `65e330b`) y contra el texto exportado del documento (`tpo-limpio.md`). Los números de línea son del archivo exportado, no del Google Doc: sirven para ubicar el texto buscándolo. Esto es un borrador de un agente de IA; el equipo decide y edita (el Doc lo edita una persona, no el agente).

## 1. Portada: profesor e integrantes inconsistentes
| Dónde (línea exportada) | Dice | Problema |
|---|---|---|
| ~12-21 (primera portada) | Profesor: Ing. Monasterio Maria Julia; 3 integrantes (sin García, Alejo) | las demás portadas dicen Ruiz, Matías Gabriel |
| ~2464 | "Ig. Ruiz, Matias Gabriel" (typo "Ig."); 3 integrantes | falta García, Alejo |
| ~2579 y ~2632 | campo Profesor vacío; 3 integrantes | vacío y sin García |
| ~2991, ~3299, ~3687, ~4225, ~4638 | Ruiz, Matías Gabriel; 4 integrantes | es la versión coherente |
**Corrección**: unificar en todas las portadas el profesor correcto (el equipo debe confirmar si es Ruiz o Monasterio: no puedo saberlo desde el repo) y los 4 integrantes (Abdala, Abrego, Fabian, García, con sus LU).

## 2. ADR-002 habla de SOAP, pero la integración real es REST con una fachada SOAP
Texto actual (líneas ~4307-4335): componentes `IdentityValidator`, `SoapIdentityValidator`, `StubIdentityValidator`; selección por `users.soap.enabled`; "validar usuarios mediante SOAP con M1".
Realidad en el código:
- No existe `SoapIdentityValidator` (`git grep SoapIdentityValidator -- src` sin resultados).
- La implementación es `auth/RestIdentityValidator`, activa con `users.rest.enabled=true` (variables `USERS_REST_ENABLED`, `USERS_REST_URL`, `USERS_REST_TIMEOUT_MS`, por defecto 5000 ms); el stub queda con `matchIfMissing`.
- M7 expone una fachada SOAP: `web/SoapAuthenticationController` en `/soap/authentication` (con `?wsdl`), activa con `users.soap-adapter.enabled`.
**Corrección**: reescribir el ADR: contexto (M1 expone REST), decisión (Adapter: `IdentityValidator` con `RestIdentityValidator` y `StubIdentityValidator`; Strategy por propiedad), fachada SOAP propia para los consumidores que la requieran, consecuencias, alternativa descartada. Corregir también: sección 6.1 de la Actividad API REST (líneas ~3585-3607: "La clase SoapIdentityValidator", `USERS_SOAP_ENABLED`, `USERS_SOAP_URL`), la sección 5 del Checkpoint 3 ("Incorporado: SoapIdentityValidator", línea ~4700), la tabla 3.3 ("Resiliencia externa: Timeout SOAP ... SoapIdentityValidator", ~4690), la sección 7 del Checkpoint 3 ("Cliente SOAP validateUser", ~4722), el requisito BR-03 y HU-03 (~4476, ~4556: "Ante errores SOAP o timeout") y los diagramas que digan SOAP.

## 3. Sección 7 dice "no implementado" para cosas que sí existen
Texto actual: en la Actividad API REST sección 7 (líneas ~3657-3662) "Evoluciones aún no implementadas: Transactional Outbox, Idempotency-Key para endpoints REST, Rate limiting, Paginación y filtros, Hipermedia"; y en la sección 7 del Checkpoint 3 (línea ~4729) "quedan como evoluciones... Idempotency Key para solicitudes REST, rate limiting, paginación y filtros".
| Afirmación del Doc | Realidad verificada | Qué hacer |
|---|---|---|
| Idempotency-Key no implementado | existe `auth/IdempotencyFilter` (POST con cabecera `Idempotency-Key`); test `SecurityEvidenceTests.idempotency` | mover a "implementado" y citar el test |
| (no se menciona ETag) | existe `auth/ConditionalRequestFilter` (ETag, `If-None-Match` 304, `If-Match` 412); tests `etag`, `notModified`, `EtagServerTest` | agregar a "implementado" |
| Rate limiting no implementado | existe `auth/LoginRateLimitFilter`: 429 con Problem Details y `Retry-After`, por IP, solo en login (10 fallos por minuto por defecto); test `SecurityEvidenceTests.bruteForce` | decir "limitado al login"; no hay rate limiting general |
| Paginación y filtros no implementados | correcto: `git grep` no encuentra paginación ni filtros en los controladores | mantener como pendiente |
| Transactional Outbox no implementado | correcto: sin resultados en `src` | mantener como pendiente |
| Hipermedia no implementado | no verificado en profundidad | mantener o verificar |
También en "Hallazgos abiertos" del Checkpoint 3 (línea ~4943): "sin límite de intentos en el login (SEC-05)" ya no es cierto (hoy 429); "DNI visible en el token (SEC-09)" sigue siendo cierto (`jwtHasNoPii` falla); "marcar estacionamiento impago responde 500" hoy no se reproduce (`bug01` pasa); "7 tests en rojo en main" está desactualizado (hoy el build tiene 0 fallos); "Cliente SOAP de M1 sin prueba automatizada" ya no es cierto (ver punto 4).

## 4. Matriz de trazabilidad: BR-03 dice "sin prueba" y hoy hay pruebas
Texto actual (línea ~4616): `BR-03 | HU-03 | Si el Módulo 1 no responde en 5 segundos, la API responde 503 sin token | Adapter / SoapIdentityValidator | Sin prueba automatizada | Pendiente`, y el párrafo siguiente ("Falta una prueba de SoapIdentityValidator con un servidor simulado").
Realidad: `unit/RestIdentityValidatorTest` (`timeout`, `connectionRefused`, `errorStatuses`, `invalidResponses`, `employeeRoles`) y `integration/RestIdentityContextTest` (`loginTimeout` -> 503, `loginConnectionRefused` -> 503, `loginUpstreamError` -> 502, `loginAuthFalse`, `loginOk`, `loginInvalid`, `beanExists`).
**Corrección**: componente `RestIdentityValidator`, prueba `RestIdentityValidatorTest.timeout` y `RestIdentityContextTest.loginTimeout`, estado "Cubierto con M1 simulado" (contra M1 real: pendiente porque el adaptador no está activo en Railway). Notar además: BR-03 dice "si auth=false, devolver 401" pero el código responde 502 (DEF-10); alinear texto o código. Agregar a la matriz el resto de las HU (hoy solo HU-01 a HU-03 de la Parte B; las F1-F7 completas están en `02-matriz-trazabilidad.md`).

## 5. Estado de State y de outbox
- **State**: la matriz (filas BR-01, ~4606-4608) cita "State" como patrón de `StreetClosureService`, y la Parte A describe la máquina de estados. En el código el estado es el enum `StreetClosureStatus` y `StreetClosureService.requireStatus` valida las transiciones; **no hay objetos State**. Corrección hasta que Lautaro lo implemente: escribir "máquina de estados con enum y validación central (el patrón State queda pendiente)"; no listarlo como patrón aplicado. Si se implementa, agregar su ADR y el test parametrizado de transiciones inválidas. La tabla de patrones del Checkpoint 3 (sección 4) no lista State: coherente con el código.
- **Outbox**: la sección 7 acierta al decir que no está implementado (sin resultados en `src`). Agregar el riesgo concreto: con el broker caído, los eventos no se reintentan (DEF-14), y que hoy no existe registro de eventos publicados (DEF-04, regla 9.3).
- `streetClosureActivated`: la Parte A dice que "se utiliza internamente"; en el código el payload existe, pero nada lo emite (DEF-21). Corregir el texto o implementarlo.

## 6. Otras correcciones detectadas (no pedidas, verificadas)
- Matriz de seguridad SEC (líneas ~3693-3702): los veredictos "falta test específico" de SEC-W01, W02, W03 (salvo estacionamiento), W06 e I02 ya tienen test (ver `06-informe-seguridad.md`); SEC-D01 y SEC-D02 siguen "No cumple".
- Sección Testing (líneas ~4900-4960): faltan los resultados de la 4.ª entrega (E2E 7/7, axe 0, IA, compose completo, PR #25 y #8) y los defectos nuevos (DEF-15, DEF-16).
- El Doc no menciona la feature de IA ni el `AiGateway` (`docs/ia/`): falta su ADR y su ficha.
