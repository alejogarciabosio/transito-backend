# Cambios de contrato para el Frontend — octubre 2026

> Qué cambió desde la última integración y qué hay que tocar en el frontend para que siga funcionando. El contrato completo y actualizado está en `docs/actividad-api-rest-entrega.md` — este documento es solo el **delta**.

## 1. Todas las URLs ahora llevan el prefijo `/api/v1`

Antes: `POST http://localhost:8080/auth/login`
Ahora: `POST http://localhost:8080/api/v1/auth/login`

**Esto aplica a absolutamente todos los endpoints**, sin excepción (incluidos los públicos como `parking-zones`). El único que **no** lleva prefijo es `GET /actuator/health`.

Acción: si el frontend tiene una base URL configurada en un solo lugar (ej. `VITE_API_URL` o similar), agregarle `/api/v1` ahí resuelve todo de una.

## 2. Varios sub-recursos cambiaron de nombre (verbo → sustantivo)

| Antes | Ahora |
|---|---|
| `POST /operations/{id}/close` | `POST /operations/{id}/closure` |
| `POST /infractions/{id}/confirm` | `POST /infractions/{id}/confirmation` |
| `POST /infractions/{id}/annul` | `POST /infractions/{id}/annulment` |
| `POST /street-closures/{id}/approve` | `POST /street-closures/{id}/approval` |
| `POST /street-closures/{id}/reject` | `POST /street-closures/{id}/rejection` |
| `POST /street-closures/{id}/activate` | `POST /street-closures/{id}/activation` |
| `POST /street-closures/{id}/finish` | `POST /street-closures/{id}/completion` |
| `PATCH /parking-sessions/{id}/close` | **`POST`** `/parking-sessions/{id}/closure` (cambió también el método HTTP) |
| `POST /parking-sessions/{id}/flag-unpaid` | `POST /parking-sessions/{id}/unpaid-flag` |

El resto de los endpoints (`/infractions`, `/accidents`, `/impoundments`, `/parking-zones`, `/parking-tariffs`, etc.) **no cambiaron de nombre**, solo ganaron el prefijo `/api/v1`.

## 3. Formato de error cambió completo (Problem Details, RFC 9457)

**Antes:**
```json
{
  "timestamp": "2026-09-18T19:24:37Z",
  "status": 404,
  "error": "Not Found",
  "message": "Infraction no encontrada: <uuid>"
}
```

**Ahora:**
```json
{
  "type": "https://transito.uade.edu.ar/problems/invalid-state-transition",
  "title": "Conflict",
  "status": 409,
  "detail": "No se puede confirmar una infracción en estado ANNULLED",
  "instance": "/api/v1/infractions/93fd4382-2343-4f3f-a127-9328b29fc520/confirmation"
}
```

El `Content-Type` de las respuestas de error también cambió a `application/problem+json`.

Acción: en el frontend, donde se lea `error.message` para mostrar el mensaje al usuario, cambiar a `error.detail`. Los campos `timestamp` y `error` ya no existen — no confiar en ellos. Hay un campo nuevo, `type`, que es una URL identificando el tipo de problema (no hace falta usarlo todavía, pero está disponible si quieren diferenciar casos sin parsear el texto de `detail`).

## 4. Separación 401 vs 403

Antes, **cualquier** problema de autenticación o autorización devolvía `403`. Ahora están separados:

- **`401 Unauthorized`**: no mandaste token, el token es inválido/está mal firmado, falta el prefijo `Bearer `, o el token no tiene un rol reconocible.
- **`403 Forbidden`**: el token es válido, pero el rol del usuario no alcanza para ese endpoint (ej. un `CIUDADANO` pegándole a `GET /infractions`).

Acción: si el frontend hoy interpreta "cualquier 403 → mandar al login de nuevo", conviene separar: un `401` sí amerita volver a loguear (sesión vencida o inválida); un `403` es "no tenés permiso", y ahí lo correcto es mostrar un mensaje, no reloguear.

## 5. Dos campos que el frontend ya no debería mandar (si los mandaba)

- `POST /infractions` y `POST /accidents`: el campo `inspectorId` se ignora si se manda — el backend lo toma del JWT del usuario logueado. Si el frontend lo estaba completando en el formulario, puede sacarlo.
- `POST /infractions/{id}/annulment`: el campo `authorizedBy` también se ignora — se toma del supervisor autenticado. Solo hace falta mandar `{ annulmentReason }`.

## 6. Documentación interactiva nueva

Con el backend levantado, ahora hay Swagger UI para probar los endpoints a mano:

- `http://localhost:8080/swagger-ui/index.html`
- Contrato estático: `http://localhost:8080/openapi.yaml`

## 7. Lo que NO cambió

- El formato de login (`POST /api/v1/auth/login`, mismo body, misma respuesta `{token, role, expiresInSeconds}`).
- CORS: sigue limitado a `http://localhost:5173` en local.
- Los enums (roles, estados, tipos) — ver la tabla completa en `docs/guia-integracion-frontend.md` §4, siguen vigentes.
- Los roles requeridos por cada endpoint.

## Resumen de lo mínimo para no romper nada

1. Prefijo `/api/v1` en la base URL.
2. Renombrar las 9 llamadas de la tabla del punto 2 (y el método de `parking-sessions/.../closure` a `POST`).
3. Cambiar `error.message` por `error.detail` en el manejo de errores.
4. Si hay lógica que trata 401 y 403 igual, separarla.
