# Evidencia k6 con datos: listado de infracciones antes y después de la corrección (DEF-38)

- **Fecha:** 15/11/2026, 23:00-23:20 UTC. **Base:** `main` `b3b990f` ("antes") y rama `perf/s21-listado-infracciones` ("después").
- **Entorno:** compose local `docker compose -p s21 up --build -d`, `.env` generado con `openssl rand` (solo local), pool Hikari = 3
  (igual que producción, **no se cambió**). k6 `grafana/k6:1.3.0`. **Nunca producción.** Al terminar: `docker compose -p s21 down -v`.
- **Datos:** 5 950 infracciones (cada una con 1 evidencia y 1 agravante) cargadas con `tests/k6/outbox.js` del PR #53
  (50 VU x 2 min, 0 % de fallas); respuesta del listado: 3,6 MB. La misma base (mismo volumen) se usó antes y después:
  solo se reconstruyó el contenedor del backend.
- **Script medido:** `tests/k6/load.js` sin cambios (login + `GET /api/v1/infractions` + `GET /api/v1/street-closures/current`).

## Diagnóstico (log de sentencias de PostgreSQL, `log_statement=all`, un listado aislado)

| | Antes | Después |
|---|---|---|
| Consultas por `GET /infractions` | **11 901** = 1 + 5 950 (`infraction_evidence_urls`) + 5 950 (`infraction_aggravating_factors`) | **2** (JOIN FETCH por colección) |
| Dónde se ejecutaban | después del `COMMIT` del servicio, en la vista (open-in-view retenía la conexión) | dentro de la transacción de solo lectura del servicio |
| Tiempo de un listado aislado | 2,25 s | 0,07-0,21 s |

Causa: N+1 por las dos `@ElementCollection` lazy de `Infraction`, inicializadas por el mapper en el controller gracias a
`spring.jpa.open-in-view=true` (por defecto), que además mantiene la conexión tomada durante toda la petición. Con 3 conexiones,
50 VU agotaban el pool: `Connection is not available, request timed out after 10000ms` (438 en los logs) → HTTP 500.

Paso intermedio (solo `hibernate.default_batch_fetch_size=500`): 25 consultas por listado, 0 % de fallas, pero p95 del listado
994 ms: los lotes `infraction_id = any(?)` con 500 ids usan plan genérico y recorren la tabla por lote (6 ms c/u, medido con
`EXPLAIN ANALYZE`). Por eso el listado completo usa JOIN FETCH (2,3 ms por consulta) y el batch queda para los demás listados.

## Resultados `load.js`, 50 VU

| Métrica | Antes, 1 min (exit 99) | Después, 1 min (exit 99) | Después, 5 min (**exit 0, umbrales OK**) |
|---|---|---|---|
| p95 global | **10,05 s** | 226 ms | **309 ms** |
| p95 listado `GET /infractions` | **10,56 s** (mediana 10,0 s) | 543 ms (mediana 74 ms) | **424 ms** (mediana 156 ms) |
| p95 `GET /street-closures/current` | 7,76 s | 90 ms | 100 ms |
| p95 login | 21 ms | 31 ms | 47 ms |
| Fallas | **24,6 % (218 de 885), HTTP 500 por pool agotado** | **0 % (0 de 7 758)** | **0,003 % (1 de 36 129)**: un `dial: i/o timeout` del cliente k6 (red de Docker), **0 HTTP 500**, 0 errores en el log del backend |
| Throughput | 12 req/s | 127 req/s | 120 req/s |

La corrida de 1 min "después" queda apenas por encima de 500 ms en el p95 del listado por el calentamiento de la JVM (JIT)
dentro de la ventana corta; en la corrida estándar de 5 min se cumplen todos los umbrales. Uso de CPU a mitad de la corrida de
5 min: backend ~260 %, PostgreSQL ~72 % (en el paso intermedio con lotes, PostgreSQL ~98 % con las 3 conexiones activas).

Respuesta idéntica antes y después (mismos 5 950 objetos, mismos campos y valores; comparado con un script). Contrato REST sin
cambios; no se agregó paginación (no hizo falta para el objetivo y el frontend no cambia).

JSON: `k6-summary-con-datos-antes-50vu-1m.json`, `k6-summary-con-datos-despues-50vu-1m.json`, `k6-summary-con-datos-despues-50vu-5m.json`.
