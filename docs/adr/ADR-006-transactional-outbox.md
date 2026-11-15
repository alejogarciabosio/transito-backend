# ADR-006 — Transactional Outbox para publicar eventos a Core (DEF-04 / DEF-14)

- **Estado:** aceptada (15/11/2026, PR #48).
- **Patrón:** Transactional Outbox + Polling Publisher (relay).

**Contexto.** Antes se publicaba al broker dentro del caso de uso: con RabbitMQ caído la publicación se perdía (DEF-14) y no había registro consultable de lo publicado (DEF-04). Publicar y luego hacer rollback podía emitir eventos de cambios que nunca existieron.

**Decisión.** `OutboxEventPublisher` (implementa `EventPublisher`) guarda el sobre en `outbox_events` con estado `PENDING` **en la misma transacción** de negocio. `OutboxRelay`, disparado por `OutboxRelayScheduler` cada `outbox.relay.interval-ms` (2 s), toma los `PENDING` en orden, los envía por `EventTransport` y los marca `PUBLISHED`; si falla, reintenta con backoff exponencial y tras `outbox.relay.max-attempts` queda `FAILED`. `GET /api/v1/admin/outbox` (ADMINISTRADOR/AUDITOR) lista el registro sin payload.

**Alternativas.** (a) Publicar directo con reintentos en memoria: se pierde al reiniciar. (b) Transacciones XA/2PC con el broker: no soportado por el Core y costoso. (c) CDC (Debezium): infraestructura no disponible en Railway Hobby.

**Consecuencias.** Con el broker caído la operación responde 200 y el evento sale cuando el broker vuelve (entrega al menos una vez: los consumidores deben ser idempotentes por `eventId`). Latencia de publicación de hasta ~2 s. La tabla crece; se puede purgar `PUBLISHED` antiguos.

**Evidencia.** `event/OutboxEventPublisher.java`, `event/OutboxRelay.java`, `event/OutboxRelayScheduler.java`, `model/OutboxEvent.java`, `web/OutboxAdminController.java`. Tests: `integration/OutboxIntegrationTest` (TC-BE-OUT-10..14, incluye rollback), `event/OutboxContractTest`, `event/OutboxRelayUnitTest`, `integration/HealthWithBrokerDownTest`. Prueba en vivo: README, sección "Simular broker caído y vuelve".
