# ADR-004 — Gateway de IA (Adapter) con degradación a carga manual

- **Estado:** aceptada (15/11/2026). Detalle completo en [docs/ia/ADR-proveedor-ia.md](../ia/ADR-proveedor-ia.md) y [docs/ia/README-ia.md](../ia/README-ia.md).
- **Patrones:** Adapter/Gateway + Circuit Breaker.

**Contexto.** Se pide una feature de IA (sugerir tipo y agravantes de infracción) barata, con timeout de 5 s, apagable y sin acoplar el dominio a un proveedor.

**Decisión.** Interfaz `AiGateway`; adaptador `GroqAiGateway` (API compatible con OpenAI, `ai.groq.url`, modelo `ai.model`, por defecto `openai/gpt-oss-20b`). `InfractionSuggestionService` valida la salida de forma estricta (enum + catálogo) y ante apagado, timeout, 5xx, salida inválida o circuito abierto responde 200 con `source: MANUAL`. La IA nunca registra ni confirma nada.

**Alternativas.** SDK propietario (acoplamiento), modelo local (sin infraestructura en Railway Hobby), no usar IA (no cumple el eje E).

**Consecuencias.** Cambiar de proveedor = otro adaptador o solo otra URL; para la demo local la URL apunta a un mock de WireMock. Los datos salen a un tercero (mitigado con `PiiMasker`).

**Evidencia.** `ai/AiGateway.java`, `ai/GroqAiGateway.java`, `ai/SimpleCircuitBreaker.java`, `service/InfractionSuggestionService.java`, `docker/wiremock/mappings/groq-chat-completions.json`. Tests: `ai/GroqAiGatewayTest`, `ai/InfractionSuggestionServiceTest`, `ai/AiGatewaySafetyTest`, `integration/InfractionSuggestionIntegrationTest`.
