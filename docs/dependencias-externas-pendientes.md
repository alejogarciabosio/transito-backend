# Dependencias externas pendientes — por qué ciertas cosas "no andan" sin ser bugs

> Para QA/DevOps: esto explica qué partes del backend están **simuladas a propósito** porque la pieza real de otro módulo todavía no existe o no está conectada. Si algo se comporta distinto a lo esperado en esas áreas, es un hueco de integración conocido, no un bug nuevo a reportar.

## 1. Verificación de identidad / login

- **Qué hay hoy:** `src/main/java/.../auth/StubIdentityValidator.java`. Implementa la interfaz `IdentityValidator` con 7 usuarios hardcodeados en memoria (uno por rol).
- **Qué falta:** el contrato real del Módulo de Usuarios — como mínimo, si es SOAP, el WSDL; si no, la URL del servicio y el formato de request/response. Hoy no tenemos ninguno de los dos.
- **Impacto visible:** el login **siempre** valida contra esa lista fija, nunca contra una base de usuarios real. No hay manera de que un usuario nuevo se registre y pueda loguearse — solo funcionan los 7 usuarios de prueba documentados en `docs/guia-integracion-frontend.md`.
- **Quién lo destraba:** el equipo de Usuarios (o Core, si coordina esa interfaz).

## 2. Broker de eventos (RabbitMQ) — ninguna integración está conectada

- **Qué hay hoy:** `event/LoggingEventPublisher.java` (único implementador de `EventPublisher`) solo hace `log.info(...)`, no publica a ningún broker real. `listener/IncomingEventDispatcher.java` existe y funciona correctamente, pero **solo se invoca desde tests** — no hay ningún `@RabbitListener` ni conexión real a una cola.
- **Qué falta:** definición de la cola (ej. `q.transito`), URL y credenciales del broker. Depende de que Core (Módulo 9) las entregue — está documentado como pendiente en `docs/arquitectura-backend-guia.md`.
- **Impacto visible:** ningún evento entrante se procesa solo. Los handlers (`StreetClosureRequestedHandler`, `TicketUpdatedHandler`, etc.) están completos y testeados, pero en producción **no les llega nada** hasta que haya un broker real conectado. Tampoco salen eventos hacia Rentas, Obras, Ambiente ni Atención Ciudadana — se loguean y nada más.
- **Confirmado además:** no existe ningún otro cliente HTTP (ni `WebClient`, ni `RestTemplate`, ni `@FeignClient`) hacia Rentas o Atención Ciudadana en todo el código. Toda la comunicación prevista con esos módulos depende exclusivamente de este broker — no hay un "plan B" simulado para ellos.

## 3. Discrepancia de formato con Rentas (no bloquea hoy, pero va a romper cuando se conecte el broker)

- **Qué pasa:** los ejemplos de eventos de Rentas usan campos `timestamp`/`payload`, mientras el resto del sistema espera `occurredAt`/`data` en el sobre estándar.
- **Por qué no se ve hoy:** como el punto 2 todavía no está conectado, nada deserializa eventos reales de Rentas — el problema está "dormido".
- **Riesgo:** en cuanto se conecte el broker, si Rentas efectivamente manda `timestamp`/`payload`, nuestros handlers van a fallar al deserializar o van a ignorar el evento silenciosamente.
- **Qué hace falta:** confirmar con Rentas si es un error de su documentación (como sospecha el equipo) o si es un formato intencional que tenemos que soportar aparte.

## Resumen

| Dependencia | Estado hoy | Bloqueado por | Si no se resuelve |
|---|---|---|---|
| Identidad (login) | Stub con 7 usuarios fijos | Contrato del Módulo Usuarios | No hay registro/login real |
| Broker (RabbitMQ) | Nada conectado, solo logs | Config de Core (Módulo 9) | Ningún evento entra ni sale en producción |
| Formato de Rentas | Sin confirmar | Respuesta de Rentas | Eventos de Rentas fallarían al llegar, una vez conectado el broker |

Ninguno de estos tres puntos es arreglable solo con código de Tránsito — los tres dependen de que otro equipo entregue información o decida algo.
