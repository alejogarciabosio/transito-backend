# 04 - Informe de ejecución de pruebas

Actualizado: 15/11/2026 ~21:10 ART (circuito M3 v1.1 confirmado por Core y M3: DEF-33 cerrado; DEF-42 corregido en el PR `fix/s22-activacion-en-ventana`). Estado anterior: 15/11/2026 ~20:40 ART. Versión probada: rama `integration/entrega4-f` del backend (= `main` `b3b990f`, que ya incluye `integration/entrega4-e` (#52), + PR #55 listado bajo carga, #54 descargo persistido y #53 k6 final); se mergea y despliega inmediatamente después del CI. Frontend en producción: `main` `6040e60` (PR #12, desplegado). Entorno de las pruebas automáticas: máquina local de los agentes y runners de GitHub Actions; PostgreSQL 16 por Testcontainers y compose local (backend + PostgreSQL + RabbitMQ + mocks). Carga, ZAP y Newman se corrieron **contra el compose local, nunca contra producción**.
Regla: solo figuran datos que existen en los reportes de los agentes, en el CI o que se corrieron para este informe. Donde una medición es anterior al estado final, se indica.

## 1. Resumen ejecutivo
| Indicador | Valor | Fuente |
|---|---|---|
| Backend `./mvnw -q -B verify` sobre `integration/entrega4-f` | exit 0; **518 tests**; 0 fallos; 0 errores; 0 omitidos | corrida del agente I7, 15/11 ~20:33 (`target/surefire-reports`); antes: 512 (`integration/entrega4-e`), 497 (`57b2765`) |
| Cobertura backend (JaCoCo) | **94,34 % de líneas** (2068/2192; mínimo 85 %); ramas 82,09 % (614/748); instrucciones 94,02 % | misma corrida (`jacoco.csv`) |
| Integración con Testcontainers (PostgreSQL + RabbitMQ reales) | **5 tests, 0 fallos**: outbox publica (TC-BE-TC-01), broker caído → PENDING → una sola publicación (TC-02), idempotencia por `eventId` (TC-03), mensajes inválidos a la DLQ (TC-04) y log sin cuerpo (TC-05, DEF-05) | PR #50; corrida I6 |
| Contratos OpenAPI / WSDL | `OpenApiStaticContractTest` (3) y `SoapWsdlContractTest` (2) en verde: `openapi.yaml` = springdoc y cubre todos los handlers; WSDL versionado = el que expone la app | PR #51; corrida I6 |
| IA (gateway Groq) | `AiGatewaySafetyTest` (6): inyección de prompt, fuera de dominio, sin fuga de DNI/patente/JWT, caída y timeout; mock WireMock en el compose | PR #51; corrida I6 |
| Frontend `vitest run --coverage` sobre `1f0187d` | **60 tests**, 0 fallos; **93,48 % de líneas** | corrida D3, 15/11; CI de `main` del frontend en verde |
| E2E Playwright | 7/7 en el CI de `main` del frontend; 3 corridas sin inestables | CI frontend; `reports/i2.md` |
| Accesibilidad axe | 0 violaciones en 360/768/1280 | `reports/i2.md`, `reports/a3.md` |
| Contrato y negativos (Newman) | **273 requests, 695 aserciones, 0 fallas**; los 6 casos de defectos conocidos `[KI-1]`..`[KI-5b]` informan "corregido" (0 omitidos) | job `api-tests` del CI de `main` (run 43764963355) |
| Carga (k6, 50 VU × 5 min) | base vacía: **p95 = 13,3 ms**, 0,00 % errores (0 de 44 259); con 5 950 infracciones y la corrección de DEF-40: **p95 = 309 ms**, listado 424 ms, 0,003 % (1 de 36 129, timeout de red del cliente; 0 HTTP 500) | [evidencias/k6](evidencias/k6/README.md) y [con-datos](evidencias/k6/con-datos.md), 15/11 (antes de DEF-40, con datos: p95 10,05 s y 24,6 % de HTTP 500) |
| Mutation (PIT) | **67 %** perfil del pom (200/299); **92 %** alcance ampliado (321/348) | `reports/t2.md`, `mutation-resumen.md` (medido antes de los PR #31/#39/#48) |
| ZAP | activo autenticado: **0 High, 0 Medium** (s8); baseline del CI de `main`: 0 FAIL, 1 WARN informativo (Non-Storable Content), 67 PASS; la alerta Low de CORP ya no aparece | `reports/s8.md`, [evidencias/zap](evidencias/zap/README.md), job `zap-baseline` |
| Dependencias | Trivy backend **0 High/Critical** con fix (antes 25); frontend `npm audit` 0 y Trivy 0 | job `dependencias` del CI de `main`; `reports/s2.md`, `reports/t2.md` |
| SonarCloud | **Quality Gate PASSED** en backend y frontend | job `sonarcloud` del CI de `main` de ambos repos |
| Tests inestables (flaky) | `verify` ×3 sin fallos ni diferencias (349 tests en ese momento); E2E 3/3 | `reports/t2.md`, `reports/i2.md` |
| gitleaks | 0 con `.gitleaksignore` sobre el historial completo | job `gitleaks` del CI de `main` |
| CI de `main` | backend: 6 jobs del workflow en verde (test, gitleaks, Trivy, Newman, ZAP, Sonar QG) + SonarCloud Code Analysis; frontend: test, E2E, Sonar, gitleaks en verde | GitHub Actions |
| Integración real con otros módulos | M3 corte ✅, M3 circuito v1.1 con `causationId` ✅, M6 corte ✅, M3 accidente ✅; M5, M2 y M1 pendientes | sección 7 |

Nota sobre conteos: los reportes intermedios informan 285, 364, 437, 496 y 497 tests porque se midieron sobre ramas anteriores; el número actual es **518** (suma de los `<testsuite tests>` de Surefire sobre `integration/entrega4-f`; respecto de 512: +3 de #54 (TC-BE-INF-15b/c/d), +2 de #55 (`ListadoConDatosRegressionTest`) y +1 de la integración (`listadoConDescargoSinNMasUno`)).

## 2. Resultados por nivel
| Nivel | Ejecutados | Pasados | Fallados | Bloqueados | Fuente / nota |
|---|---|---|---|---|---|
| Unitario + integración backend (build normal) | 518 | 518 | 0 | 0 | corrida I7 sobre `integration/entrega4-f` (incluye Testcontainers, contratos e IA) |
| Integración con Testcontainers (PostgreSQL + RabbitMQ) | 5 | 5 | 0 | 0 | `OutboxRabbitContainerIntegrationTest` (2), `ConsumerRabbitContainerIntegrationTest` (3) |
| Backend grupo `bug` (excluido del build) | 19 | 16 | 1 (`bug09a`) | 2 (errores de preparación: `bug02`, `bug14`) | corrida D3 sobre `57b2765`. Los 3 son pruebas desactualizadas (DEF-17, DEF-18), no defectos del producto |
| Frontend unitario/componentes (Vitest) | 60 | 60 | 0 | 0 | corrida D3 sobre `1f0187d` |
| E2E (Playwright, API simulada) | 7 | 7 | 0 | 0 | CI de `main` del frontend (antes: 3 corridas × 7, I2) |
| Contrato / API (Newman, compose local) | 273 requests / 695 aserciones | 695 | 0 | 0 omitidas | CI de `main` |
| Carga (k6, compose local) | 44 259 (base vacía) + 36 129 (con datos) requests | umbrales cumplidos en ambas (exit 0) | 0 / 1 (timeout de red del cliente) | | [evidencias/k6](evidencias/k6/README.md) (`b3b990f` y rama de #55, 15/11) |
| Seguridad dinámica (ZAP, compose local) | activo (34 rutas) + baseline en CI | 0 High / 0 Medium | 0 FAIL en baseline | | `reports/s8.md`, CI de `main` |
| Integración real con otros módulos | ver sección 7 | | | | |

Diferencia con la corrida del grupo `bug` anterior (D1, 22 tests): `bolaParkingSession` y `jwtHasNoPii` salieron del grupo porque sus defectos se corrigieron (DEF-15, DEF-06) y hoy corren en el build normal; `bug04` se retiró porque contradecía la regla de la PO (DEF-16).

## 3. Cobertura y calidad de las pruebas
| Medición | Valor | Estado |
|---|---|---|
| Backend líneas (JaCoCo, gate 85 %) | 94,34 % (2068/2192) | medido (I7, `integration/entrega4-f`) |
| Backend ramas / instrucciones globales | 82,09 % (614/748) / 94,02 % | medido (I7, `integration/entrega4-f`) |
| Backend por capa (service + model + listener) | líneas 94,2 %, ramas 87,4 % | medido (T2, antes del merge conjunto) |
| Frontend líneas | 93,48 % | medido (D3, `1f0187d`) |
| Mutation, perfil `mutation` del pom (`service.*`) | 67 % (200/299); objetivo ≥ 60 %: cumple | medido (T2) |
| Mutation, alcance ampliado (service + model + listener, toda la suite) | 92 % (321/348) | medido (T2) |
| Flaky backend | 3 corridas de `verify`, 0 fallos, sin diferencias | medido (T2, 349 tests); no repetido ×3 sobre `57b2765` |
| Flaky E2E | 3/3 sin inestables | medido (I2) |

## 4. No funcional
### 4.1 Tiempos
| Medición | Valor | Fuente |
|---|---|---|
| `/actuator/health` con broker pausado, antes del fix | sin respuesta en 40 s | `reports/b4.md` |
| `/actuator/health` con broker pausado, después del fix | 200 UP en ~3 ms (local) | `reports/b4.md` |
| `/actuator/health` en producción tras los deploys del 15/11 | 0,7 s / 0,9 s / 0,7 s / 0,76 s, UP | `TABLERO.md` |
| Arranque del compose local con imágenes en caché | 23 s | `reports/a1.md` |
| Reconexión al broker tras stop/start (local) | ~9 s; con outbox, el evento PENDING pasa a PUBLISHED a los ~9 s | `reports/a1.md`, `reports/s12.md` |
| Duración de los jobs del CI (PR #48) | test 2 min 25 s; Newman 2 min 30 s; ZAP 3 min 15 s; Trivy 1 min 37 s; Sonar 3 min 10 s | `reports/i5.md` |

### 4.2 Performance (k6)
Dos corridas del criterio, ambas con el mismo script `tests/k6/load.js` (50 VU × 5 min; login + `GET /api/v1/infractions` + `GET /api/v1/street-closures/current`), imagen `grafana/k6:1.3.0` (versión fija), compose local con pool Hikari de 3 conexiones (igual que producción), **nunca producción**. Umbrales: p95 < 500 ms (global y por endpoint) y errores < 1 %.

| Métrica | (1) Base vacía, `main` `b3b990f` (15/11 22:33 UTC) | (2) Con datos: 5 950 infracciones, con la corrección de DEF-40 (15/11 23:00-23:20 UTC) | Referencia (2) sin la corrección, 1 min |
|---|---|---|---|
| p95 global | **13,3 ms (cumple)** | **309 ms (cumple)** | 10,05 s |
| p95 listado de infracciones | 16,4 ms | **424 ms** (mediana 156 ms) | 10,56 s |
| p95 cortes vigentes / login | 11,9 ms / 10,5 ms | 100 ms / 47 ms | 7,76 s / 21 ms |
| Errores | **0,00 % (0 de 44 259)** | **0,003 % (1 de 36 129)**: un `dial: i/o timeout` del cliente k6; 0 HTTP 500 | 24,6 % (218 de 885), HTTP 500 por pool agotado |
| Throughput | 147 req/s | 120 req/s | 12 req/s |
| Exit de k6 | 0 | 0 | 99 |

Evidencia: [evidencias/k6/README.md](evidencias/k6/README.md) (corrida 1 y complementarios) y [evidencias/k6/con-datos.md](evidencias/k6/con-datos.md) (corrida 2, diagnóstico de 11 901 → 2 consultas por listado). La corrida anterior de T1 (341 ms) no es comparable: no registró estado de la base ni versión de k6.
Complementarios (fuera del criterio): (a) `tests/k6/outbox.js`, 50 VU × 2 min de `POST /api/v1/infractions`: p95 28,5 ms, 0 errores en 5 951; los 5 950 eventos quedaron PUBLISHED ~110 s después del fin de la carga (relay ≈ 25 eventos/s; demora creación→publicación p95 120 s), 0 FAILED. (b) Con datos, 1 min después de la corrección: p95 global 226 ms, listado 543 ms (calentamiento de la JVM en la ventana corta), 0 errores. Riesgo residual: el listado sigue sin paginar (respuesta de 3,6 MB con 5 950 filas); con volúmenes mucho mayores habría que agregar paginación opcional aditiva.

### 4.3 Quality Gate
| Puerta | Umbral | Resultado |
|---|---|---|
| Cobertura backend | ≥ 85 % líneas | 94,34 %: cumple |
| Cobertura frontend | ≥ 85 % líneas | 93,48 %: cumple |
| Tests del build | 0 fallos | 0 fallos (518 backend; frontend 60, medido sobre `1f0187d`): cumple |
| E2E | ≥ 5 escenarios en CI | 7 escenarios en el job `e2e`: cumple |
| SonarCloud Quality Gate | aprobado | PASSED en backend y frontend: cumple. Los proyectos son privados: métricas globales solo en el dashboard |
| Mutation | ≥ 60 % | 67 % (perfil del pom): cumple |
| Secretos | 0 | cumple con `.gitleaksignore` (ver DEF-12) |
| ZAP | sin alertas High | 0 High, 0 Medium: cumple |
| Dependencias | sin CVE High/Critical sin mitigación | Trivy 0 (job `dependencias` bloqueante): cumple |
| Carga | p95 < 500 ms con 50 VU | base vacía 13,3 ms; con 5 950 infracciones 309 ms: cumple |
| Defectos | 0 Bloqueantes, 0 Críticos, ≤ 3 Mayores abiertos con workaround | 0 / 0 / **3** (DEF-19, 20, 33): cumple, en el límite. Abiertos por severidad: Bloqueante 0, Crítica 0, Mayor 3, Menor 5 (DEF-07, 10, 17, 18, 21); cerrados 30, aceptados 2 (ver `05`). Condición: DEF-38/40/41 cuentan como cerrados porque `integration/entrega4-f` se despliega tras el CI; sin ese deploy serían 5 Mayores abiertos |
| CI verde sobre `v2.0-entrega4` | verde | **PENDIENTE**: el tag se crea al final, con CI verde (lo hace una persona). Hoy: CI de `main` `57b2765` en verde |

### 4.3b Evidencia de arquitectura, contratos e IA (PR #51)
- **ADRs**: `docs/adr/ADR-001..006` (GRASP Controller, Adapter de identidad M1, Observer/pub-sub, Gateway de IA, State, Transactional Outbox) e índice `docs/adr/README.md`.
- **OpenAPI / WSDL**: `openapi.yaml` (raíz y `static/`) generado desde springdoc (36 rutas) y `docs/contratos-integracion/authentication.wsdl`, ambos protegidos por tests de contrato (ver sección 1).
- **IA**: modelo por defecto `openai/gpt-oss-20b` (`ai.model`), apagada por defecto (`AI_ENABLED=false`), mock de Groq en el servicio `mocks` del compose; `AiGatewaySafetyTest` cubre seguridad y fallas.
- **HU pendientes**: `docs/entrega-academica/hu-pendientes.md` con el workaround operativo de cada criterio (DEF-20).

### 4.4 Seguridad dinámica y dependencias
Ver `06-informe-seguridad.md`. ZAP: [evidencias/zap](evidencias/zap/README.md) y job `zap-baseline`. Trivy: job `dependencias` del CI.


### 4.5 Ambiente de pruebas
- **Ambiente de testing: compose local reproducible** (`docker compose up`, un comando, ~75 s desde cero): backend, PostgreSQL, RabbitMQ y simuladores de M1, Core e IA (WireMock). Es el mismo ambiente que levanta el CI en cada PR (Newman, ZAP y E2E corren contra él) y el que usan los tests con Testcontainers (PostgreSQL + RabbitMQ reales).
- **Integración entre módulos:** validada en producción (Railway) coordinada con Core, que monitorea entregas, intentos y DLQ (ver sección 7).
- No se usó un entorno de test separado en la nube por costo: el entorno `test` de Railway quedó sin desplegar.

## 5. Defectos encontrados en esta ronda
Ver `05-registro-defectos.md`. Hallazgos del 15/11: KI-2 (Newman), 25 CVE (Trivy), 500 con textos largos/NUL (ZAP), `causationId` nulo (Core), fecha sin offset (M5), constraint sin `EXPIRED` (latente), KI-3/4/5 (Newman), CORP (ZAP), `updateTicketStatus` fuera del esquema de Core (revisión de contrato), y al cierre: `show-sql` ruidoso en producción (DEF-32) e incompatibilidad del consumidor de M3 con `causationId` (DEF-33). Todos menos DEF-32, DEF-33 y los preexistentes DEF-07/10/17/18/19/20/21 quedaron cerrados y desplegados. Después: DEF-32 y la ampliación de DEF-05 (log de mensajes AMQP inválidos sin cuerpo, `SafeRejectingErrorHandler`, TC-BE-TC-05) se cerraron en el PR #50 (en `main` por #52). Al escribir el manual de usuario (S18) aparecieron 4 fallas de UI del frontend (DEF-34 Mayor, DEF-35/36/37 Menores), cerradas con el PR frontend #12 (desplegado, `6040e60`). Después: k6 con datos destapó el listado degradado (DEF-40, Mayor) y el password generado en el log (DEF-41, Menor), y al analizar DEF-35 apareció el descargo no persistido (DEF-38, Mayor); los tres se corrigieron en #55 y #54 y quedan cerrados con el deploy de `integration/entrega4-f`. DEF-33 quedó cerrado con la confirmación de Core (20:51) y M3 (20:54) del circuito v1.1; en esa prueba Core halló la activación antes del inicio de la ventana (DEF-42, Mayor, corregido en el PR `fix/s22-activacion-en-ventana`; DEF-07 queda como su duplicado).

## 6. Conclusión
Build, cobertura, mutation, Sonar, ZAP, dependencias, Newman y carga cumplen sus umbrales con evidencia. La condición de defectos queda en 0 / 0 / 3 Mayores abiertos (DEF-19, 20, 42; DEF-33 cerrado; con el merge y deploy del PR de DEF-42 quedan 2), en el límite, una vez desplegada `integration/entrega4-f` (cierra DEF-38 y DEF-40). DEF-20 tiene workaround operativo documentado. Falta: tag `v2.0-entrega4` con CI verde (se crea al final), y cerrar las integraciones pendientes con M5, M2 y M1 (sección 7).

## 7. Pruebas de integración con otros módulos (producción, 15/11)
Ejecutadas por el equipo sobre el entorno integrado con Core; resultados confirmados por Core y por cada módulo (fuente: `pruebas-modulos-15-11.md`). Los ids son de datos de prueba.
| # | Flujo | Ids | Resultado |
|---|---|---|---|
| 1 | Corte M3 → M7 (respuesta `streetClosureApproved` y `streetClosureEnded`) | closureRequestId `44555606…`, streetClosureId `c5941b88…`; Approved `6f521edf-177f-43c4-b3ec-0a3eb1245fd5`, Ended `c8479b39-4741-49f3-a3ea-f255d16416c2` | ✅ Core: ROUTED y DELIVERED a `q.obras` y `q.ambiente` en 1 intento, sin DLQ, correlationId conservado. M3: procesó ambos, corte ENDED. Hallazgo: Ended sin causationId (DEF-08, cerrado después) |
| 1b | Re-prueba del corte M3 con `causationId` (circuito v1.1) | 15/11 19:45–19:48: Approved `614222ce-715d-44e7-8e63-89250488ea7b`, Ended `7f098173-82c6-4831-849f-47a3b5ec0cb4`, `causationId` `7f371af0-…` | ✅ Core (20:51): ROUTED y DELIVERED a `q.obras` y `q.ambiente` en 1 intento, correlationId `3f2e4d0b-…` y causationId `7f371af0-…` correctos, 0 dead letters. M3 (20:54): ambos PROCESSED, corte ENDED. **DEF-33 cerrado**. Hallazgo de Core: el corte se activó a las 19:45:51 con `startDate` 19:46:41 (DEF-42, corregido en PR: la activación exige estar dentro de `[startDate, endDate)`) |
| 2 | Corte M6 → M7 | eventId entrante `3db7aa72-e6cd-4908-a53a-97d0adacdfbc`, closureRequestId `fc780f03-898b-47e8-b374-af61cb4ab86b`, streetClosureId `91b26e25-822b-42d3-84cb-93d49e6ad1a5`; Approved `d25ff155-0b92-4f7d-b529-230f40457a8c`, Ended `af4062fb-4bde-4134-864a-60f6431086d9` | ✅ M6 confirmó ambos eventos (16:46:10 aprobado, 16:46:31 finalizado), correlationId y closureRequestId conservados, sin errores ni reintentos. Eventos previos al fix de causationId. Nota: la ventana autorizada (16:47-18:46) no fue la pedida (16:30-17:30) porque el script fija ahora+1 min a +2 h |
| 3 | `roadAccidentRegistered` M7 → M3 | eventId `9d7b06ec-dfa3-4ca6-9d28-acc17ed82523`, accidentId `c4c66355…`, dead letter `c5f8d3df-7d2c-4c50-8f6c-a224dae5c0e5` | ✅ Primero DEAD en `q.obras` (consumidor de M3 rechazaba; luego IP de M3 no habilitada en Core). Core reintentó una sola vez la dead letter conservando el eventId → RETRIED/SUCCESS. M3 (17:33): Inbox PROCESSED, 1 intento, 1 proyección, sin persistir DNI/patente/personas/inspectorId. M7 no republicó nada |
| 4 | `infractionConfirmed` M7 → M5 (y `paymentRegistered` M5 → M7) | eventId `114ab437-f36a-4f26-a565-0a3b59b2b88b`, infractionId `cb63dbb5…` | 🔄 En curso. Core: DELIVERED a `q.rentas` en 1 intento (M5 sin consumidores en ese momento). La fecha con offset que pidió M5 está corregida y desplegada (DEF-25); se les envió el envelope sin credenciales. M5 espera un DNI válido para M1 para procesar; luego pago simulado → `paymentRegistered` |
| 5 | Reclamos M2 → M7 (`ticketUpdated` → `updateTicketStatus`) | — | ⬜ PENDIENTE: falta que M2 publique su `ticketUpdated` derivado a M7. Del lado de M7, `updateTicketStatus` ya cumple el esquema de Core (DEF-31, `CoreSchemaContractTest`) |
| 6 | Identidad M1 (adaptador REST de login) | — | ⬜ PENDIENTE: faltan las cuentas de M1 para probar; el adaptador está corregido (DEF-03) y probado con M1 simulado, pero sin activar en producción (`USERS_REST_ENABLED` sin definir) |
