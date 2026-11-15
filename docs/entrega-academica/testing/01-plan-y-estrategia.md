# 01 - Plan y estrategia de pruebas (M7 Tránsito)

Base: ISO/IEC/IEEE 29119-3 (documentación de pruebas). Versión: borrador del 15/11/2026, redactado con asistencia de IA (ver `07-declaracion-ia.md`) y revisado contra el código de `main` (commit `65e330b`).
Regla de honestidad: cada afirmación de resultado indica su evidencia; donde no hay ejecución se escribe **PENDIENTE** o **NO VERIFICABLE**.

## 1. Alcance

### 1.1 Dentro de alcance
| Área | Qué se prueba | Fuente de requisitos |
|---|---|---|
| F1 Infracciones | alta, edición, apelación, confirmación, anulación, consulta propia | HU-INF-01..06 |
| F2 Cortes de calle | recepción del evento, aprobar/rechazar, activar, finalizar, vencimiento, 409 | HU-COR-01..04 |
| F3 Operativos | alta, edición, cierre, derivación desde ticket | HU-OPE-01..04 |
| F4 Accidentes | alta, involucrados/daños, verificación, ticket de origen | HU-ACC-01..04 |
| F5 Retenidos | ingreso, requisitos, liberación, pagos de Rentas | HU-VEH-01..04 |
| F6 Estacionamiento | zonas, tarifas, sesiones, marca de impago | HU-EST-01..05 |
| F7 Consultas | listados, tablero del frontend | HU-IND-01..03 |
| Transversal | login y roles, JWT, Problem Details, Idempotency-Key, ETag, 429 de login | matriz SEC, ADR |
| Integración | eventos publicados y consumidos (envelope Core), idempotencia de consumo, adaptador REST de M1, health con broker caído | sección 2.7 del TPO |
| IA del producto | sugerencia de infracción (`POST /api/v1/infractions/suggestions`), modo manual | `docs/ia/` |
| Frontend | pruebas de componentes (Vitest), E2E (Playwright, API simulada), accesibilidad (axe/Lighthouse) | repo `transito-frontend` |

### 1.2 Fuera de alcance (y por qué)
- Comportamiento interno de Core, M1, M3, M5, M6: se prueba solo el contrato visible para M7 (simulado en pruebas automáticas; real solo en las pruebas manuales del 15/11).
- Pruebas de carga contra producción (Railway): prohibido por regla de trabajo; la carga se mide contra el compose local.
- Rotación de credenciales de producción: decisión del equipo (no se hace por ahora), se reporta como riesgo.
- Pruebas de usabilidad con usuarios reales y lector de pantalla: NO VERIFICABLE (no hubo sesión).
- HU-COR-05 (consumo de `urbanServiceScheduled`, `treePruningScheduled`, `treeRiskDetected`): el backend no tiene handlers (ver `git grep` en `listener/handler/`); queda como brecha de implementación, no como prueba.

## 2. Niveles y tipos de prueba

| Nivel | Herramienta | Dónde está | Estado de ejecución |
|---|---|---|---|
| Unitario | JUnit 5 + Mockito/AssertJ | `src/test/java/.../unit/`, `ai/`, `listener/` | Ejecutado (`./mvnw verify`, 15/11, 0 fallos según reporte I1) |
| Integración (API + base) | `@SpringBootTest` + MockMvc, base en memoria; 1 prueba con PostgreSQL real (Testcontainers) | `integration/`, `PostgresContainerIntegrationTest` | Ejecutado |
| Integración con servidor HTTP real | `EtagServerTest`, `RestIdentityContextTest` (M1 simulado) | `integration/` | Ejecutado |
| Contrato | `ApiContractSmokeTests`, `EventInfrastructureTest`, `TicketUpdatedHandlerTest`, `InfractionPaymentEventServiceTest`; OpenAPI en `openapi.yaml` | `src/test` | Ejecutado. Contrato SOAP: la fachada sirve un WSDL en `GET /soap/authentication?wsdl` (`SoapAuthenticationController`), pero ninguna prueba valida las respuestas contra ese WSDL: NO VERIFICABLE. Newman/Postman PENDIENTE (otro agente) |
| Seguridad automatizada | `SecurityEvidenceTests`, `AuthAndSecurityIntegrationTest` | `integration/` | Ejecutado (2 pruebas etiquetadas `bug` fallan a propósito, ver 04 y 06) |
| E2E | Playwright (7 escenarios, API simulada) | frontend `e2e/tests/transito.e2e.js` | 7/7 en 3 corridas (reporte I2) |
| Accesibilidad | axe + Lighthouse (modo snapshot) | frontend `scripts/a11y/` | axe 0 violaciones en 3 anchos (I2) |
| No funcional: carga | k6, 50 usuarios, p95 < 500 ms, contra compose local | PENDIENTE | PENDIENTE (lo mide otro agente) |
| No funcional: mutación | pitest, perfil `mutation` del `pom.xml` | `pom.xml` | PENDIENTE |
| No funcional: seguridad dinámica | OWASP ZAP baseline + activo | PENDIENTE | PENDIENTE |
| No funcional: dependencias | Trivy / `npm audit` | PENDIENTE | PENDIENTE |
| Análisis estático | SonarCloud, gitleaks | CI (`secretos`); Sonar no configurado | gitleaks 0 hallazgos con `.gitleaksignore` (B2b); Sonar PENDIENTE |
| Manual / exploratorio en producción | `prueba-prod.sh` (accidente, infracción, aprobar, activar, finalizar) | raíz del espacio de trabajo | Ejecutado el 15/11 con M3 (ver 04) |

