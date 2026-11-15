# ADR-003 — Observer / Publish-Subscribe para integrarse con Core

- **Estado:** aceptada (20/10/2026). Migrada desde `docs/entrega-academica/actividad-integrador-patrones-brd.md` §2; la "evolución a outbox" quedó resuelta en [ADR-006](ADR-006-transactional-outbox.md).
- **Patrón:** Observer / Publish-Subscribe (+ Strategy en el transporte).

**Contexto.** Rentas (M5) y otros módulos deben enterarse de hechos de M7 (infracción confirmada, corte aprobado) sin bloquear la respuesta. M7 también recibe eventos de Obras, Ambiente y Rentas vía Core.

**Decisión.** Los servicios publican `EventEnvelope` por la interfaz `EventPublisher`. El envío real es una **Strategy** (`EventTransport`): `RabbitMqEventPublisher` (exchange `muni.inbox` de Core) o `LoggingEventPublisher` (local sin broker), elegida por `core.rabbitmq.enabled`. Los eventos entrantes los despacha `IncomingEventDispatcher` y se deduplican por `eventId` en `processed_events`.

**Alternativas.** HTTP síncrono a cada módulo: una caída de M5 impediría confirmar una infracción.

**Consecuencias.** Desacople temporal y de despliegue; la entrega depende del broker de Core; consumidores idempotentes y rechazo sin reencolar (`default-requeue-rejected=false`).

**Evidencia.** `event/EventPublisher.java`, `event/EventTransport.java`, `event/RabbitMqEventPublisher.java`, `event/LoggingEventPublisher.java`, `listener/IncomingEventDispatcher.java`. Tests: `listener/IncomingEventDispatcherTest`, `integration/CoreSchemaContractTest`, `integration/TicketEventListenerIntegrationTest`.
