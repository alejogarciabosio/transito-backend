# Dependencias externas pendientes — por qué ciertas cosas "no andan" sin ser bugs

> Para QA/DevOps: esto explica qué partes del backend están **simuladas a propósito** porque la pieza real de otro módulo todavía no existe o no está conectada. Si algo se comporta distinto a lo esperado en esas áreas, es un hueco de integración conocido, no un bug nuevo a reportar.

## 1. Verificación de identidad / login

- **Qué hay hoy:** `src/main/java/.../auth/StubIdentityValidator.java`. Implementa la interfaz `IdentityValidator` con 7 usuarios hardcodeados en memoria (uno por rol).
- **Qué falta:** el contrato real del Módulo de Usuarios — como mínimo, si es SOAP, el WSDL; si no, la URL del servicio y el formato de request/response. Hoy no tenemos ninguno de los dos.
- **Impacto visible:** el login **siempre** valida contra esa lista fija, nunca contra una base de usuarios real. No hay manera de que un usuario nuevo se registre y pueda loguearse — solo funcionan los 7 usuarios de prueba documentados en `docs/guia-integracion-frontend.md`.
- **Quién lo destraba:** el equipo de Usuarios (o Core, si coordina esa interfaz).

## 2. Broker de eventos (RabbitMQ) — listo para conexión, pendiente de habilitación externa

- **Qué hay hoy:** `RabbitMqEventPublisher` publica al exchange `muni.inbox` y
  `RabbitMqIncomingEventListener` consume `q.transito`. La integración se habilita con
  `CORE_RABBITMQ_ENABLED=true`; cuando está apagada, `LoggingEventPublisher` conserva el
  comportamiento local. La cola se comprueba con `queueDeclarePassive`, sin declararla ni
  crear bindings.
- **Configuración confirmada por Core:** host `core-uade-2026.duckdns.org`, puerto `5671`,
  virtual host `core`, usuario `module_transito` y AMQPS. La validación del certificado y
  del hostname están activadas en la configuración; no se requiere certificado de cliente.
- **Qué falta para operar:** Core debe entregar la contraseña AMQP de forma privada y
  habilitar la IP pública de salida del backend M7 (y de cualquier red de prueba). Luego
  corresponde ejecutar la prueba bilateral de consumo y publicación.
- **Registros en Core:** M7 no hace POST automáticos de publicaciones o suscripciones, por
  lo que no reactiva pausas configuradas en el dashboard. `streetClosureRequested` ya está
  suscripto con `maxAttempts: 4`. Core debe registrar/corregir la publicación
  `streetClosureEnded`: el registro histórico `streetClousureEnded` no es un alias válido.

## 3. Discrepancia de formato con Rentas (no bloquea hoy, pero va a romper cuando se conecte el broker)

- **Qué pasa:** los ejemplos de eventos de Rentas usan campos `timestamp`/`payload`, mientras el resto del sistema espera `occurredAt`/`data` en el sobre estándar.
- **Por qué no se ve hoy:** como el punto 2 todavía no está conectado, nada deserializa eventos reales de Rentas — el problema está "dormido".
- **Riesgo:** en cuanto se conecte el broker, si Rentas efectivamente manda `timestamp`/`payload`, nuestros handlers van a fallar al deserializar o van a ignorar el evento silenciosamente.
- **Qué hace falta:** confirmar con Rentas si es un error de su documentación (como sospecha el equipo) o si es un formato intencional que tenemos que soportar aparte.

## Resumen

| Dependencia | Estado hoy | Bloqueado por | Si no se resuelve |
|---|---|---|---|
| Identidad (login) | Stub con 7 usuarios fijos | Contrato del Módulo Usuarios | No hay registro/login real |
| Broker (RabbitMQ) | Código y AMQPS configurados; desactivado hasta habilitación | Contraseña, allowlist de IP y prueba con Core | No hay tráfico real hasta habilitarlo |
| Formato de Rentas | Sin confirmar | Respuesta de Rentas | Eventos de Rentas fallarían al llegar, una vez conectado el broker |

Ninguno de estos tres puntos es arreglable solo con código de Tránsito — los tres dependen de que otro equipo entregue información o decida algo.
