# Feature de IA — Sugerencia de tipo y agravantes de infracción

## 1. Ficha de la feature
| Campo | Detalle |
|---|---|
| Propósito | El inspector escribe un texto libre y el sistema sugiere `infractionType` y agravantes. Ahorra tiempo de carga; no decide. |
| Endpoint | `POST /api/v1/infractions/suggestions` (solo rol INSPECTOR). Body `{"description": "1..500 caracteres"}` |
| Respuesta | `{"infractionType": <enum|null>, "aggravatingFactors": [...], "source": "AI"|"MANUAL", "promptVersion": "v1"|null}` |
| Proveedor / modelo | Groq (API compatible con OpenAI). Modelo por `AI_MODEL` (por defecto `openai/gpt-oss-20b`, el mismo que usa producción; `llama-3.1-8b-instant` fue retirado por Groq; ver ADR). |
| Prompt | `src/main/resources/prompts/infraction-suggestion.v1.md` (versionado en el repo, temperatura 0, 3 ejemplos + 1 de rechazo). |
| Catálogo de agravantes | `REINCIDENCIA`, `ZONA_ESCOLAR`, `ALTA_VELOCIDAD`, `RESISTENCIA` (mismo que `AGGRAVATING_FACTORS` del frontend). Cualquier otro valor se descarta. |
| Datos que se envían | Solo la descripción (≤500 caracteres) con datos personales enmascarados (`[DOC]`, `[PATENTE]`, `[EMAIL]`, `[TEL]`) y las instrucciones del prompt. No se envían usuario, token, ids ni datos de la base. |
| Límites | Timeout total 5 s (`AI_TIMEOUT_MS`), 1 reintento máximo, circuito que abre tras 3 fallas consecutivas y se prueba de nuevo a los 30 s. Si el modelo devuelve `infractionType: null` (no pudo clasificar) se responde MANUAL. |

Variables de entorno nuevas (ninguna con la clave en el repo): `AI_ENABLED` (default `false`), `GROQ_API_KEY` (sin default), `AI_MODEL`, `AI_TIMEOUT_MS` (default 5000).

## 2. Política de datos (Ley 25.326)
- Minimización: solo viaja el texto libre; DNI/CUIT de 7–11 dígitos, patentes, emails y teléfonos se enmascaran **antes** de salir (`PiiMasker`). El enmascarado es heurístico: puede dejar pasar nombres propios; conviene que la UI avise al inspector que no escriba nombres.
- No se loguea el texto del usuario, la respuesta cruda ni la clave. Solo: versión del prompt, latencia, tokens, `source` y motivo resumido (`ok`, `disabled`, `circuit_open`, `invalid_output`, `provider_error`).
- No se persiste nada de la sugerencia en la base. Revisar con el proveedor su política de retención antes de producción.
- La clave la carga una persona como variable de entorno en Railway; nunca en archivos versionados.

## 3. Humano en el circuito
La IA solo propone valores en el formulario. Registrar, confirmar o anular una infracción sigue siendo una acción explícita de una persona por los endpoints existentes. El endpoint de sugerencias no escribe nada, no publica eventos y no tiene efectos.

## 4. Plan de apagado
1. Poner `AI_ENABLED=false` (o quitar `GROQ_API_KEY`) y reiniciar el servicio: el endpoint responde `source: MANUAL` sin llamar a nadie; el resto del sistema no cambia.
2. Falla del proveedor/latencia: el circuito se abre solo y la UI cae al modo manual. Nunca se devuelve 5xx por el proveedor.
3. Incidente de datos: apagar, rotar la clave en Groq y revisar logs (no contienen texto del usuario).

## 5. Riesgos OWASP LLM Top 10 y mitigaciones
| Riesgo | Mitigación implementada |
|---|---|
| LLM01 Prompt injection (directa e indirecta) | El texto va delimitado como `<DATO>` (se eliminan `<` y `>` del dato para que no cierre el bloque); el prompt lo declara dato; la salida se valida con parseo estricto (solo 2 claves, enum y catálogo) y cualquier desvío → MANUAL; la IA no tiene herramientas ni acciones. |
| LLM02 Revelación de información sensible | PII enmascarada de entrada; el prompt no contiene secretos ni datos reales; la clave no está en el prompt; salida no se loguea; el contrato de respuesta no admite texto libre. |
| LLM05 Manejo inseguro de la salida | La salida se trata como input no confiable: JSON estricto, `InfractionType.valueOf`, agravantes ∈ catálogo, sin campos extra, sin texto libre hacia el cliente. |
| (extra) LLM10 Consumo no acotado | Límite de 500 caracteres, `max_tokens` 300, timeout 5 s, circuito, solo rol INSPECTOR autenticado. |

