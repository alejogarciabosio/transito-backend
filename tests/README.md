# Pruebas de API, carga y seguridad (Eje B/C/D)

Todo se ejecuta contra el **compose LOCAL**. Ninguno de estos scripts apunta a producción ni al Core real
(`run-newman.sh`, `run-k6.sh` y `run-zap.sh` rechazan hosts que no sean locales).

## Preparación (una vez)

```bash
cp .env.example .env        # completar con valores de desarrollo inventados (ver README raíz)
docker compose up --build -d
curl http://localhost:8080/actuator/health   # {"status":"UP",...}
```

Usuarios locales: `ciudadano1/ciudadano123`, `inspector1/inspector123`, `admin1/admin123`.

## 1. Pruebas de API (Postman + Newman)

```bash
tests/run-newman.sh
```

- Colección: `tests/postman/transito-api.postman_collection.json`; environment: `tests/postman/transito-local.postman_environment.json`.
- El script lee `JWT_SECRET` y `CORE_RABBITMQ_PASSWORD` de `.env` (sin imprimirlos) y los pasa como variables de Newman:
  el secreto sirve para forjar un JWT **vencido** de prueba; la clave de RabbitMQ para publicar en el broker **local**
  dos pedidos de corte de calle (los cortes no se crean por REST, llegan por evento de M3).
- Reportes: `tests/reports/newman-junit.xml` y `tests/reports/newman-report.html` (ignorados por git).
- Newman se usa instalado (`newman`) o, si no, vía `npx` (requiere Node 18+).
- Cubre los 10 endpoints de `openapi.yaml` y el resto de `/api/v1` (zonas, tarifas, sesiones, operativos, siniestros,
  retenciones, cortes): al menos un caso de éxito y uno de error por endpoint, con aserciones de status, Problem Details
  (RFC 9457) y campos clave. Carpetas transversales: BOLA/IDOR, BFLA, JWT (basura, alg=none, firma alterada, otra clave,
  vencido, control válido), mass assignment, inyección SQL/XSS, Idempotency-Key repetida, ETag/If-Match (412) y If-None-Match (304),
  cabeceras de seguridad, CORS y fuerza bruta en login (429).
- **Defectos conocidos**: los casos `[KI-n]` verifican el comportamiento *seguro*; si el backend todavía falla, el caso
  queda **skipped** con el texto "DEFECTO CONOCIDO (falla esperada)" (visible en CLI, JUnit y HTML) y el pipeline no se rompe.
  Cuando el defecto se corrige, el caso pasa a "corregido" automáticamente. Lista y análisis en `reports/t1.md`.
- Cada corrida crea datos nuevos (patentes con sufijo aleatorio) y se puede repetir sin limpiar la base.

## 2. Prueba de carga (k6)

```bash
tests/run-k6.sh                       # 50 VUs x 5 min (lo pedido)
DURATION=30s VUS=10 tests/run-k6.sh   # corrida corta de humo
```

- Script: `tests/k6/load.js`. Endpoints críticos: `POST /auth/login`, `GET /infractions` (inspector) y `GET /street-closures/current`.
- Umbrales: `http_req_duration p(95)<500 ms` (global y por endpoint) y `http_req_failed<1 %`.
- Usa la imagen `grafana/k6` (no hace falta instalar k6). Resumen versionado: `tests/reports/k6-resumen.txt`.
- Ojo: el pool de conexiones por defecto es 3 (`spring.datasource.hikari.maximum-pool-size`) y el listado no está paginado.

## 3. OWASP ZAP

```bash
tests/run-zap.sh            # baseline + escaneo activo autenticado del API (puede tardar bastante)
tests/run-zap.sh baseline   # solo baseline
tests/run-zap.sh api        # solo escaneo activo (zap-api-scan.py) con el JWT de inspector1
```

- Imagen oficial `ghcr.io/zaproxy/zaproxy:2.17.0` (fija; se puede cambiar con `ZAP_IMAGE`). El OpenAPI se toma de `http://localhost:8080/v3/api-docs` (completo) y se le
  reescribe `servers` al host local antes de escanear (el `openapi.yaml` versionado lista Railway como primer servidor).
- El token JWT viaja por la variable `ZAP_AUTH_HEADER_VALUE` (no se imprime ni se guarda).
- Reportes HTML/JSON/MD en `tests/reports/zap/` (ignorados). Resumen versionado y tratamiento propuesto: `tests/reports/zap-resumen.md`.
- El escaneo activo escribe datos basura en la base **local** y publica eventos al mock: al terminar, `docker compose down -v`.

## CI

El job `api-tests` de `.github/workflows/ci.yml` levanta el compose, espera `/actuator/health` y corre Newman; sube los reportes
como artefacto. k6 y ZAP son manuales (no corren en el pipeline para mantenerlo en ≤15 min).

## Limpieza

```bash
docker compose down -v && rm -f .env
```
