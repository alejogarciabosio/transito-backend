# 04 - Informe de ejecución de pruebas

Actualizado: 16/11/2026 (confirmaciones recibidas el 15/11 por la noche: M3 validó las pruebas adicionales de rechazo, cierre automático y vencimiento sin activar (22:45, sección 7 fila 1c) y M2 validó el circuito de reclamos (23:11, fila 5); M5 queda parcial (fila 4); DEF-43 cerrado con el PR #59). Estado anterior: 15/11/2026 ~21:50 ART (login contra M1 real verificado en local con el adaptador REST: fila 6 de la sección 7; circuito M3 v1.1 confirmado por Core y M3: DEF-33 cerrado; DEF-42 corregido en el PR #57 `fix/s22-activacion-en-ventana`, que se mergea y despliega al pasar el CI; con ese PR: 532 tests, 94,45 % de líneas; frontend `main` `0279950`: 65 tests, 94,04 %). Estado anterior: 15/11/2026 ~20:40 ART. Versión probada: rama `integration/entrega4-f` del backend (= `main` `b3b990f`, que ya incluye `integration/entrega4-e` (#52), + PR #55 listado bajo carga, #54 descargo persistido y #53 k6 final); se mergea y despliega inmediatamente después del CI. Frontend en producción: `main` `6040e60` (PR #12, desplegado). Entorno de las pruebas automáticas: máquina local de los agentes y runners de GitHub Actions; PostgreSQL 16 por Testcontainers y compose local (backend + PostgreSQL + RabbitMQ + mocks). Carga, ZAP y Newman se corrieron **contra el compose local, nunca contra producción**.
Regla: solo figuran datos que existen en los reportes de los agentes, en el CI o que se corrieron para este informe. Donde una medición es anterior al estado final, se indica.

## 1. Resumen ejecutivo
| Indicador | Valor | Fuente |
|---|---|---|
| Backend `./mvnw -q -B verify` sobre la rama del PR #57 | exit 0; **532 tests**; 0 fallos; 0 errores; 0 omitidos | corrida del 15/11 ~21:30 (`target/surefire-reports`) y job `test` del CI del PR; antes: 518 (`integration/entrega4-f`), 512, 497 (`57b2765`) |
| Cobertura backend (JaCoCo) | **94,45 % de líneas** (mínimo 85 %); ramas 82,43 % (638/774); instrucciones 94,17 % | misma corrida (`jacoco.csv`) |
| Integración con Testcontainers (PostgreSQL + RabbitMQ reales) | **5 tests, 0 fallos**: outbox publica (TC-BE-TC-01), broker caído → PENDING → una sola publicación (TC-02), idempotencia por `eventId` (TC-03), mensajes inválidos a la DLQ (TC-04) y log sin cuerpo (TC-05, DEF-05) | PR #50; corrida I6 |
| Contratos OpenAPI / WSDL | `OpenApiStaticContractTest` (3) y `SoapWsdlContractTest` (2) en verde: `openapi.yaml` = springdoc y cubre todos los handlers; WSDL versionado = el que expone la app | PR #51; corrida I6 |
| IA (gateway Groq) | `AiGatewaySafetyTest` (6): inyección de prompt, fuera de dominio, sin fuga de DNI/patente/JWT, caída y timeout; mock WireMock en el compose | PR #51; corrida I6 |
| Frontend `npm run test:coverage` sobre `main` `0279950` | **65 tests**, 0 fallos; **94,04 % de líneas** (1059/1126) | corrida del 15/11 ~21:30 (`npm ci` limpio); antes: 60 tests y 93,48 % sobre `1f0187d` |
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
| Integración real con otros módulos | M3 corte ✅, M3 circuito v1.1 con `causationId` ✅ (validado por Core y M3), M3 rechazo, cierre automático y vencimiento sin activar ✅ (validado por M3), M6 corte ✅ (validado por M6), M3 accidente ✅, M2 reclamos ✅ (validado por M2), M1 login ✅ (adaptador REST en local contra M1 real); M5 **parcial** (entregado por Core, rechazado por formato de fecha ya corregido; falta un DNI válido de M5 para una infracción nueva). Pendiente: verificación de Core de las pruebas adicionales de M3 y del circuito M2 | sección 7 |

Nota sobre conteos: los reportes intermedios informan 285, 364, 437, 496 y 497 tests porque se midieron sobre ramas anteriores; el número actual es **532** (rama del PR #57: +14 tests de DEF-42 sobre los 518 de `integration/entrega4-f`; respecto de 512, los 518 sumaban +3 de #54 (TC-BE-INF-15b/c/d), +2 de #55 (`ListadoConDatosRegressionTest`) y +1 de la integración (`listadoConDescargoSinNMasUno`)).

## 2. Resultados por nivel
| Nivel | Ejecutados | Pasados | Fallados | Bloqueados | Fuente / nota |
|---|---|---|---|---|---|
| Unitario + integración backend (build normal) | 532 | 532 | 0 | 0 | rama del PR #57 (incluye Testcontainers, contratos, IA y ventana de activación) |
| Integración con Testcontainers (PostgreSQL + RabbitMQ) | 5 | 5 | 0 | 0 | `OutboxRabbitContainerIntegrationTest` (2), `ConsumerRabbitContainerIntegrationTest` (3) |
| Backend grupo `bug` (excluido del build) | 19 | 16 | 1 (`bug09a`) | 2 (errores de preparación: `bug02`, `bug14`) | corrida D3 sobre `57b2765`. Los 3 son pruebas desactualizadas (DEF-17, DEF-18), no defectos del producto |
| Frontend unitario/componentes (Vitest) | 65 | 65 | 0 | 0 | `main` `0279950`, 15/11 ~21:30 |
| E2E (Playwright, API simulada) | 7 | 7 | 0 | 0 | CI de `main` del frontend (antes: 3 corridas × 7, I2) |
| Contrato / API (Newman, compose local) | 273 requests / 695 aserciones | 695 | 0 | 0 omitidas | CI de `main` |
| Carga (k6, compose local) | 44 259 (base vacía) + 36 129 (con datos) requests | umbrales cumplidos en ambas (exit 0) | 0 / 1 (timeout de red del cliente) | | [evidencias/k6](evidencias/k6/README.md) (`b3b990f` y rama de #55, 15/11) |
| Seguridad dinámica (ZAP, compose local) | activo (34 rutas) + baseline en CI | 0 High / 0 Medium | 0 FAIL en baseline | | `reports/s8.md`, CI de `main` |
| Integración real con otros módulos | ver sección 7 | | | | |

Diferencia con la corrida del grupo `bug` anterior (D1, 22 tests): `bolaParkingSession` y `jwtHasNoPii` salieron del grupo porque sus defectos se corrigieron (DEF-15, DEF-06) y hoy corren en el build normal; `bug04` se retiró porque contradecía la regla de la PO (DEF-16).

## 3. Cobertura y calidad de las pruebas
| Medición | Valor | Estado |
|---|---|---|
| Backend líneas (JaCoCo, gate 85 %) | 94,45 % | medido (rama del PR #57) |
| Backend ramas / instrucciones globales | 82,43 % (638/774) / 94,17 % | medido (rama del PR #57) |
| Backend por capa (service + model + listener) | líneas 94,2 %, ramas 87,4 % | medido (T2, antes del merge conjunto) |
| Frontend líneas | 94,04 % (1059/1126) | medido (`main` `0279950`) |
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
| Cobertura backend | ≥ 85 % líneas | 94,45 %: cumple |
| Cobertura frontend | ≥ 85 % líneas | 94,04 %: cumple |
| Tests del build | 0 fallos | 0 fallos (532 backend; 65 frontend): cumple |
| E2E | ≥ 5 escenarios en CI | 7 escenarios en el job `e2e`: cumple |
| SonarCloud Quality Gate | aprobado | PASSED en backend y frontend: cumple. Los proyectos son privados: métricas globales solo en el dashboard |
| Mutation | ≥ 60 % | 67 % (perfil del pom): cumple |
| Secretos | 0 | cumple con `.gitleaksignore` (ver DEF-12) |
| ZAP | sin alertas High | 0 High, 0 Medium: cumple |
| Dependencias | sin CVE High/Critical sin mitigación | Trivy 0 (job `dependencias` bloqueante): cumple |
| Carga | p95 < 500 ms con 50 VU | base vacía 13,3 ms; con 5 950 infracciones 309 ms: cumple |
| Defectos | 0 Bloqueantes, 0 Críticos, 0 Mayores abiertos | 0 / 0 / **0**: cumple. Abiertos por severidad: Bloqueante 0, Crítica 0, Mayor 0, Menor 5 (DEF-10, 17, 18, 19, 21); cerrados 33, aceptados 2 (DEF-09, 12), duplicado 1 (DEF-07 → DEF-42), reclasificado 1 (DEF-20: alcance no implementado, no defecto); total 42 (ver `05`). El 17/11 se revisaron DEF-19 (Mayor → Menor) y DEF-20 (reclasificado); la justificación está en `05` |
| CI verde sobre `v2.0-entrega4` | verde | **PENDIENTE**: el tag se crea al final, con CI verde (lo hace una persona). Hoy: CI de `main` `57b2765` en verde |

### 4.3b Evidencia de arquitectura, contratos e IA (PR #51)
- **ADRs**: `docs/adr/ADR-001..006` (GRASP Controller, Adapter de identidad M1, Observer/pub-sub, Gateway de IA, State, Transactional Outbox) e índice `docs/adr/README.md`.
- **OpenAPI / WSDL**: `openapi.yaml` (raíz y `static/`) generado desde springdoc (36 rutas) y `docs/contratos-integracion/authentication.wsdl`, ambos protegidos por tests de contrato (ver sección 1).
- **IA**: modelo por defecto `openai/gpt-oss-20b` (`ai.model`), apagada por defecto (`AI_ENABLED=false`), mock de Groq en el servicio `mocks` del compose; `AiGatewaySafetyTest` cubre seguridad y fallas.
- **HU pendientes**: `docs/entrega-academica/hu-pendientes.md` con el workaround operativo de cada criterio (alcance diferido a la próxima iteración, ex DEF-20).

### 4.4 Seguridad dinámica y dependencias
Ver `06-informe-seguridad.md`. ZAP: [evidencias/zap](evidencias/zap/README.md) y job `zap-baseline`. Trivy: job `dependencias` del CI.


### 4.5 Ambiente de pruebas
- **Ambiente de testing: compose local reproducible** (`docker compose up`, un comando, ~75 s desde cero): backend, PostgreSQL, RabbitMQ y simuladores de M1, Core e IA (WireMock). Es el mismo ambiente que levanta el CI en cada PR (Newman, ZAP y E2E corren contra él) y el que usan los tests con Testcontainers (PostgreSQL + RabbitMQ reales).
- **Integración entre módulos:** validada en producción (Railway) coordinada con Core, que monitorea entregas, intentos y DLQ (ver sección 7).
- No se usó un entorno de test separado en la nube por costo: el entorno `test` de Railway quedó sin desplegar.

## 5. Defectos encontrados en esta ronda
Ver `05-registro-defectos.md`. Hallazgos del 15/11: KI-2 (Newman), 25 CVE (Trivy), 500 con textos largos/NUL (ZAP), `causationId` nulo (Core), fecha sin offset (M5), constraint sin `EXPIRED` (latente), KI-3/4/5 (Newman), CORP (ZAP), `updateTicketStatus` fuera del esquema de Core (revisión de contrato), y al cierre: `show-sql` ruidoso en producción (DEF-32) e incompatibilidad del consumidor de M3 con `causationId` (DEF-33). Todos menos DEF-32, DEF-33 y los preexistentes DEF-07/10/17/18/19/20/21 quedaron cerrados y desplegados. Después: DEF-32 y la ampliación de DEF-05 (log de mensajes AMQP inválidos sin cuerpo, `SafeRejectingErrorHandler`, TC-BE-TC-05) se cerraron en el PR #50 (en `main` por #52). Al escribir el manual de usuario (S18) aparecieron 4 fallas de UI del frontend (DEF-34 Mayor, DEF-35/36/37 Menores), cerradas con el PR frontend #12 (desplegado, `6040e60`). Después: k6 con datos destapó el listado degradado (DEF-40, Mayor) y el password generado en el log (DEF-41, Menor), y al analizar DEF-35 apareció el descargo no persistido (DEF-38, Mayor); los tres se corrigieron en #55 y #54 y quedan cerrados con el deploy de `integration/entrega4-f`. DEF-33 quedó cerrado con la confirmación de Core (20:51) y M3 (20:54) del circuito v1.1; en esa prueba Core halló la activación antes del inicio de la ventana (DEF-42, Mayor; DEF-07 queda como su duplicado), cerrado con el PR #57 (cerrado — desplegado 15/11).

## 6. Conclusión
Build, cobertura, mutation, Sonar, ZAP, dependencias, Newman y carga cumplen sus umbrales con evidencia. La condición de defectos queda en 0 Bloqueantes, 0 Críticos, 0 Mayores abiertos (DEF-33 y DEF-42 cerrados). El 17/11 DEF-19 se bajó de Mayor a Menor (sigue abierto) y DEF-20 se reclasificó como alcance no implementado, diferido a la próxima iteración con workaround operativo (`hu-pendientes.md`); ambas decisiones están justificadas en `05`. Falta: tag `v2.0-entrega4` con CI verde (se crea al final), y completar la integración con M5 (parcial: falta que M5 pase un DNI válido para una infracción nueva; sección 7, fila 4). M2 y las pruebas adicionales de M3 quedaron validadas por esos módulos; resta la verificación de Core. M1 quedó validado en local contra M1 real; producción sigue con el validador local a propósito.

## 7. Pruebas de integración con otros módulos (producción, 15/11)
Ejecutadas por el equipo sobre el entorno integrado con Core (salvo la fila 6: en local contra M1 real, resultado verificado por el equipo); resultados confirmados por Core y por cada módulo (fuente: `pruebas-modulos-15-11.md`). Los ids son de datos de prueba.
| # | Flujo | Ids | Resultado |
|---|---|---|---|
| 1 | Corte M3 → M7 (respuesta `streetClosureApproved` y `streetClosureEnded`) | closureRequestId `44555606…`, streetClosureId `c5941b88…`; Approved `6f521edf-177f-43c4-b3ec-0a3eb1245fd5`, Ended `c8479b39-4741-49f3-a3ea-f255d16416c2` | ✅ Core: ROUTED y DELIVERED a `q.obras` y `q.ambiente` en 1 intento, sin DLQ, correlationId conservado. M3: procesó ambos, corte ENDED. Hallazgo: Ended sin causationId (DEF-08, cerrado después) |
| 1b | Re-prueba del corte M3 con `causationId` (circuito v1.1) | 15/11 19:45–19:48: Approved `614222ce-715d-44e7-8e63-89250488ea7b`, Ended `7f098173-82c6-4831-849f-47a3b5ec0cb4`, `causationId` `7f371af0-…` | ✅ Core (20:51): ROUTED y DELIVERED a `q.obras` y `q.ambiente` en 1 intento, correlationId `3f2e4d0b-…` y causationId `7f371af0-…` correctos, 0 dead letters. M3 (20:54): ambos PROCESSED, corte ENDED. **DEF-33 cerrado**. Hallazgo de Core: el corte se activó a las 19:45:51 con `startDate` 19:46:41 (DEF-42, cerrado con el PR #57: la activación exige estar dentro de `[startDate, endDate)`) |
| 1c | Pruebas adicionales de cortes M3 → M7 (rechazo, cierre automático, vencimiento sin activar) | 15/11 22:36–22:40 ART. A: `streetClosureRejected` `04edb9b6-9a45-4a68-b81f-4bd1c20979aa`. B: corte ACTIVE, `streetClosureApproved` `43597886-72a5-4028-9c75-822847197a3d` y `streetClosureEnded` automático (scheduler) `a6fe83e5-5339-4686-b399-6c1239571e34` (22:40:33). C: `streetClosureApproved` `1d426b5a-ac1b-428b-81dc-7f3890a6a35f`, sin activar | ✅ Validado por M3 (15/11 22:45): A PROCESSED, solicitud REJECTED; B ambos PROCESSED, solicitud ENDED; C PROCESSED, solicitud EXPIRED y sin `streetClosureEnded` (lo esperado: un corte APPROVED que vence no publica Ended). Correlaciones y `causationId` aceptados. Del lado de M7, los 4 eventos PUBLISHED en el intento 1. **Pendiente: verificación de Core** (entregas a `q.obras`, intentos, DLQ) |
| 2 | Corte M6 → M7 | eventId entrante `3db7aa72-e6cd-4908-a53a-97d0adacdfbc`, closureRequestId `fc780f03-898b-47e8-b374-af61cb4ab86b`, streetClosureId `91b26e25-822b-42d3-84cb-93d49e6ad1a5`; Approved `d25ff155-0b92-4f7d-b529-230f40457a8c`, Ended `af4062fb-4bde-4134-864a-60f6431086d9` | ✅ Validado por M6 (15/11 16:58): confirmó ambos eventos (16:46:10 aprobado, 16:46:31 finalizado), correlationId y closureRequestId conservados, sin errores ni reintentos. Eventos previos al fix de causationId. Nota: la ventana autorizada (16:47-18:46) no fue la pedida (16:30-17:30) porque el script fija ahora+1 min a +2 h |
| 3 | `roadAccidentRegistered` M7 → M3 | eventId `9d7b06ec-dfa3-4ca6-9d28-acc17ed82523`, accidentId `c4c66355…`, dead letter `c5f8d3df-7d2c-4c50-8f6c-a224dae5c0e5` | ✅ Primero DEAD en `q.obras` (consumidor de M3 rechazaba; luego IP de M3 no habilitada en Core). Core reintentó una sola vez la dead letter conservando el eventId → RETRIED/SUCCESS. M3 (17:33): Inbox PROCESSED, 1 intento, 1 proyección, sin persistir DNI/patente/personas/inspectorId. M7 no republicó nada |
| 4 | `infractionConfirmed` M7 → M5 (y `paymentRegistered` M5 → M7) | eventId `114ab437-f36a-4f26-a565-0a3b59b2b88b`, infractionId `cb63dbb5…` | ⚠ **Parcial, prueba NO completada.** Core entregó el evento a `q.rentas` en 1 intento, pero M5 lo rechazó porque `infractionDateTime` salía sin offset (DEF-25); corregido y desplegado el 15/11 17:50. A pedido de M5 se le envió una decisión escrita: se ratifica ISO-8601 con offset y el evento histórico se descarta (no se reenvía). Pendiente: que M5 pase un DNI válido para registrar una infracción nueva y repetir el circuito (luego pago simulado → `paymentRegistered`) |
| 5 | Reclamos M2 → M7 (`ticketUpdated` → `updateTicketStatus`) | 15/11: `ticketUpdated` entrante `83a759ab-95b1-4c31-9dee-2537f960245d` (ROUTED a M7) procesado 15:27 ART → operativo `a1c94b63-6818-4eeb-a3a9-5f52b774a5db` (COMPLAINT_VERIFICATION) → `updateTicketStatus` STARTED; al cerrar el operativo, `updateTicketStatus` RESOLVED eventId `3ff1f1a4-f7eb-4e58-a1d0-f7502f19fa17` (21:59:17 ART, `NO_FURTHER_ACTION_REQUIRED`, 1 intento) | ✅ **Validado por M2** (15/11 23:11): aplicó la resolución del ticket `50a85c7e-4778-4ba4-a330-61c105269c8c`, transición IN_PROGRESS → RESOLVED, motivo `NO_FURTHER_ACTION_REQUIRED`, evento 21:59:15 ART; el estado previo IN_PROGRESS es consistente con el STARTED publicado por M7. Del lado de M7: evento procesado, operativo creado, STARTED y RESOLVED publicados (esquema de Core, DEF-31). **Pendiente: verificación de Core.** Hallazgo: el operativo se mostró con inicio 18:27 y cierre 00:59 del 16/11 (hora UTC guardada como local, **DEF-43**, cerrado con el PR #59; los datos ya guardados no se modifican) |
| 6 | Identidad M1 (adaptador REST de login, ADR-002) | 15/11 21:46 ART; backend **local** `main` `bc3d6b7` con `USERS_REST_ENABLED=true` y `USERS_REST_URL` = login de M1 real (`https://ciudadanos-m1-backend.onrender.com/auth/usuario/login`); las 2 cuentas de prueba entregadas por M1 (credenciales no reproducidas) | ✅ Cuenta ciudadano → HTTP 200, JWT de M7 con `role` CIUDADANO (`sub` `CIUDADANO:400e00dd-…`), sin DNI. Cuenta empleado → HTTP 200, `role` ADMINISTRADOR (`sub` `EMPLEADO:4c6f7004-…`), sin DNI. El JWT lo firma M7 después de validar contra M1 (diseño del ADR-002). Producción sigue con el validador local (cuentas de prueba) a propósito, para no afectar la demo; timeout y errores de M1 siguen probados solo con M1 simulado (TC-BE-AUTH-M1) |
