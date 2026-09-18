# Guía de arquitectura — Backend Tránsito

> Documento de referencia para el equipo, a nivel general (no explica cada controller línea por línea). Pensado para entender **para qué sirve cada pieza** del proyecto, sobre todo las que no se tocan directamente desde `service`/`repository`/`controller` (DTOs, mappers, eventos, seguridad).

---

## 1. Las 3 capas de siempre

`model` (entidad JPA) → `repository` (Spring Data JPA) → `service` (reglas de negocio) → `controller` (REST). Esto ya lo conocen — sigue el mismo patrón en las 8 entidades del dominio (`TrafficOperation`, `Infraction`, `RoadAccident`, `VehicleImpoundment`, `StreetClosure`, `ParkingZone`, `ParkingTariff`, `ParkingSession`).

---

## 2. DTOs (`web/dto/`)

Un DTO (*Data Transfer Object*) es simplemente una clase que define **qué datos entran o salen por la API** — separada de la entidad que se guarda en la base.

**¿Por qué no usar la entidad JPA directamente en el controller?** Porque la entidad tiene campos que el cliente no debería poder mandar (por ejemplo, `status` de una infracción — eso lo decide el negocio, no quien hace el `POST`) ni todos los que debería ver (dos personas con roles distintos podrían necesitar ver cosas distintas de un mismo registro). Por eso cada entidad tiene:
- Uno o más `Create*Request`/`Update*Request` (lo que el cliente puede mandar).
- Un `*Response` (lo que el cliente recibe de vuelta).

Hoy usamos **un solo `Response` por entidad** (no uno distinto por rol) — fue una decisión consciente para no multiplicar el trabajo, ver sección 7.

---

## 3. Mappers (`web/mapper/`) — la pieza que preguntaste

Un mapper es un traductor: convierte un DTO en una entidad, y una entidad en un DTO de respuesta. Nada más que eso — no tiene lógica de negocio, no valida nada, no decide nada.

**¿Por qué existen si no se "usan" desde ningún lado que uno note?** Porque en cuanto separás DTO de entidad (punto 2), *alguien* tiene que hacer la conversión campo por campo entre una cosa y la otra. Sin el mapper, esa conversión terminaría escrita a mano y repetida dentro de cada método de cada controller — con el mapper, esa traducción vive en **un solo lugar** por entidad. El controller los llama así:

```java
// DTO que llega del cliente → entidad que entiende el service
Infraction entidad = InfractionMapper.toEntity(request);
Infraction guardada = infractionService.register(entidad);
// entidad que devolvió el service → DTO que se manda de vuelta
return InfractionMapper.toResponse(guardada);
```

Si mañana un DTO cambia de forma, solo hay que tocar el mapper de esa entidad — no cada controller que la use.

---

## 4. Sistema de eventos (`event/`)

Esto modela la comunicación con **otros módulos** del TP (Rentas, Obras, Ambiente, Atención Ciudadana), aunque hoy no hay ningún broker real conectado (Kafka/RabbitMQ sigue sin definir, ver `CLAUDE.md`).

- **`EventEnvelope<T>`**: el "sobre" común que exige la consigna del TPO (`eventId`, `eventType`, `occurredAt`, `sourceModule`, `data`). Envuelve cualquier payload.
- **`EventPublisher`** (interfaz) + **`LoggingEventPublisher`** (única implementación hoy): en vez de que cada `service` sepa *cómo* mandar un evento a un broker real, solo le dice a `EventPublisher` "publicá esto". Hoy esa interfaz solo lo loguea a consola. El día que el equipo defina Kafka/RabbitMQ, se escribe una implementación nueva de `EventPublisher` y ningún `service` se toca.
- **`event/payload/`**: una clase (`record`) por cada tipo de evento (`InfractionRegisteredPayload`, `VehicleReleasedPayload`, etc.) — es el contenido (`data`) del sobre, ya alineado con el catálogo de payloads que definieron con el resto de los módulos.

**Publicar vs. escuchar — dos mitades distintas.** Todo lo de arriba es el lado "publicar" (salida): el `service` le avisa a `EventPublisher` que algo pasó. El lado "escuchar" (entrada) es otra cosa — es el que recibe eventos de **otros** módulos (`ticketUpdated`, `streetClosureRequested`, `paymentRegistered`, etc.) y ejecuta la acción correspondiente en Tránsito. Ese lado hoy **no existe conectado a nada real**, pero sí hay un esqueleto de referencia en `listener/` (`TicketEventListener`, `StreetClosureEventListener`) — muestra cómo se vería un listener en este proyecto, con `TODO`s explícitos marcando que ningún broker los invoca todavía. Sirve para discutir la forma, no para usar en producción tal cual.

---

## 5. Seguridad (`auth/`)

Resumen rápido de qué hace cada archivo, para saber cuál tocar si hace falta:

| Archivo | Qué hace |
|---|---|
| `JwtService` | Firma y valida el token (JWT propio de Tránsito). |
| `JwtAuthenticationFilter` | Se ejecuta en cada request: lee el header `Authorization`, valida el token, carga el usuario/rol en el contexto de Spring Security. |
| `SecurityConfig` | Define qué endpoints son públicos y cuáles exigen estar logueado; conecta el filtro anterior. |
| `CurrentUserProvider` | Devuelve el usuario autenticado actual (id, DNI, rol) — lo usan los controllers para saber "quién está pidiendo esto" (ej. `releasedBy`, `flagUnpaid`). |
| `IdentityValidator` / `StubIdentityValidator` | Valida usuario/contraseña. Hoy es un **stub** con usuarios de prueba hardcodeados — ver sección 7. |
| `Role` (enum) | Los 7 roles del sistema, usados en los `@PreAuthorize` de cada endpoint. |

