# Evidencia OWASP ZAP (compose local)

- **Fecha:** 15/11/2026. **Herramienta:** imagen oficial `ghcr.io/zaproxy/zaproxy:2.17.0` (versión fija).
- **Objetivo:** backend del `docker-compose.yml` local (`http://host.docker.internal:8080`). Nunca producción.
- **Comando:** `tests/run-zap.sh todo` (≈ 1 min 15 s). Baseline pasivo sin autenticar + `zap-api-scan.py` (activo)
  con el OpenAPI de `/v3/api-docs` (34 rutas, `servers` reescrito al host local) y JWT de `inspector1` en `Authorization`.
- **Archivos:** `zap-baseline.html/json`, `zap-api.html/json` (sin tokens: verificado con grep `eyJ`).

## Conteo por riesgo (tipos de alerta; instancias entre paréntesis)

| Escaneo | High | Medium | Low | Informational |
|---|---|---|---|---|
| Baseline (pasivo) | **0** | 0 | 0 | 1 (3) |
| API activo autenticado, 1.ª corrida (antes de la corrección) | **0** | 1 (10) | 2 (7) | 3 (241) |
| API activo autenticado, re-escaneo tras la corrección | **0** | **0** | 1 (5) | 3 (241) |

**Criterio del profesor (sin alertas High): CUMPLE.** `zap-api.html/json` corresponden al re-escaneo (15/11 20:37 UTC).

## Detalle y tratamiento

| Riesgo | Alerta (plugin) | Diagnóstico | Tratamiento | Estado |
|---|---|---|---|---|
| Medium | Buffer Overflow (30001), POST `/accidents` (7 campos) y `/operations` (3) | No era un desbordamiento (JVM): con textos >255 la base cortaba (`varchar(255)`) y se respondía 500. | `@Size(max = 255)` (largo real de columna, Hibernate por defecto) en todos los String de los DTOs de request y embebidos (`Location`, `AccidentVehicle`, `AccidentPerson`, `Detour`) que se persisten. Responde 400 `validation-error`. | **Corregido** |
| Low | Server Error response (100000), mismos endpoints | Mismo origen + byte NUL (`invalid byte sequence for encoding "UTF8": 0x00`). | `RejectNulCharactersWebConfig`: el conversor JSON de lectura de Spring MVC rechaza `\u0000` en cualquier String → 400 Problem Details (`invalid-request`). No afecta al ObjectMapper de los listeners RabbitMQ ni a las respuestas. | **Corregido** |
| Low | Cross-Origin-Resource-Policy missing (90004), GET de listados | Falta la cabecera CORP. Riesgo bajo (API JSON con JWT, CORS restringido). | Opcional: `Cross-Origin-Resource-Policy: same-site` en `SecurityConfig`. | Abierto (bajo) |
| Info | Client Error 4xx, Authentication Request Identified, Non-Storable Content | Comportamiento esperado (401/403/404/400, login, `no-store`). | Ninguno. | — |

Tests: `InputLimitsValidationTests` (256 caracteres → 400 en accidents/operations, 255 → 201, NUL → 400 también en login).
Cabeceras ya presentes: `X-Content-Type-Options`, `X-Frame-Options: DENY`, CSP, `Referrer-Policy`, `Cache-Control: no-store`.

## Riesgo aceptado: Swagger y `/v3/api-docs` públicos en producción
Documentación de API pública intencionalmente para la integración entre módulos. En Railway `SPRING_PROFILES_ACTIVE=dev`
y no se cambia (otros módulos pueden usar el Swagger de producción). `application-prod.properties` (apaga springdoc con el
perfil `prod`) queda en el repo **sin efecto hoy**, por si en el futuro se decide activar ese perfil. El frontend no usa
estos endpoints (grep en `transito-frontend`: sin resultados).
