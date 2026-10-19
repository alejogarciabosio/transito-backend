# Estado de integraciones - revisión de pendientes

## Resuelto por M7

- RabbitMQ: publicación al exchange `muni.inbox` y consumo de `q.transito`, con JSON, deduplicación por `eventId` y configuración por variables `CORE_RABBITMQ_*`.
- Usuarios: cliente SOAP `validateUser` implementado (`dni`, `password` y respuesta `user_id`, `dni`, `role`, `auth`), con timeout de cinco segundos y errores `502`/`503`.
- Roles: el backend usa exclusivamente `ADMINISTRADOR`, `INSPECTOR` y `CIUDADANO`.
- Cortes: `streetClosureRequested` procesa `M3`/`M6`, conserva `sourceRef`, `reason`, `closureType` y `requestedAt`; las respuestas usan `Obras`/`Ambiente`.
- Ventana operativa: `authorizedTo` gobierna el corte. Al vencer, un corte `ACTIVE` se finaliza y emite `streetClosureEnded`; uno `APPROVED` pasa a `EXPIRED` y no puede activarse.
- Rentas: se procesan `paymentRegistered`, `debtSettled` y `paymentReversed` para `TRAFFIC_FINE`. `originId` e `infractionId` son el mismo UUID de la infracción confirmada por M7.

## Convención cerrada para Rentas

- `eventType`: camelCase con inicial minúscula (`paymentRegistered`, `debtSettled`, `paymentReversed`).
- `sourceModule`: minúscula (`rentas`).
- `originType`: MAYÚSCULA (`TRAFFIC_FINE`).
- Campos dentro de `data`: camelCase (`originId`, `infractionId`, `amountApplied`, `remainingBalance`, `paidAt`).

## Pendientes externos

### Módulo de Usuarios

El contrato SOAP fue definido por el equipo y el cliente está listo. Sólo falta publicar la URL real de M1 para activar `USERS_SOAP_ENABLED=true`; mientras tanto continúa el stub local de desarrollo.

### Core

Entregar host, puerto, vhost, usuario, contraseña y TLS/CA de RabbitMQ. Confirmar que la aplicación debe publicar al fanout `muni.inbox`, consumir `q.transito`, y el formato definitivo de DLQ/reintentos. También confirmar si requiere Transactional Outbox antes de habilitar producción.

### Obras y Ambiente

Publicar la versión final de `streetClosureRequested`, incluido si las prórrogas usan `previousClosureRequestId`, y confirmar si `streetClosureActivated` se distribuye también a M3/M6 o sólo queda como evento interno.

### Rentas

Confirmar sólo la grafía definitiva de `occurredAt` (en algunos textos aparece `occuredAt`) y realizar una prueba real por Core. El envelope y el contrato para `TRAFFIC_FINE` se adoptan de la especificación de M5.

### Equipo de integración

Confirmar el casing definitivo de los enums en todos los contratos entre módulos. La API REST de M7 serializa enums Java en mayúscula (`PENDING_APPEAL`, `DNI`, etc.); no se debe transformar la serialización de eventos sin ese acuerdo explícito.