---

## 6. Manejo de errores (`GlobalExceptionHandler` + `exception/`)

En vez de que cada controller decida a mano qué código HTTP devolver ante un error, hay excepciones propias (`ResourceNotFoundException`, `InvalidStateTransitionException`, `InvalidCredentialsException`) que cualquier `service` puede lanzar, y un único `@RestControllerAdvice` (`GlobalExceptionHandler`) que las traduce a la respuesta HTTP correcta (404, 409, 401, 403) con un formato de error consistente (`ErrorResponse`). Así el `service` solo se preocupa de *qué* salió mal, no de *cómo* se lo comunica al cliente.

---

## 7. Qué falta definir para un backend más sólido

Lista consolidada de todo lo que quedó pendiente durante la construcción — para que el equipo la revise y priorice:

### Modelo de datos / reglas de negocio
- **Catálogo de `InfractionType`** — hoy es un `enum` de Java fijo, pero lo maneja Módulo 2 y puede crecer sin avisar (riesgo de tener que redeployar por cada categoría nueva).
- **`TrafficOperation.close()` no crea automáticamente la `Infraction`/`RoadAccident` resultante** — hoy son 2 llamadas separadas del cliente (cerrar el operativo, y después crear la infracción/accidente a mano), no un flujo encadenado.
- **Baja de `ParkingTariff` simplificada** — solo chequea si hay sesiones sin cerrar en la zona, no cruza contra el rango de vigencia de la tarifa.
- **`flagUnpaid` requiere `baseAmount` manual** — no hay ninguna regla de cálculo automático de monto para infracciones de estacionamiento.

### Seguridad
- **Ownership sin validar en `ParkingSession`** — `GET /parking-sessions/{id}` no puede confirmar que el ciudadano sea el "dueño" de la sesión porque la entidad no guarda ningún dato del titular, solo la patente. Necesitaría un registro de propiedad de vehículos que hoy no existe (y no está claro si es responsabilidad de Tránsito tenerlo).
- **`StubIdentityValidator` es temporal** — usuarios hardcodeados en memoria. Falta el contrato real con el módulo de Usuarios para validar credenciales.
- **Roles vs. "dueño del recurso"** — hoy la seguridad es por rol (`@PreAuthorize`), y las 2 validaciones de "dueño" que sí están hechas (`Infraction`, `VehicleImpoundment`) se resolvieron a mano en cada controller — no es un mecanismo genérico, si aparecen más casos hay que repetir el patrón.

### Eventos / integración con otros módulos
- **No hay broker definido** (Kafka/RabbitMQ) — depende del equipo Core (Módulo 9). Bloquea *consumir* cualquier evento externo (`streetClosureRequested`, `paymentRegistered`, `debtSettled`, `paymentReversed`, `ticketUpdated`, y los "avisos anticipados" de Obras/Ambiente). Los contratos ya están documentados, falta la infraestructura.
- **Formato de sobre distinto para tickets** (`updateTicketStatus`/`ticketUpdated` usa `specVersion`/`producer`/`subject` en vez de `eventId`/`sourceModule`/`data`) — nuestro `EventEnvelope` genérico no lo representa. Falta un segundo tipo de sobre el día que se implemente ese evento (hoy no publicamos ningún evento de tickets todavía).
- **Envelope de Rentas a confirmar** — sus ejemplos usan `timestamp`/`payload` en vez de `occurredAt`/`data`. El equipo cree que es un error de Rentas, pendiente de confirmar en persona.
- **`streetClosureActivated`** sigue marcado como evento interno — falta decidir si también se le informa a Obras/Ambiente.
- **Eventos de "aviso anticipado"** (`urbanServiceScheduled`, `treeRiskDetected`, `treePruningScheduled`, `workOrderScheduled`) — se decidió no crear un `StreetClosure` a partir de estos (evita registros huérfanos), pero el "registro liviano tipo alerta" que se propuso como alternativa todavía no está implementado.
- **Casing de valores en el contrato de eventos** — nuestros enums de Java son mayúscula (`MINOR`, `DNI`) pero algunos ejemplos de la doc muestran minúscula (`"minor"`). Hoy no importa porque los eventos solo se loguean, pero hay que definirlo antes de serializar JSON real hacia un broker.
- **Los logs de eventos hoy muestran objetos anidados como `Location@50e52116`** en vez de sus campos — es solo un problema de legibilidad del log actual (`toString` plano), se resuelve solo cuando haya serialización JSON real.

### Testing / CI
- **Cobertura de tests real es baja** (ya señalado en `CLAUDE.md`) — casi no hay tests de la lógica de negocio (services), lo cual hoy no bloquea `develop` pero sí bloquearía un PR a `main` si no llega al 85% que exige Jacoco.

### DTOs
- **Se usa un único DTO de respuesta por entidad**, no uno distinto por rol — decisión consciente para no multiplicar el trabajo. Si en algún momento un rol necesita ver/ocultar campos específicos de una entidad, hay que revisar esto puntualmente.
