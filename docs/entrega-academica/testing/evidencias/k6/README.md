# Evidencia k6 sobre `main` final (con Transactional Outbox)

- **Fecha:** 15/11/2026, 22:33-22:45 UTC. **Commit probado:** `main` `b3b990f` (incluye el outbox, PR #31 y siguientes).
- **Herramienta:** imagen oficial `grafana/k6:1.3.0` (versión fija). **Objetivo:** compose local (`docker compose -p k6 up --build -d`,
  `.env` generado con `openssl rand` como en el CI, pool Hikari = 3 como producción). **Nunca producción.**
- **Comando (corrida principal):** el mismo `tests/k6/load.js` que usa `tests/run-k6.sh`, con `VUS=50 DURATION=5m`, mismos umbrales
  (`p(95)<500` global y por endpoint, `http_req_failed<1 %`). Se invocó `docker run` a mano solo para fijar la versión de la imagen
  (`run-k6.sh` usa `grafana/k6:latest`). No hubo 429: el límite de login cuenta solo **fallos**, y todos los logins fueron 200.
- **Base al inicio:** vacía (0 infracciones, 0 eventos en `outbox_events`).

## 1. Corrida principal: `load.js`, 50 VU x 5 min (exit 0, umbrales cumplidos)

| Métrica | Esta corrida (`b3b990f`) | Corrida anterior (T1, antes del outbox) |
|---|---|---|
| p95 global (`http_req_duration`) | **13,3 ms** | 341 ms |
| p95 login | 10,5 ms | 150 ms |
| p95 `GET /infractions` (listado) | **16,4 ms** (máx. 249 ms) | 472 ms |
| p95 `GET /street-closures/current` | 11,9 ms | 358 ms |
| Fallas (`http_req_failed`) | **0,00 % (0 de 44 259)** | 0,01 % (7 de 35 923) |
| Throughput | 147 req/s (14 753 iteraciones) | 119 req/s |
| Máximo | 282 ms | 2,6 s (una iteración de 31 s) |

La mejora no se puede atribuir solo al código: T1 no registró el estado de la base ni la versión de k6, y la máquina no es la misma
corrida. Lo comparable es que, con la base vacía y el relay del outbox activo compartiendo el pool de 3 conexiones, **no hay
contención ni errores** a 50 VU. `load.js` **no ejercita endpoints que publiquen eventos** (solo lecturas y login); por eso se agregó el
escenario 2. JSON: `k6-summary-50vu-5m.json`.

## 2. Complementario: alta de infracciones (publica `infractionRegistered` vía outbox), 50 VU x 2 min (exit 0)

Script nuevo `tests/k6/outbox.js` (aditivo; **no** modifica `load.js`): login una sola vez en `setup()` y token reutilizado,
`POST /api/v1/infractions` + `sleep(1)` por iteración. Mismos umbrales.

| Métrica | Valor |
|---|---|
| p95 `POST /infractions` (guarda infracción + fila del outbox en la misma transacción) | **28,5 ms** (máx. 282 ms) |
| Fallas | 0,00 % (0 de 5 951); 5 950 altas 201 |
| Eventos en `outbox_events` | 5 950; al terminar la carga: 3 050 PENDING / 2 900 PUBLISHED |
| Drenaje del outbox | todos PUBLISHED ~110 s después del fin de la carga; `attempts` máx. 1, 0 FAILED |
| Demora creación → publicación | mediana 63,7 s; p95 120 s; máx. 126 s |

Hallazgo: el relay publica como máximo `batch-size=50` cada `interval-ms=2000` (≈ 25 eventos/s); a 49 altas/s la cola PENDING crece
mientras dura la carga y se vacía después. La respuesta HTTP no se ve afectada (el envío al broker está fuera de la transacción), pero
la latencia de entrega a Core sube a ~2 min en este pico sintético (muy superior al tráfico real). JSON (sin `setup_data`, que contenía
el token local): `k6-summary-outbox-50vu-2m.json`.

## 3. Complementario: `load.js` con la base poblada (5 950 infracciones), 50 VU x 1 min (exit 99, umbrales INCUMPLIDOS)

| Métrica | Valor |
|---|---|
| p95 global | **10,1 s** |
| p95 listado `GET /infractions` | **10,4 s** (respuesta de 3,6 MB; aislado tarda ~1,9 s) |
| p95 cortes / login | 6,9 s / 14 ms |
| Fallas | **25,6 % (230 de 897)** |

Causa de las fallas (logs del backend): `SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed
out after 10000ms (total=3, active=3, idle=0, waiting=33..47)`, sin handler propio → **HTTP 500** (confirmado: 50 listados concurrentes
dieron 14 x 200 y 36 x 500). El listado **sin paginar** retiene una de las 3 conexiones ~2 s por pedido y arrastra a los demás endpoints.
Es el riesgo ya señalado en T1; con base vacía no aparece. JSON: `k6-summary-base-poblada-50vu-1m.json`.

## Conclusión
- Criterio (p95 < 500 ms, errores < 1 % con 50 VU x 5 min, script del repo): **CUMPLE** (p95 13,3 ms; peor endpoint listado 16,4 ms; 0 fallas).
- El outbox no degrada la latencia de escritura (p95 28,5 ms) ni genera errores; su throughput de relay (~25 ev/s) es el límite de entrega.
- Riesgo hallado: el listado de infracciones no escalaba (con ~6 000 filas: p95 10 s y 25 % de HTTP 500). Registrado como DEF-40 y
  corregido en el PR #55 (N+1 y open-in-view): con los mismos datos, p95 309 ms y 0 HTTP 500 en 50 VU x 5 min ([con-datos.md](con-datos.md)).
- Al terminar: `docker compose -p k6 down -v` y `.env` borrado.