Nota: las pruebas `@Tag("bug")` (22 métodos) se excluyen del build normal (`bugs.excluded=bug` en `pom.xml`) y se ejecutan con `./mvnw test -Dbugs.excluded=none -Dgroups=bug -Djacoco.skip=true`.

## 3. Entornos

| Entorno | Descripción | Uso | Limitaciones |
|---|---|---|---|
| Local con compose | `docker compose up`: app + PostgreSQL + RabbitMQ + mocks WireMock de M1 y Core (README) | demo, carga, ZAP, broker caído | el adaptador REST de M1 queda apagado por defecto (`USERS_REST_ENABLED=false`) |
| CI (GitHub Actions) | `./mvnw verify` (cobertura mínima 85 % de líneas) y gitleaks; frontend: lint, test, build, job `e2e` | puerta de calidad | Sonar y mutación no están en el CI (PENDIENTE) |
| Producción (Railway) | integrada con Core y M3/M5/M6/M1; `main` despliega automáticamente | solo pruebas manuales autorizadas y de bajo impacto (15/11) | no se hacen pruebas de carga ni de intrusión; pool de base limitado a 3 conexiones |

## 4. Datos de prueba
- Usuarios de desarrollo del stub (`StubIdentityValidator`): rol ciudadano, inspector y administrador. Las contraseñas **no se reproducen en este documento**; están en el código de pruebas y el README.
- Fixtures: `src/test/java/.../support/{Payloads,TestUsers}.java`; eventos de entrada inyectados con `scripts/inyectar-street-closure.sh`.
- Mocks del compose: `docker/wiremock/mappings` (login de M1: 200, 200, 401; Core: token de módulo y publicaciones).
- Datos de IA: `docs/ia/eval-set.json` (la corrida real contra el proveedor está PENDIENTE: requiere clave cargada por una persona).
- Producción: solo datos marcados como prueba (patente `TEST123`, calle "Av. de Prueba").

## 5. Criterios de entrada y de salida

**Entrada** (para ejecutar una ronda): rama integrada sin conflictos; `./mvnw -B verify` compila; compose levanta con `.env` completo (falta de variable corta con mensaje); Docker disponible.

**Salida** (para dar la entrega por probada):
1. Backend: `./mvnw verify` en verde, cobertura de líneas >= 85 % (hoy 88,5 %, reporte I1).
2. Frontend: lint sin errores, `npm run test:coverage` en verde, >= 85 % de líneas (hoy 93,43 %), build OK, E2E 7/7.
3. CI verde sobre el tag `v2.0-entrega4` (PENDIENTE: el tag no existe).
4. 0 defectos Bloqueantes, 0 Críticos y como máximo 3 Mayores abiertos con workaround. **Hoy NO se cumple** (ver `05-registro-defectos.md`, sección "Riesgo de salida").
5. Mutation >= 60 % (PENDIENTE), k6 p95 < 500 ms (PENDIENTE), ZAP sin High (PENDIENTE), sin CVE crítica sin mitigación (PENDIENTE), Sonar Quality Gate aprobado (PENDIENTE).
6. Suite repetida 3 veces sin resultados intermitentes (PENDIENTE para backend; frontend E2E 3/3 verificado).

## 6. Riesgos priorizados (probabilidad x impacto)

Escala: Probabilidad 1 (rara) a 3 (probable); Impacto 1 (menor) a 3 (alto). Puntaje = P x I. Prioridad: 6-9 alta, 3-4 media, 1-2 baja.

