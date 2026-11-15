# 01 - Plan y estrategia de pruebas (M7 Tránsito)

Base: ISO/IEC/IEEE 29119-3 (documentación de pruebas). Versión: estado final del 15/11/2026 ~18:45 ART, redactada con asistencia de IA (ver `07-declaracion-ia.md`) y revisada contra el código de `main` del backend (commit `57b2765`, PR #48, desplegado en Railway) y del frontend (`1f0187d`, PR #10).
Regla de honestidad: cada afirmación de resultado indica su evidencia; donde no hay ejecución se escribe **PENDIENTE** o **NO VERIFICABLE**. Los resultados se detallan en `04-informe-ejecucion.md`; aquí se resume el estado de cada nivel.

## 1. Alcance

### 1.1 Dentro de alcance
| Área | Qué se prueba | Fuente de requisitos |
|---|---|---|
| F1 Infracciones | alta (evidencia o validación del inspector), edición, apelación, confirmación, anulación, consulta propia | HU-INF-01..06 |
| F2 Cortes de calle | recepción del evento, aprobar/rechazar, activar, finalizar, vencimiento, 409; recepción de servicios urbanos, podas y riesgo de árboles | HU-COR-01..05 (HU-COR-05 sin implementar: se traza como brecha, DEF-20) |
| F3 Operativos | alta, edición, cierre, derivación desde ticket de M2 | HU-OPE-01..04 |
| F4 Accidentes | alta, involucrados/daños, verificación, ticket de origen | HU-ACC-01..04 |
| F5 Retenidos | ingreso, requisitos, liberación, pagos de Rentas | HU-VEH-01..04 |
| F6 Estacionamiento | zonas, tarifas, sesiones, marca de impago | HU-EST-01..05 |
| F7 Consultas | listados, tablero del frontend | HU-IND-01..03 |
| Transversal | login y roles, JWT (sin DNI), Problem Details, Idempotency-Key, ETag, 429 de login, cabeceras | matriz SEC, ADR |
| Integración | eventos publicados (Transactional Outbox) y consumidos (envelope Core), esquema de Core de `updateTicketStatus`, idempotencia de consumo, adaptador REST de M1, health con broker caído | sección 2.7 del TPO, contratos de Core |
| IA del producto | sugerencia de infracción (`POST /api/v1/infractions/suggestions`), modo manual | `docs/ia/` |
| Frontend | componentes (Vitest), E2E (Playwright, API simulada), accesibilidad (axe/Lighthouse) | repo `transito-frontend` |

Total de historias trazadas: **31** (INF 6, COR 5, OPE 4, ACC 4, VEH 4, EST 5, IND 3). Ver `02-matriz-trazabilidad.md`.

### 1.2 Fuera de alcance (y por qué)
- Comportamiento interno de Core, M1, M2, M3, M5, M6: se prueba solo el contrato visible para M7 (simulado en pruebas automáticas; real en las pruebas con módulos del 15/11, ver 04 sección 7).
- Pruebas de carga e intrusión contra producción (Railway): prohibido por regla de trabajo; se miden contra el compose local.
- Rotación de credenciales de producción: decisión del equipo (no se hace por ahora), se reporta como riesgo aceptado (DEF-12).
- Pruebas de usabilidad con usuarios reales y lector de pantalla: NO VERIFICABLE (no hubo sesión).

## 2. Niveles y tipos de prueba

| Nivel | Herramienta | Dónde está | Estado de ejecución (15/11, `main` `57b2765`) |
|---|---|---|---|
| Unitario | JUnit 5 + Mockito/AssertJ | `src/test/java/.../unit/`, `ai/`, `listener/`, `event/`, `model/` | Ejecutado: parte de los 497 tests de `./mvnw verify`, 0 fallos |
| Integración (API + base) | `@SpringBootTest` + MockMvc, base en memoria; PostgreSQL real con Testcontainers | `integration/`, `PostgresContainerIntegrationTest` | Ejecutado, 0 fallos |
| Integración con servidor HTTP real | `EtagServerTest`, `RestIdentityContextTest` (M1 simulado) | `integration/` | Ejecutado, 0 fallos |
| Contrato | `ApiContractSmokeTests`, `EventInfrastructureTest`, `OutboxContractTest` (bytes idénticos al formato anterior), `CoreSchemaContractTest` (esquemas JSON de Core para `ticketUpdated`/`updateTicketStatus`), `InfractionConfirmedOffsetTest`; Newman/Postman | `src/test`, `tests/postman/` | Ejecutado. Newman en el CI de `main`: 273 requests, 695 aserciones, 0 fallas. Contrato SOAP: hay WSDL (`GET /soap/authentication?wsdl`) pero ninguna prueba valida contra él: NO VERIFICABLE |
| Seguridad automatizada | `SecurityEvidenceTests`, `AuthAndSecurityIntegrationTest`, `JwtSinDniIntegrationTest`, `ValidationAndHeadersDefectsTest`, `InputLimitsValidationTests` | `integration/` | Ejecutado, 0 fallos (BOLA 4/4 recursos y `jwtHasNoPii` ya están en el build normal) |
| E2E | Playwright (7 escenarios, API simulada) | frontend `e2e/tests/transito.e2e.js` | 7/7 en el CI de `main` del frontend; 3 corridas sin inestables (I2) |
| Accesibilidad | axe + Lighthouse (modo snapshot) | frontend `scripts/a11y/` | axe 0 violaciones en 3 anchos (I2) |
| No funcional: carga | k6, 50 usuarios × 5 min, contra compose local | `tests/k6/load.js` | Ejecutado (T1): p95 341 ms. Medido antes de los PR #31/#39/#48: repetición sobre `main` final PENDIENTE |
| No funcional: mutación | PIT, perfil `mutation` del `pom.xml` | `pom.xml`, `mutation-resumen.md` | Ejecutado (T2): 67 % perfil del pom; 92 % alcance ampliado. Medido antes de los PR #31/#39/#48 |
| No funcional: seguridad dinámica | OWASP ZAP baseline (CI) + activo autenticado (local) | `tests/run-zap.sh`, job `zap-baseline` | Ejecutado: activo 0 High / 0 Medium (s8); baseline del CI de `main`: 0 FAIL, 1 WARN informativo |
| No funcional: dependencias | Trivy (backend y frontend), `npm audit` | job `dependencias` | Ejecutado: 0 High/Critical con fix disponible (job bloqueante en verde) |
| Análisis estático | SonarCloud, gitleaks | CI | Quality Gate PASSED (backend y frontend); gitleaks 0 con `.gitleaksignore` |
| Manual / integración con módulos en producción | `prueba-prod.sh`, mensajes con Core y cada módulo | espacio de trabajo, `pruebas-modulos-15-11.md` | Ejecutado el 15/11: M3 corte y accidente, M6 corte (ver 04 sección 7) |

Nota: las reproducciones `@Tag("bug")` (19 métodos en `BugReproductionTest`) se excluyen del build normal (`bugs.excluded=bug` en `pom.xml`) y se ejecutan con `./mvnw test -Dbugs.excluded=none -Dgroups=bug -Djacoco.skip=true`. Corrida del 15/11 sobre `57b2765`: 16 pasan, 1 falla y 2 dan error de preparación; las 3 son pruebas desactualizadas (DEF-17, DEF-18), no defectos del producto.

## 3. Entornos

| Entorno | Descripción | Uso | Limitaciones |
|---|---|---|---|
| Local con compose | `docker compose up`: app + PostgreSQL + RabbitMQ + mocks WireMock de M1 y Core (README) | demo, carga, ZAP, Newman, broker caído y outbox | el adaptador REST de M1 queda apagado por defecto (`USERS_REST_ENABLED=false`) |
| CI (GitHub Actions) | backend: `test` (`./mvnw verify`, cobertura mínima 85 %), `gitleaks`, `dependencias` (Trivy), `api-tests` (Newman), `zap-baseline`, `sonarcloud`; frontend: `test`, `e2e`, `sonarcloud`, `gitleaks` | puerta de calidad | mutación y k6 no están en el CI (se corren a mano) |
| Producción (Railway) | integrada con Core y M1/M2/M3/M5/M6; `main` despliega automáticamente | solo pruebas manuales autorizadas y de bajo impacto | sin carga ni intrusión; pool de base de 3 conexiones |

## 4. Datos de prueba
- Usuarios de prueba del validador local (`StubIdentityValidator`): ciudadano, inspector y administrador. Desde DEF-01 las contraseñas salen de variables `STUB_PASSWORD_<USUARIO>` (sin valor por defecto); **no se reproducen en este documento**.
- Fixtures: `src/test/java/.../support/{Payloads,TestUsers}.java`; esquemas de Core en `src/test/resources/contracts/core/`; eventos de entrada inyectados con `scripts/inyectar-street-closure.sh`.
- Mocks del compose: `docker/wiremock/mappings` (login de M1 y Core: token de módulo y publicaciones).
- Datos de IA: `docs/ia/eval-set.json` (la corrida real contra el proveedor está PENDIENTE: requiere clave cargada por una persona).
- Producción: solo datos marcados como prueba (patente `TEST123`, calle "Av. de Prueba", DNI ficticios).

## 5. Criterios de entrada y de salida

**Entrada** (para ejecutar una ronda): rama integrada sin conflictos; `./mvnw -B verify` compila; compose levanta con `.env` completo; Docker disponible.

**Salida** (para dar la entrega por probada) y estado al 15/11 ~18:45:
| # | Criterio | Umbral | Estado |
|---|---|---|---|
| 1 | Backend `./mvnw verify` | verde, ≥ 85 % de líneas | Cumple: 497 tests, 0 fallos, 93,74 % |
| 2 | Frontend | lint, `test:coverage` ≥ 85 %, build, E2E | Cumple: 60 tests, 93,48 %; E2E 7/7 en CI |
| 3 | CI verde sobre el tag `v2.0-entrega4` | verde | **PENDIENTE**: el tag se crea al final (lo hace una persona). CI de `main` `57b2765`: todos los jobs en verde |
| 4 | Defectos abiertos | 0 Bloqueantes, 0 Críticos, ≤ 3 Mayores con workaround | 0 / 0 / 3 (DEF-19, DEF-20, DEF-33). Cumple en número; DEF-20 no tiene workaround técnico (ver 05, "Riesgo de salida") |
| 5 | Mutación | ≥ 60 % | Cumple: 67 % (perfil del pom) |
| 6 | Carga | p95 < 500 ms con 50 usuarios | Cumple: 341 ms (medido antes del merge final) |
| 7 | ZAP | sin High | Cumple: 0 High, 0 Medium |
| 8 | Dependencias | sin CVE High/Critical sin mitigación | Cumple: Trivy 0 |
| 9 | SonarCloud | Quality Gate aprobado | Cumple: PASSED |
| 10 | Estabilidad | suite repetida 3 veces sin intermitentes | Cumple con la evidencia disponible: `verify` ×3 (T2, 349 tests) y E2E ×3; no se repitió ×3 sobre `57b2765` |
| 11 | Secretos | 0 hallazgos de gitleaks | Cumple con `.gitleaksignore` (DEF-12 aceptado) |

## 6. Riesgos priorizados (probabilidad x impacto)

Escala: Probabilidad 1 (rara) a 3 (probable); Impacto 1 (menor) a 3 (alto). Puntaje = P x I. Prioridad: 6-9 alta, 3-4 media, 1-2 baja. Los puntajes son los del análisis inicial; la columna "Estado" indica cómo quedó cada riesgo al 15/11.

| Id | Riesgo | P | I | Pts | Prio | Pruebas que lo cubren | Estado al 15/11 |
|---|---|---|---|---|---|---|---|
| R1 | Credenciales de desarrollo fijas activas en producción (login local) | 3 | 3 | 9 | Alta | `TC-BE-AUTH-04b`, `RestIdentityContextTest`, `git grep` de contraseñas | Mitigado: contraseñas fuera del código (DEF-01 cerrado), login de las 3 cuentas verificado en producción. Residual: el adaptador de M1 sigue sin activar |
| R2 | Pérdida de eventos si el broker está caído | 2 | 3 | 6 | Alta | `OutboxIntegrationTest.brokerCaidoYVuelve`, `rollbackNoDejaEvento`, `sinDuplicadosEnParalelo`; stop/start de RabbitMQ en compose (s12, i5); `HealthWithBrokerDownTest` | Cubierto: Transactional Outbox en producción (DEF-14 cerrado). Entrega "al menos una vez": los consumidores deduplican por `eventId` |
| R3 | BOLA/IDOR: ciudadano accede a recursos ajenos | 2 | 3 | 6 | Alta | `SecurityEvidenceTests.bolaInfractions/bolaImpoundments/bolaAccidents/bolaParkingSession`, `ParkingIntegrationTest.Ownership` | Cubierto en los 4 recursos (DEF-15 cerrado) |
| R4 | Transiciones inválidas del corte de calle | 2 | 3 | 6 | Alta | `TC-BE-SC-07`, `StreetClosureStateTest` (30 casos), `StateTransitionTests` | Cubierto (409) |
| R5 | Fuga de datos personales (DNI en JWT, payload en logs) | 3 | 2 | 6 | Alta | `SecurityEvidenceTests.jwtHasNoPii`, `JwtSinDniIntegrationTest`, `EventLoggingSinPiiTest`, `IncomingEventLoggingSinPiiTest` | Cubierto (DEF-05 y DEF-06 cerrados) |
| R6 | Evento duplicado o inválido procesado dos veces | 2 | 2 | 4 | Media | `IncomingEventDispatcherTest`, `StreetClosureRequestedHandlerTest`, `TC-BE-SC-02` | Cubierto |
| R7 | Indisponibilidad o respuesta inválida de M1 (login) | 2 | 2 | 4 | Media | `RestIdentityContextTest`, `RestIdentityValidatorTest` | Cubierto con M1 simulado; contra M1 real NO VERIFICABLE (adaptador sin activar) |
| R8 | Despliegue de `main` rompe producción (deploy automático) | 2 | 3 | 6 | Alta | CI completo; smoke manual post-deploy (health, Core, catálogo de eventos, frontend) | Parcial: los 4 deploys del 15/11 se verificaron a mano; no hay smoke automático post-deploy |
| R9 | Sugerencia de IA incorrecta o manipulada | 2 | 2 | 4 | Media | `InfractionSuggestionServiceTest.promptInjectionCannotChangeContract`, `InfractionSuggestionIntegrationTest.promptInjection/piiMasked`, `GroqAiGatewayTest` | Cubierto con proveedor simulado; eval real PENDIENTE |
| R10 | Degradación bajo carga (pool de 3 conexiones) | 2 | 2 | 4 | Media | k6 | Medido: p95 341 ms; listado sin paginar a 472 ms (cerca del límite); repetición sobre `main` final PENDIENTE |
| R11 | Fuerza bruta sobre el login | 2 | 2 | 4 | Media | `SecurityEvidenceTests.bruteForce`, `LoginRateLimitIntegrationTest` | Cubierto (DEF-22 cerrado, sonda en producción) |
| R12 | Requisito sin implementar (importes, costos de retención, HU-COR-05, filtros e indicadores del backend) | 3 | 2 | 6 | Alta | Matriz 02, sección "sin cobertura" | Declarado como DEF-19 y DEF-20 (abiertos) |
| R13 | Secretos en el historial de Git | 1 | 3 | 3 | Media | gitleaks sobre historial | 4 hallazgos en `.gitleaksignore`; riesgo aceptado (DEF-12) |
| R14 | Cambio correcto del envelope rompe a un consumidor de otro módulo | 3 | 2 | 6 | Alta | `TC-BE-SC-09`; re-prueba con M3 y Core | Materializado: el consumidor v1.0 de M3 rechaza `streetClosureEnded` con `causationId` (DEF-33, abierto con workaround) |
| R15 | Logs de producción ruidosos que ocultan errores | 3 | 1 | 3 | Media | revisión de `application.properties` y logs de Railway | Materializado: `show-sql` imprime el SELECT del relay cada 2 s (DEF-32, abierto) |

Matriz (celdas con ids de riesgo):

| P \ I | 1 | 2 | 3 |
|---|---|---|---|
| 3 | R15 | R5, R12, R14 | R1 |
| 2 | | R6, R7, R9, R10, R11 | R2, R3, R4, R8 |
| 1 | | | R13 |

## 7. Gestión de defectos y reporte
Registro en `05-registro-defectos.md`; severidades Bloqueante / Crítica / Mayor / Menor. Los resultados se informan en `04-informe-ejecucion.md`.

## 8. Responsables
Alejo García (QA/DevOps), Romina Fabian (PO), Lautaro Abrego (backend), Fernando Abdala (frontend). Este documento lo escribió un agente de IA y debe ser revisado por una persona antes de la entrega.
