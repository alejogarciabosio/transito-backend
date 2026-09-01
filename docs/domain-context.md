# Módulo 7 — Contexto de dominio (Tránsito, Estacionamiento y Seguridad Vial)

> Importado desde CLAUDE.md. Este archivo cubre el modelo de negocio; el archivo raíz cubre stack e infraestructura. Ver también `docs/modulo7-crud-endpoints.md` (endpoints base) y `docs/modulo7-revision-payloads.md` (contratos de eventos).

## Entidades del dominio

- **TrafficOperation** (Operativo) — paso de verificación obligatorio antes de crear una Infraction o un RoadAccident. Nace de un ticket de Módulo 2, de una denuncia, o de un control de rutina.
- **Infraction** — nunca nace directamente de un ticket ciudadano. Siempre pasa por un Operativo que la confirma (salvo que la evidencia adjunta ya alcance para que el inspector valide de forma remota, sin despacho físico).
- **RoadAccident** — puede registrarse en estado preliminar y completarse después.
- **StreetClosure** — se crea al consumir `streetClosureRequested` (de Obras/Ambiente), no vía POST del frontend.
- **VehicleImpoundment** — puede originarse en una Infraction o en un TrafficOperation (`originType`).
- **ParkingZone / ParkingTariff / ParkingSession** — dominio de estacionamiento medido. No agrega eventos nuevos hacia otros módulos: la falta de pago genera una `Infraction` con `infractionType: "unpaid_parking"`, reutilizando todo el circuito de confirmación/apelación/Rentas ya existente.

## Flujo: ticket de Módulo 2 → resolución

1. Módulo 2 (Atención Ciudadana) es el único módulo que crea tickets. Categoriza el reclamo (infracción / accidente / estacionamiento / seguridad vial) antes de derivarlo.
2. Tránsito recibe el ticket → crea un `TrafficOperation` en estado `pending_verification`. **La categoría del ticket indica qué investigar, no que ya esté validado** — un reclamo ciudadano no es evidencia legal por sí solo.
3. Tránsito publica `updateTicketStatus` a Módulo 2 (informativo: "en revisión"). Esto **no es un pedido de despacho** — el inspector es un recurso propio de Tránsito, no de Módulo 2.
4. El inspector valida: en persona (despacho físico) o revisando evidencia adjunta si ya es suficiente (ej. foto/video con patente y timestamp).
5. Según el resultado: se crea la `Infraction`/`RoadAccident` correspondiente, o se cierra el Operativo sin sanción.
6. Tránsito publica `updateTicketStatus` final a Módulo 2 con el resultado.

## Relación entre procesos internos

- `TrafficOperation` es el hub: puede derivar en `Infraction`, `RoadAccident`, o `VehicleImpoundment` (retención durante el operativo mismo).
- `Infraction` también puede derivar en `VehicleImpoundment` (retención por infracción, no por operativo).
- `ParkingSession` (falta de pago) alimenta a `Infraction`.
- `StreetClosure` es independiente — solo interactúa con Obras/Ambiente, no con el resto de las entidades propias.
- Los indicadores/dashboard (F7) son de solo lectura sobre el resto — no participan del flujo de negocio.

## Reglas de negocio (además de las ya listadas en CLAUDE.md)

- Una infracción puede confirmarse por dos caminos que convergen en el mismo evento `infractionConfirmed`: (a) vence el plazo de apelación sin que el ciudadano apele, o (b) el ciudadano apela y Tránsito rechaza la apelación.
- El descargo se resuelve internamente en Tránsito — no depende de ningún evento externo.
- Pendiente de definir con el equipo: ¿se permite volver a apelar una infracción ya resuelta (reapelación)? Esto determina si el campo `appealId` en `infractionAppealed` es necesario o redundante con `infractionId`.

## Catálogo de eventos — pendientes conocidos

- Falta el "sobre" común de evento exigido por la consigna (eventId, eventType, occurredAt, sourceModule) — todos los payloads documentados hasta ahora son solo el `data` interno.
- Falta el payload de `infractionAnulled` (marcado "a completar").
- Typo pendiente de corregir: `streetClousureEnded` → `streetClosureEnded`.
- Falta el payload de `streetClosureActivated` (aparece en el enunciado original pero no en el contrato de eventos).
- Falta agregar `ticketCreated` a los eventos que Tránsito consume de Módulo 2 (hoy solo está `ticketUpdated`).
- Payloads de otros módulos que Tránsito todavía no tiene pero necesita: `paymentRegistered` (Rentas) y `streetClosureRequested` (Obras/Ambiente).

## Tipos de integración presentes en el proyecto

- **Procesos**: patrón dominante — toda la integración asincrónica basada en eventos entre módulos.
- **Procedural**: excepción acotada — REST síncrono solo cuando se necesita respuesta inmediata (ej. validar identidad).
- Explícitamente ausentes/prohibidos: integración de Datos (prohibida por consigna — nada de acceso directo a BD ajena), Archivos, y Portales (cada módulo tiene su FE independiente).