| Id | Riesgo | P | I | Pts | Prio | Pruebas que lo cubren | Estado de la cobertura |
|---|---|---|---|---|---|---|---|
| R1 | Credenciales de desarrollo fijas activas en producción (login stub) | 3 | 3 | 9 | Alta | `RestIdentityContextTest` (el adaptador REST funciona); revisión de config; ZAP autenticado | Riesgo ABIERTO: ninguna prueba lo elimina, solo la configuración de Railway |
| R2 | Pérdida de eventos si el broker está caído (sin outbox) | 2 | 3 | 6 | Alta | Prueba manual con compose (stop/start RabbitMQ, reporte A1); `HealthWithBrokerDownTest` | Parcial: reconexión verificada; no hay prueba de no pérdida (no hay outbox) |
| R3 | BOLA/IDOR: ciudadano accede a recursos ajenos | 2 | 3 | 6 | Alta | `SecurityEvidenceTests.bolaInfractions/bolaImpoundments/bolaAccidents/bolaParkingSession` | Cubierto salvo sesiones de estacionamiento, donde la prueba FALLA (defecto abierto) |
| R4 | Transiciones inválidas del corte de calle (activar sin aprobar, etc.) | 2 | 3 | 6 | Alta | `TC-BE-SC-07`, `OtherServicesTest.closureApproveOnlyFromRequested`, `StateTransitionTests` | Cubierto (409) |
| R5 | Fuga de datos personales (DNI en JWT, payload en logs) | 3 | 2 | 6 | Alta | `SecurityEvidenceTests.jwtHasNoPii` (falla), `AuthComponentsTest` | Defecto confirmado; no hay prueba de logs (PENDIENTE) |
| R6 | Evento duplicado o inválido procesado dos veces | 2 | 2 | 4 | Media | `IncomingEventDispatcherTest`, `StreetClosureRequestedHandlerTest`, `TC-BE-SC-02` | Cubierto |
| R7 | Indisponibilidad o respuesta inválida de M1 (login) | 2 | 2 | 4 | Media | `RestIdentityContextTest`, `RestIdentityValidatorTest` (timeout, 5xx, rechazo de conexión) | Cubierto con M1 simulado; contra M1 real NO VERIFICABLE (adaptador sin activar en producción) |
| R8 | Despliegue de `main` rompe producción (deploy automático, el Dockerfile compila los tests) | 2 | 3 | 6 | Alta | CI `./mvnw verify`; smoke manual post-deploy (health, Core) | Parcial: no hay smoke automático post-deploy |
| R9 | Sugerencia de IA incorrecta o manipulada (inyección de prompt) | 2 | 2 | 4 | Media | `InfractionSuggestionServiceTest.promptInjectionCannotChangeContract`, `InfractionSuggestionIntegrationTest.promptInjection/piiMasked`, `GroqAiGatewayTest` | Cubierto con proveedor simulado; eval real PENDIENTE |
| R10 | Degradación bajo carga (pool de 3 conexiones) | 2 | 2 | 4 | Media | k6 | PENDIENTE |
| R11 | Fuerza bruta sobre el login | 2 | 2 | 4 | Media | `SecurityEvidenceTests.bruteForce` (429) | Cubierto |
| R12 | Requisito sin implementar o sin prueba (cálculo de importe, costos de retención, HU-COR-05, filtros e indicadores del backend) | 3 | 2 | 6 | Alta | Matriz de trazabilidad (02), sección "sin cobertura" | Declarado; sin prueba por no existir la función |
| R13 | Secretos en el historial de Git | 1 | 3 | 3 | Media | gitleaks sobre historial (B2) | 4 hallazgos, todos en `.gitleaksignore` con comentario; riesgo aceptado, no resuelto |

Matriz (celdas con ids de riesgo):

| P \ I | 1 | 2 | 3 |
|---|---|---|---|
| 3 | | R5, R12 | R1 |
| 2 | | R6, R7, R9, R10, R11 | R2, R3, R4, R8 |
| 1 | | | R13 |

## 7. Gestión de defectos y reporte
Registro en `05-registro-defectos.md`; severidades Bloqueante / Crítica / Mayor / Menor según definiciones de la cátedra. Los resultados se informan en `04-informe-ejecucion.md`.

## 8. Responsables
Alejo García (QA/DevOps), Romina Fabian (PO), Lautaro Abrego (backend), Fernando Abdala (frontend). Este borrador lo escribió un agente de IA y debe ser revisado por una persona antes de la entrega.
