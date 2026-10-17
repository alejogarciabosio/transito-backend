# Integraciones externas pendientes

## Vigencia de cortes

El backend puede finalizar automáticamente un corte `ACTIVE` al llegar `requestedTo`, publicando
`streetClosureEnded` con la nota `Cierre automático por vencimiento`. La regla está preparada,
pero se mantiene deshabilitada hasta la confirmación formal de M3:

```properties
STREET_CLOSURES_EXPIRATION_ENABLED=true
STREET_CLOSURES_EXPIRATION_FIXED_DELAY_MS=60000
```

Las solicitudes `REQUESTED` y los cortes `APPROVED` aún no activados no se cierran solos.

## RabbitMQ

No se deben inventar credenciales ni una topología de broker. Para conectar RabbitMQ faltan host,
puerto, usuario, contraseña, vhost/TLS, exchange, colas, bindings/routing keys, reintentos y
dead-letter queues. El backend conserva el contrato y el `LoggingEventPublisher` hasta recibir
esa información de infraestructura/M9.

## SOAP de Usuarios

El cliente SOAP real requiere URL/WSDL, operación, namespaces, esquema de request/response y
seguridad. También falta el mapeo oficial entre los roles de Usuarios (incluidos los códigos 3 y
7) y los roles de M7. Hasta recibirlo se conserva `StubIdentityValidator` solo para desarrollo y
tests.

## Contrato de Obras

`direction` ya se valida como `FORWARD`, `REVERSE` o `BOTH`. M3 debe confirmar formalmente que
`requestedTo` representa el fin operativo antes de habilitar el cierre automático.
