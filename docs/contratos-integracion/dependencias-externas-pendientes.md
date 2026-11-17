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

## 2. Broker de eventos (RabbitMQ) — activo contra Core en producción desde el 15/11

- **Qué hay hoy:** `RabbitMqEventPublisher` publica al exchange `muni.inbox` y
  `RabbitMqIncomingEventListener` consume `q.transito`. La integración se habilita con
  `CORE_RABBITMQ_ENABLED=true`; cuando está apagada, `LoggingEventPublisher` conserva el
  comportamiento local. La cola se comprueba con `queueDeclarePassive`, sin declararla ni
  crear bindings.
- **Configuración confirmada por Core:** host `core-uade-2026.duckdns.org`, puerto `5671`,
  virtual host `core`, usuario `module_transito` y AMQPS. La validación del certificado y
  del hostname están activadas en la configuración; no se requiere certificado de cliente.
- **Estado:** AMQPS activo contra Core en producción desde el 15/11; la prueba bilateral de
  consumo y publicación se hizo con M3, M6 y M2 (ver `entrega-academica/testing/04-informe-ejecucion.md`, sección 7).
- **Registros en Core:** M7 no hace POST automáticos de publicaciones o suscripciones, por
  lo que no reactiva pausas configuradas en el dashboard. `streetClosureRequested` ya está
  suscripto con `maxAttempts: 4`. Core debe registrar/corregir la publicación
  `streetClosureEnded`: el registro histórico `streetClousureEnded` no es un alias válido.
- **M5 — Rentas (hecho, entorno de pruebas):** la prueba bilateral `infractionConfirmed` M7 → M5 →
  pago → `paymentRegistered` M5 → M7 quedó validada por el equipo en un entorno de pruebas
  (no en producción) el 17/11. El problema de fecha sin offset que había señalado M5 se
  corrigió y desplegó el 15/11 17:50 (DEF-25).

## Resumen

| Dependencia | Estado hoy | Bloqueado por | Si no se resuelve |
|---|---|---|---|
| Identidad (login) | Cliente SOAP listo; stub activo hasta habilitación | URL final y prueba con M1 | No hay login validado por Usuarios |
| Broker (RabbitMQ) | AMQPS activo contra Core en producción desde el 15/11; M5 validado en entorno de pruebas (17/11) | — | — |

Los dos puntos dependen de que los módulos externos entreguen acceso o confirmen su
configuración; no requieren cambios funcionales adicionales en M7.
