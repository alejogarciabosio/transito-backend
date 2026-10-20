# Dependencias externas pendientes — por qué ciertas cosas "no andan" sin ser bugs

> Para QA/DevOps: esto explica qué partes del backend están **simuladas a propósito** porque la pieza real de otro módulo todavía no existe o no está conectada. Si algo se comporta distinto a lo esperado en esas áreas, es un hueco de integración conocido, no un bug nuevo a reportar.

## 1. Verificación de identidad / login

- **Qué hay hoy:** `SoapIdentityValidator` implementa el contrato SOAP `validateUser` de M1, con
  solicitud `dni`/`password`, respuesta `user_id`/`dni`/`role`/`auth`, timeout de cinco segundos
  y manejo de SOAP Fault. Se activa con `USERS_SOAP_ENABLED=true` y `USERS_SOAP_URL`. Mientras
  tanto, `StubIdentityValidator` mantiene usuarios locales de desarrollo.
- **Qué falta:** M1 debe confirmar la URL final —propuesta: `/soap/users/login`— y habilitar una
  prueba con su servicio real. Al activar SOAP, el valor que hoy el login llama `username` debe
  ser un DNI, ya que M7 lo envía en el elemento SOAP `dni`.
- **Impacto visible:** hasta concretar esa prueba, el ambiente local sigue autenticando contra los
  usuarios de prueba y no contra M1.
- **Quién lo destraba:** el equipo de Usuarios (M1).

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
- **Pendiente M5 — Rentas:** confirmar que Core tiene activa la publicación
  `infractionConfirmed` para `transito` y que M5 está suscripto a ese mismo evento. El backend
  ya lo publica al confirmar una infracción; falta la prueba bilateral M7 → Core → M5.

## Resumen

| Dependencia | Estado hoy | Bloqueado por | Si no se resuelve |
|---|---|---|---|
| Identidad (login) | Cliente SOAP listo; stub activo hasta habilitación | URL final y prueba con M1 | No hay login validado por Usuarios |
| Broker (RabbitMQ) | Código y AMQPS configurados; desactivado hasta habilitación | Contraseña, allowlist de IP, registros y prueba con Core/M5 | No hay tráfico real hasta habilitarlo |

Los dos puntos dependen de que los módulos externos entreguen acceso o confirmen su
configuración; no requieren cambios funcionales adicionales en M7.
