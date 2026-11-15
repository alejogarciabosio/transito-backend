# Contraseñas de los usuarios de prueba (DEF-01)

Con `USERS_REST_ENABLED` sin definir o en `false` el login usa `StubIdentityValidator`. Sus contraseñas
**no están en el código ni en `application.properties`**: se leen de variables de entorno, sin valor por defecto.

| Usuario | Rol | Variable de entorno (propiedad Spring) |
|---|---|---|
| `ciudadano1` | CIUDADANO | `STUB_PASSWORD_CIUDADANO1` (`stub.password.ciudadano1`) |
| `inspector1` | INSPECTOR | `STUB_PASSWORD_INSPECTOR1` (`stub.password.inspector1`) |
| `admin1` | ADMINISTRADOR | `STUB_PASSWORD_ADMIN1` (`stub.password.admin1`) |

- **Fail-closed:** si una variable falta o está vacía, ese usuario queda deshabilitado (login 401). La app arranca
  igual y deja un `WARN` con el **nombre** de la variable faltante (nunca el valor).
- La comparación es de tiempo constante (`MessageDigest.isEqual`).

## Producción (Railway)
En el servicio del backend → *Variables*, cargar `STUB_PASSWORD_CIUDADANO1`, `STUB_PASSWORD_INSPECTOR1` y
`STUB_PASSWORD_ADMIN1`. Hacerlo **antes** de desplegar este cambio; si no, esos usuarios dejan de poder ingresar.
Los valores de producción no se escriben en el repo, en issues ni en PRs.

## Local y CI
- Local: `cp .env.example .env` (trae valores de desarrollo, distintos a los de producción); el compose los pasa al backend.
- CI (`api-tests`): genera valores aleatorios en el `.env` del runner; `tests/run-newman.sh`, `run-k6.sh` y `run-zap.sh` los leen del `.env` sin imprimirlos.
- Tests Java: `src/test/resources/application.properties` (`stub.password.*` con valores de prueba).
