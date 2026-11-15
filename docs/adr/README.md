# Decisiones de arquitectura (ADR) — M7 Tránsito

Formato corto: contexto, decisión, alternativas, consecuencias y evidencia en código y tests.

| ADR | Decisión | Patrón(es) | Estado |
|---|---|---|---|
| [ADR-001](ADR-001-grasp-controller.md) | Controladores delgados que delegan en servicios | GRASP Controller | aceptada |
| [ADR-002](ADR-002-adapter-identidad-m1.md) | Identidad de M1 por REST detrás de `IdentityValidator`; implementación elegida por `users.rest.enabled`; fachada SOAP propia `/soap/authentication?wsdl` | **Adapter**, **Strategy**, Facade | aceptada (reescrita 15/11) |
| [ADR-003](ADR-003-observer-pubsub.md) | Integración con Core por eventos (`EventPublisher` / `EventTransport`) | Observer / Pub-Sub, **Strategy** | aceptada |
| [ADR-004](ADR-004-gateway-ia.md) | Proveedor de IA detrás de `AiGateway`, degradación a MANUAL | **Gateway/Adapter**, Circuit Breaker | aceptada |
| [ADR-005](ADR-005-state.md) | Ciclo de vida de cortes e infracciones; transición inválida → 409 | **State** | aceptada |
| [ADR-006](ADR-006-transactional-outbox.md) | Eventos guardados en la transacción y publicados por un relay | **Transactional Outbox** | aceptada |

## Patrones justificados (resumen)
- **Adapter** (ADR-002, ADR-004): aíslan a M7 de contratos ajenos (REST de M1, API de Groq). Cambiar el proveedor no toca el dominio.
- **Strategy** (ADR-002, ADR-003): la implementación se elige por configuración (`users.rest.enabled`, `core.rabbitmq.enabled`) sin `if` en el código de negocio; permite correr local/CI sin M1 ni Core.
- **State** (ADR-005): cada estado es una clase; las transiciones inválidas son 409 por construcción.
- **Transactional Outbox** (ADR-006): no se pierden eventos con el broker caído ni se publican eventos de transacciones revertidas.
- **Gateway de IA + Circuit Breaker** (ADR-004): la IA es opcional y nunca rompe el flujo (200 MANUAL).

Documentos previos: los ADR-001..003 originales están en `docs/entrega-academica/actividad-integrador-patrones-brd.md` §2 (el ADR-002 de ese documento quedó **reemplazado** por el de esta carpeta); el ADR del proveedor de IA, en `docs/ia/ADR-proveedor-ia.md`.
