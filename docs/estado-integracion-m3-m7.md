# Estado de integración M3 Obras Públicas -> M7 Tránsito

**Fecha de actualización:** 17/10/2026  
**Rama:** `codex/validacion-backend-tpo`

## Implementado

### Evento de entrada

M7 consume el evento `streetClosureRequested` y crea un corte en estado `REQUESTED`.

Se reciben y persisten los campos acordados:

| Campo | Estado |
| --- | --- |
| `closureRequestId` | Implementado |
| `sourceModule` | Implementado; se mapea a Obras/Ambiente |
| `sourceRef` | Implementado; M3 puede enviar su `workOrderId` |
| `reason` | Implementado |
| `affectedSections` | Implementado como objetos `{street, from, to, direction}` |
| `requestedFrom` / `requestedTo` | Implementados como fecha y hora con zona horaria |
| `closureType` | Implementado y opcional |
| `requestedAt` | Implementado |
| `previousClosureRequestId` | Implementado para prórrogas |

`direction` acepta exclusivamente `FORWARD`, `REVERSE` y `BOTH`.

### Eventos de salida

Se publican con estos nombres exactos:

- `streetClosureApproved`
- `streetClosureRejected`
- `streetClosureActivated`
- `streetClosureEnded`

El nombre `streetClosureEnded` está corregido y validado; no se usa el typo `streetClousureEnded`.

### Vigencia de un corte

Se agregó cierre automático configurable. Si un corte está en estado `ACTIVE` y llegó su
`requestedTo`, M7 lo finaliza y publica `streetClosureEnded` con la nota
`Cierre automático por vencimiento`.

La regla está deshabilitada por defecto hasta que M3 confirme formalmente que `requestedTo`
representa el fin operativo del corte. Para habilitarla:

```properties
STREET_CLOSURES_EXPIRATION_ENABLED=true
STREET_CLOSURES_EXPIRATION_FIXED_DELAY_MS=60000
```

Una solicitud `REQUESTED` o un corte `APPROVED` todavía no activado no se finaliza en forma automática.

## Validación realizada

Se ejecutó la suite completa del backend:

```text
Tests run: 163, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Además, se validaron específicamente:

- recepción y persistencia de una solicitud M3 completa;
- prórrogas mediante `previousClosureRequestId`;
- tramos estructurados y las tres direcciones permitidas;
- rechazo de una dirección inválida;
- publicación de eventos de aprobación, activación, rechazo y finalización;
- cierre automático de un corte activo vencido.

## Pendientes externos

Estos puntos no pueden implementarse correctamente sin información de otros equipos; no se deben
reemplazar por valores inventados.

| Dependencia | Información requerida | Responsable |
| --- | --- | --- |
| RabbitMQ | Host, puerto, credenciales, vhost/TLS, exchange, colas, bindings/routing keys, reintentos y DLQ | Infraestructura / M9 |
| Usuarios SOAP | URL/WSDL, operación, namespaces, request/response y seguridad | Módulo Usuarios |
| Roles | Mapeo oficial de los códigos de Usuarios (incluidos 3 y 7) a los roles de M7 | Módulo Usuarios |
| Vigencia | Confirmación de que `requestedTo` es el fin operativo y habilitación de la variable | M3 Obras Públicas |
| Contrato final | Ejemplos JSON versionados y aprobación final del contrato M3-M7 | M3 y M7 |

Mientras esos datos no estén disponibles, el backend conserva el publicador de eventos en modo
log y el validador local de identidad para desarrollo y pruebas. La estructura de dominio y los
contratos de cortes ya están preparados.
