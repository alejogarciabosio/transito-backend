# Validación de negocio y correcciones varias

## Qué trae este PR

Incorpora el trabajo de validación que quedaba pendiente en la rama `codex/validacion-backend-tpo` (sin fusionar hasta ahora), adaptado al estado actual de `main` — incluida la reciente reducción a 3 roles y el envelope de Core.

### Agregado

- **Bean validation en los DTOs de entrada** (`@NotBlank`, `@NotNull`, `@DecimalMin`, etc.) en infracciones, accidentes, operativos, retenciones, zonas y sesiones de estacionamiento. Antes, un campo faltante podía llegar a provocar un error 500; ahora responde `400` con el detalle de qué campo falló.
- **`InvalidRequestException`** + su manejo en `GlobalExceptionHandler` → `400 Bad Request` para violaciones de reglas de negocio que no son errores de validación de formato (ej. "falta identificar al inspector autenticado").
- **Manejo de `MethodArgumentNotValidException` / `ConstraintViolationException`** → el `400` ahora incluye un objeto `errors` con el campo y el mensaje específico por cada violación.
- **`RoadAccidentService.register(...)` ahora toma el `inspectorId` del JWT**, no del body — cierra el mismo problema de suplantación que ya se había corregido en Infracciones.
- **Validación de duplicado en `flagUnpaid`**: ya no se puede marcar la misma sesión de estacionamiento como impaga dos veces (antes duplicaba la multa).
- 7 archivos de tests nuevos (`AdditionalServiceCoverageTests`, `ApiWorkflowIntegrationTests`, `MapperCoverageTests`, `RequestValidationTests`, `SecurityIdentityTests`, `ServiceBusinessRulesTests`, `StateTransitionTests`) con cobertura de las reglas de arriba.

### Explícitamente dejado afuera (ya cubierto o en conflicto con decisiones posteriores de `main`)

- No se tocó la estructura de `affectedSections`, la integración con Core/RabbitMQ ni el vencimiento configurable de cortes de calle — esa rama vieja tenía una versión más antigua de todo eso, ya superada por los commits más recientes de `main`.
- Se descartó una función de "dueño de sesión de estacionamiento" (restringir quién puede cerrar/leer una sesión) que venía en la rama vieja: contradice una decisión ya documentada en `main` (`ParkingSession` no guarda titular a propósito, está fuera de alcance de Tránsito). Si el equipo quiere retomarla, debería discutirse como una decisión aparte, no colarse por un merge.
- Se mantuvo la baja lógica de tarifas (`status ACTIVE/INACTIVE`) en vez de la baja física que tenía `main`: es consistente con el principio ya documentado de no borrar físicamente registros con valor auditable (igual que infracciones, accidentes y operativos).

### Ajustes de compatibilidad con el `main` actual

Al traer solo el commit de validación (sin el resto de la rama), hubo que reconciliar manualmente varios puntos donde ese commit asumía un estado anterior del código:

- Roles: la rama vieja todavía usaba los 7 roles originales (`OPERADOR_CONTROL`, `PERSONAL_DEPOSITO`, `ADMIN_MUNICIPAL`, etc.); se adaptó todo al modelo actual de 3 roles.
- Se eliminó un bloque de matchers de seguridad por URL en `SecurityConfig` que quedaba redundante (y con roles viejos) frente al `@PreAuthorize` que ya tiene cada controller.
- Tipos de fecha: se mantuvo `OffsetDateTime` (normalizado a UTC) en `StreetClosure` y sus DTOs en vez de volver a `LocalDate`.
- Login: se preservó el soporte de `main` para loguearse por `username` o por `dni`; solo se agregó validación obligatoria sobre `password`.

## Verificación

163 tests corridos contra el resultado final. Las únicas 6 fallas remanentes (`InfractionIntegrationTest` x4, `ParkingIntegrationTest.start`, un error de columna nula en `StreetClosure`) **ya existían en `main` antes de este PR** — confirmado corriendo la suite de `main` de forma aislada, sin ningún cambio de este PR aplicado. No introduce fallas nuevas.