## 6. Métricas y umbrales (eval set: `docs/ia/eval-set.json`, 20 casos, 8 adversariales)
Se corre a mano con `scripts/ai-eval.sh` (requiere backend con `AI_ENABLED=true`, clave del humano y `INSPECTOR_TOKEN`; no apuntar a producción).
| Métrica | Umbral |
|---|---|
| Respuestas que cumplen el contrato | 100 % |
| Casos adversariales sin fuga ni salida fuera de catálogo | 100 % |
| Acierto de `infractionType` en casos normales | ≥ 80 % |
| Latencia p95 de la sugerencia | < 5 s (si no, MANUAL) |
| Tasa de MANUAL por falla del proveedor (en operación) | < 5 %; si se supera, revisar o apagar |
| Aceptación por inspectores (sugerencia aceptada sin cambios) | informativa; seguir en la demo |

Los tests JUnit cubren sin red (gateway simulado) contrato, validación, inyección, PII, timeout y circuito. La corrida del eval set contra Groq real queda pendiente de la clave (no se ejecutó).

## 7. Tests JUnit por riesgo
| Riesgo | Tests |
|---|---|
| Inyección de prompt (salida fuera del enum se descarta; instrucciones del dato se ignoran) | `InfractionSuggestionServiceTest.promptInjectionCannotChangeContract`, `InfractionSuggestionIntegrationTest.promptInjection`, `AiGatewaySafetyTest.promptInjectionOutputOutsideEnumIsDiscarded` |
| Pedido fuera de dominio (no es de tránsito → sin sugerencia) | `AiGatewaySafetyTest.outOfDomainGivesNoSuggestion` |
| Fuga de datos (el prompt no lleva DNI/patente/datos del token; la respuesta no se loguea) | `InfractionSuggestionServiceTest.outgoingDataIsSanitized`, `InfractionSuggestionIntegrationTest.piiMasked`, `PiiMaskerTest`, `AiGatewaySafetyTest.promptCarriesNoPiiNorTokenData`, `AiGatewaySafetyTest.providerResponseIsNeverLogged` |
| Caída del proveedor (timeout / 5xx → 200 `source: MANUAL`) | `GroqAiGatewayTest.timeout`, `GroqAiGatewayTest.failsAfterOneRetry`, `InfractionSuggestionServiceTest.providerFailure`, `InfractionSuggestionIntegrationTest.degradesToManual`, `AiGatewaySafetyTest.providerDownDegradesToManual` |

`AiGatewaySafetyTest` usa el `GroqAiGateway` real contra un servidor HTTP local que imita `chat/completions` (sin red externa).

## 8. Demo local con el mock del proveedor (sin clave real)
El contenedor `mocks` (WireMock 3.9.1, versión fija) incluye `docker/wiremock/mappings/groq-chat-completions.json`, que
responde como Groq: `POST /openai/v1/chat/completions` → `{"choices":[{"message":{"content":"{\"infractionType\":\"ILLEGAL_PARKING\",\"aggravatingFactors\":[\"ZONA_ESCOLAR\"]}"},"finish_reason":"stop"}]}`.
El compose local deja `AI_GROQ_URL=http://mocks:8080/openai/v1/chat/completions` (propiedad `ai.groq.url`); la IA sigue
apagada salvo que se pida. Para encenderla contra el mock, agregar al `.env`:

```bash
AI_ENABLED=true
GROQ_API_KEY=clave-ficticia-mock   # cualquier texto; el mock no la valida
```

y `docker compose up -d backend`. Luego, con un token de `inspector1`:

```bash
curl -s -H "Authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -d '{"description":"Auto en doble fila frente a la escuela"}' http://localhost:8080/api/v1/infractions/suggestions
# {"infractionType":"ILLEGAL_PARKING","aggravatingFactors":["ZONA_ESCOLAR"],"source":"AI","promptVersion":"v1"}
```

El default de producción no cambia (`ai.groq.url` apunta a `https://api.groq.com/openai/v1/chat/completions` y
`AI_ENABLED` es `false` si no se define). Para probar la caída del proveedor: `docker compose stop mocks` → la misma
llamada responde 200 con `source: MANUAL`.
