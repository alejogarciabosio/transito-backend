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
| API activo autenticado | **0** | 1 (10) | 2 (7) | 3 (241) |

**Criterio del profesor (sin alertas High): CUMPLE.**

## Detalle y tratamiento

| Riesgo | Alerta (plugin) | Dónde | Diagnóstico | Tratamiento |
|---|---|---|---|---|
| Medium | Buffer Overflow (30001) | POST `/api/v1/accidents` (7 campos), POST `/api/v1/operations` (3) | No es un desbordamiento real (JVM): con cadenas largas la base corta con `value too long for type character varying(255)` y el backend responde 500 con `Connection: close`. | Defecto de validación: faltan `@Size(max=255)` en esos DTOs. Documentado, no corregido (cambia validación de DTOs congelados: 500 → 400). |
| Low | Server Error response (100000) | mismos endpoints | Mismo origen + byte NUL (`invalid byte sequence for encoding "UTF8": 0x00`). | Igual al anterior; idem handler de `DataIntegrityViolationException` → 400. |
| Low | Cross-Origin-Resource-Policy missing (90004) | GET de listados | Falta la cabecera CORP. Riesgo bajo (API JSON con JWT, CORS restringido). | Opcional: agregar `Cross-Origin-Resource-Policy: same-site` en `SecurityConfig`. No hecho. |
| Info | Client Error 4xx, Authentication Request Identified, Non-Storable Content | varios | Comportamiento esperado (401/403/404/400, login, `no-store`). | Ninguno. |

Cabeceras ya presentes: `X-Content-Type-Options`, `X-Frame-Options: DENY`, CSP, `Referrer-Policy`, `Cache-Control: no-store`.

## Cambio de configuración asociado
`/v3/api-docs` y Swagger UI quedan **deshabilitados con el perfil `prod`** (`application-prod.properties`).
Solo tiene efecto si Railway (producción) tiene `SPRING_PROFILES_ACTIVE=prod`; hoy no se sabe qué valor tiene: lo debe
confirmar el humano. `/openapi.yaml` (contrato curado) sigue público. El frontend no usa estos endpoints (grep sin resultados).
