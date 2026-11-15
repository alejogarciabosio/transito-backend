# ADR-001 — GRASP Controller para los casos de uso REST

- **Estado:** aceptada (20/10/2026). Migrada desde `docs/entrega-academica/actividad-integrador-patrones-brd.md` §2.
- **Patrón:** GRASP Controller.

**Contexto.** La API recibe HTTP, controla permisos y aplica reglas de negocio; mezclar transporte y dominio duplica reglas y dificulta los tests.

**Decisión.** Los controladores (`web/*Controller`) reciben DTOs, aplican `@PreAuthorize`, delegan en un servicio (`service/*Service`) y devuelven la respuesta. Ej.: `StreetClosureController` delega aprobar, rechazar, activar y finalizar en `StreetClosureService`, que también usa el scheduler de vencimiento.

**Alternativas.** Cambiar estados, persistir y publicar eventos dentro de cada controlador: descartada por duplicar reglas y acoplar HTTP al dominio.

**Consecuencias.** Endpoints delgados; las reglas se reutilizan desde REST, eventos entrantes y tareas programadas. Más clases a cambio de menor acoplamiento.

**Evidencia.** `web/StreetClosureController.java`, `service/StreetClosureService.java`, `service/StreetClosureExpirationScheduler.java`; tests `integration/StreetClosureIntegrationTest`, `ServiceBusinessRulesTests`.
