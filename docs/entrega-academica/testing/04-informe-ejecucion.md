# 04 - Informe de ejecución de pruebas

Actualizado: 15/11/2026 ~18:00 ART. Versión probada: `main` del backend en `c196d7a` (merge del PR #39, desplegado en Railway) y `main` del frontend (PR #9 mergeado). Entorno de las pruebas automáticas: máquina local de los agentes y runners de GitHub Actions; base PostgreSQL 16 por Testcontainers y compose local (backend + PostgreSQL + RabbitMQ + mocks). Carga, ZAP y Newman se corrieron **contra el compose local, nunca contra producción**.
Regla: solo figuran datos que existen en los reportes de los agentes o que se corrieron para este informe. Lo demás dice **PENDIENTE**. No se estimaron ni redondearon números salvo donde se indica.

## 1. Resumen ejecutivo
| Indicador | Valor | Fuente |
|---|---|---|
| Backend `./mvnw -q -B verify` sobre `c196d7a` | exit 0; **437 tests**; 0 fallos; 0 errores; 0 omitidos | corrida del agente D2, 15/11 (suma de `target/surefire-reports`) |
| Cobertura backend (líneas, JaCoCo) | **91,92 %** (mínimo 85 %) | misma corrida (`jacoco.csv`) |
| Frontend `npm run test:coverage` | 59 tests, 0 fallos; 93,43 % líneas | `reports/s9.md` (local y CI) |
| E2E Playwright | 7/7 en cada una de 3 corridas | `reports/i2.md` |
| Accesibilidad axe | 0 violaciones en 360/768/1280 | `reports/i2.md`, `reports/a3.md` |
| Contrato y negativos (Newman) | **273 requests, 695 aserciones, 0 fallas** (6 omitidas = defectos conocidos) | `reports/t1.md`; job `api-tests` del CI en verde |
| Carga (k6, 50 VU × 5 min) | **p95 = 341 ms**, errores 0,01 % (7 de 35 923), 119 req/s | `reports/t1.md` |
| Mutation (PIT) | **67 %** perfil del pom (200/299); **92 %** alcance ampliado (321/348) | `reports/t2.md`, `mutation-resumen.md` |
| ZAP (baseline + API activo autenticado) | **0 High, 0 Medium, 1 Low**, 3 Info | `reports/s8.md`, [evidencias/zap](evidencias/zap/README.md) |
| Dependencias | Trivy backend **0 High/Critical** (antes 25); frontend `npm audit` 0 y Trivy 0 | `reports/s2.md`, `reports/t2.md`; job CI `dependencias` |
| SonarCloud | **Quality Gate PASSED** en backend y frontend | `reports/s9.md`; job CI `sonarcloud` |
| Tests inestables (flaky) | `verify` ×3: 0 fallos y sin diferencias entre corridas (349 tests en ese momento) | `reports/t2.md` |
| gitleaks | 0 con `.gitleaksignore` sobre el historial completo | `reports/b2b.md`; job CI `gitleaks` |
| CI de `main` (merge #39) | 7/7 jobs en verde | `TABLERO.md` |
| Integración real con otros módulos | ver sección 7 | `pruebas-modulos-15-11.md` |

Nota: el conteo de 437 es de esta corrida sobre `c196d7a`; los reportes individuales informan números menores (366-421) porque se midieron sobre sus ramas antes del merge conjunto.

## 2. Resultados por nivel
| Nivel | Ejecutados | Pasados | Fallados | Bloqueados | Fuente / nota |
|---|---|---|---|---|---|
| Unitario + integración backend (build normal) | 437 | 437 | 0 | 0 | corrida D2 sobre `c196d7a`. Desglose por nivel: PENDIENTE |
| Backend grupo `bug` (excluido del build) | 22 | 16 | 4 | 2 (errores de preparación) | corrida D1 del 15/11, **anterior** a los fixes de BOLA/State; PENDIENTE repetir (SEC-BOLA-04 ya salió del grupo) |
| Frontend unitario/componentes (Vitest) | 59 | 59 | 0 | 0 | `reports/s9.md` |
| E2E (Playwright, API simulada) | 7 por corrida, 3 corridas | 21 | 0 | 0 | `reports/i2.md` |
| Contrato / API (Newman, compose local) | 273 requests / 695 aserciones | 695 | 0 | 6 omitidas (KI conocidos) | `reports/t1.md`; en CI (job `api-tests`) |
| Carga (k6, compose local) | 35 923 requests | umbrales cumplidos (exit 0) | 7 requests (0,01 %) | | `reports/t1.md` |
| Seguridad dinámica (ZAP, compose local) | baseline + API activo (34 rutas) | 0 High / 0 Medium | 1 Low abierto (CORP) | | `reports/s8.md` |
| Integración real con otros módulos | ver sección 7 | | | | |

Las 6 aserciones omitidas de Newman correspondían a KI-1..KI-5/5b. KI-1 (BOLA, DEF-15) y KI-2 (rate limit, DEF-22) se cerraron después; KI-3, KI-4 y KI-5 siguen abiertos (DEF-27, 28, 29). **PENDIENTE**: volver a correr Newman sobre `c196d7a` para dejar asentado cuántas omitidas quedan.

Detalle de la corrida del grupo `bug` (D1, antes de los fixes): `bolaParkingSession` falló (DEF-15, hoy cerrado); `jwtHasNoPii` falló (DEF-06, abierto); `bug04` falló con 201 (DEF-16, abierto); `bug09a`, `bug02`, `bug14` son pruebas desactualizadas (DEF-17/18); los otros 16 pasaron.

## 3. Cobertura y calidad de las pruebas
| Medición | Valor | Estado |
|---|---|---|
| Backend líneas (JaCoCo, gate 85 %) | 91,92 % | medido (D2, `c196d7a`) |
| Backend por capa (service + model + listener) | líneas 94,2 %, ramas 87,4 % (235/269) | medido (T2, antes del merge conjunto) |
| Backend instrucciones / ramas globales | PENDIENTE de extraer de la corrida actual | |
| Frontend líneas | 93,43 % | medido (s9) |
| Mutation, perfil `mutation` del pom (`service.*`) | 67 % (200/299); objetivo ≥ 60 %: cumple | medido (T2) |
| Mutation, alcance ampliado (service + model + listener, toda la suite) | 92 % (321/348) | medido (T2) |
| Flaky backend | 3 corridas de `verify`, 0 fallos, sin diferencias | medido (T2, 349 tests) |
| Flaky E2E | 3/3 sin inestables | medido (I2) |

## 4. No funcional
### 4.1 Tiempos
| Medición | Valor | Fuente |
|---|---|---|
| `/actuator/health` con broker pausado, antes del fix | sin respuesta en 40 s | `reports/b4.md` |
| `/actuator/health` con broker pausado, después del fix | 200 UP en ~3 ms (local) | `reports/b4.md` |
| `/actuator/health` en producción tras los deploys del 15/11 | 0,7 s / 0,9 s / 0,7 s, UP | `TABLERO.md` |
| Arranque del compose local con imágenes en caché | 23 s | `reports/a1.md` |
| Reconexión al broker tras stop/start (local) | ~9 s | `reports/a1.md` |
| Duración total del `verify` y de cada nivel | PENDIENTE | |

### 4.2 Performance (k6)
Escenario: 50 VU durante 5 minutos; login + `GET /api/v1/infractions` + `GET /api/v1/street-closures/current`; compose local con pool Hikari de 3 conexiones (igual que producción). Umbrales: p95 < 500 ms y errores < 1 %.
| Métrica | Valor |
|---|---|
| p95 global | 341 ms (cumple) |
| Errores | 0,01 % (7 de 35 923) (cumple) |
| Throughput | 119 req/s |
| p95 por endpoint | login 150 ms; listado de infracciones 472 ms (cerca del límite); cortes 358 ms |
| Máximos | 2,6 s; una iteración llegó a 31 s |
Observaciones: la causa de los 7 errores no se investigó (sospecha: contención del pool de 3 conexiones). El listado de infracciones no está paginado y su tiempo crece con la base. Resumen en `tests/reports/k6-resumen.txt`.

### 4.3 Quality Gate
| Puerta | Umbral | Resultado |
|---|---|---|
| Cobertura backend | ≥ 85 % líneas | 91,92 %: cumple |
| Cobertura frontend | ≥ 85 % líneas | 93,43 %: cumple |
| Tests del build | 0 fallos | 0 fallos (437): cumple |
| E2E | ≥ 5 escenarios en CI | 7 escenarios en el job `e2e`: cumple |
| SonarCloud Quality Gate | aprobado | PASSED en backend y frontend: cumple. Los proyectos son privados: métricas globales solo en el dashboard |
| Mutation | ≥ 60 % | 67 % (perfil del pom): cumple |
| Secretos | 0 | cumple con `.gitleaksignore` (ver DEF-12) |
| ZAP | sin alertas High | 0 High, 0 Medium: cumple |
| Dependencias | sin CVE High/Critical sin mitigación | Trivy 0 (job `dependencias` bloqueante): cumple |
| Carga | p95 < 500 ms con 50 VU | 341 ms: cumple |
| Defectos | 0 Bloqueantes, 0 Críticos, ≤ 3 Mayores abiertos | 0 / 0 / **7**: **no cumple** (ver 05) |
| CI verde sobre `v2.0-entrega4` | verde | PENDIENTE: el tag no existe todavía |

### 4.4 Seguridad dinámica y dependencias
Ver `06-informe-seguridad.md`. ZAP: [evidencias/zap](evidencias/zap/README.md). Trivy: job `dependencias` del CI.

## 5. Defectos encontrados en esta ronda
Ver `05-registro-defectos.md`. Hallazgos nuevos del 15/11: KI-2 (Newman), 25 CVE (Trivy), 500 con textos largos/NUL (ZAP), `causationId` nulo (Core), fecha sin offset (M5), constraint sin `EXPIRED` (latente), KI-3/4/5 (Newman).

## 6. Conclusión
Build, cobertura, mutation, Sonar, ZAP, dependencias y carga cumplen sus umbrales con evidencia. **No se cumple** la condición de salida de defectos (7 Mayores abiertos, máximo 3). Faltan: tag `v2.0-entrega4` con CI verde, repetir Newman y el grupo `bug` sobre `main` actual, y cerrar las integraciones con M5, M2 y M1 (sección 7).

## 7. Pruebas de integración con otros módulos (producción, 15/11)
Ejecutadas por el equipo sobre el entorno integrado con Core; resultados confirmados por Core y por cada módulo (fuente: `pruebas-modulos-15-11.md`). Los ids son de datos de prueba.
| # | Flujo | Ids | Resultado |
|---|---|---|---|
| 1 | Corte M3 → M7 (respuesta `streetClosureApproved` y `streetClosureEnded`) | closureRequestId `44555606…`, streetClosureId `c5941b88…`; Approved `6f521edf-177f-43c4-b3ec-0a3eb1245fd5`, Ended `c8479b39-4741-49f3-a3ea-f255d16416c2` | ✅ Core: ROUTED y DELIVERED a `q.obras` y `q.ambiente` en 1 intento, sin DLQ, correlationId conservado. M3: procesó ambos, corte ENDED. Hallazgo: Ended sin causationId (DEF-08, cerrado después) |
| 2 | Corte M6 → M7 | eventId entrante `3db7aa72-e6cd-4908-a53a-97d0adacdfbc`, closureRequestId `fc780f03-898b-47e8-b374-af61cb4ab86b`, streetClosureId `91b26e25-822b-42d3-84cb-93d49e6ad1a5`; Approved `d25ff155-0b92-4f7d-b529-230f40457a8c`, Ended `af4062fb-4bde-4134-864a-60f6431086d9` | ✅ M6 confirmó ambos eventos (16:46:10 aprobado, 16:46:31 finalizado), correlationId y closureRequestId conservados, sin errores ni reintentos. Falta confirmación de Core sobre causationId (eventos previos al fix). Nota: la ventana autorizada (16:47-18:46) no fue la pedida (16:30-17:30) porque el script fija ahora+1 min a +2 h |
| 3 | `roadAccidentRegistered` M7 → M3 | eventId `9d7b06ec-dfa3-4ca6-9d28-acc17ed82523`, accidentId `c4c66355…`, dead letter `c5f8d3df-7d2c-4c50-8f6c-a224dae5c0e5` | ✅ Primero DEAD en `q.obras` (consumidor de M3 rechazaba; luego IP de M3 no habilitada en Core). Core reintentó una sola vez la dead letter conservando el eventId → RETRIED/SUCCESS. M3 (17:33): Inbox PROCESSED, 1 intento, 1 proyección, sin persistir DNI/patente/personas/inspectorId. M7 no republicó nada |
| 4 | `infractionConfirmed` M7 → M5 (y `paymentRegistered` M5 → M7) | eventId `114ab437-f36a-4f26-a565-0a3b59b2b88b`, infractionId `cb63dbb5…` | 🔄 En curso. Core: DELIVERED a `q.rentas` en 1 intento, pero M5 sin consumidores en ese momento. M5 pidió el envelope (enviado sin credenciales) y la fecha con offset (DEF-25, corregido). Falta: procesamiento por M5 y pago simulado → `paymentRegistered` |
| 5 | Reclamos M2 → M7 (`ticketUpdated` → `updateTicketStatus`) | — | ⬜ PENDIENTE: sin probar; mensaje para M2 preparado |
| 6 | Identidad M1 (adaptador REST de login) | — | ⬜ PENDIENTE: adaptador corregido (DEF-03) pero sin activar en producción (`USERS_REST_ENABLED` sin definir) |
