Mensajes de RO atener en cuenta a fecha de hoy --> 20/10/26

“# Estado de integración M3 Obras Públicas -> M7 Tránsito

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
`requestedTo`, M7 lo finaliza y publica `streetClosureEnded` con la nota `Cierre automático por vencimiento`.

La regla está deshabilitada por defecto hasta que M3 confirme formalmente que `requestedTo` representa el fin operativo del corte. Para habilitarla:

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

Estos puntos no pueden implementarse correctamente sin información de otros equipos; no se deben reemplazar por valores inventados.

| Dependencia | Información requerida | Responsable |
| --- | --- | --- |
| RabbitMQ | Host, puerto, credenciales, vhost/TLS, exchange, colas, bindings/routing keys, reintentos y DLQ | Infraestructura / M9 |
| Usuarios SOAP | URL/WSDL, operación, namespaces, request/response y seguridad | Módulo Usuarios |
| Roles | Mapeo oficial de los códigos de Usuarios (incluidos 3 y 7) a los roles de M7 | Módulo Usuarios |
| Vigencia | Confirmación de que `requestedTo` es el fin operativo y habilitación de la variable | M3 Obras Públicas |
| Contrato final | Ejemplos JSON versionados y aprobación final del contrato M3-M7 | M3 y M7 |

Mientras esos datos no estén disponibles, el backend conserva el publicador de eventos en modo log y el validador local de identidad para desarrollo y pruebas. La estructura de dominio y los contratos de cortes ya están preparados.”

------------------------------------------------------------------------------------
“De payloads, ya no falta definir nada para Rentas ni para Obras; para Ambiente aplica el mismo streetClosureRequested, cambiando sourceModule por ambiente.

## Lo único que falta cerrar explícitamente es:

- Si streetClosureActivated se publica hacia Obras/Ambiente o queda sólo interno de Tránsito. Si se publica, hay que confirmar destinatarios y congelar ese payload.
- Confirmar si el formato anterior de affectedSections como texto llegó a circular: eso decide eventVersion 1.0 o 2.0.
- La grafía final de occurredAt en Rentas, porque aparece una vez como occuredAt en material previo.
El resto de los payloads de M7 no cambia.”


------------------------------------------------------------------------------------
"Hola! Revisamos la configuración desplegada, los registros de Tránsito y el código de los endpoints. Les respondemos punto por punto:

# 1. Ambiente y URL
Actualmente hay un único ambiente compartido en https://core-uade-2026.duckdns.org, no uno de testing separado. Los endpoints están bajo /api/v1. Podemos coordinar pruebas ahí, teniendo en cuenta que los eventos pueden llegar a otros módulos suscriptos.

# 2. Secret de máquina
Está pendiente de entrega por privado. Vamos a localizarlo y verificarlo; si hace falta rotarlo, lo coordinamos antes porque invalida el anterior. Este secret sirve para obtener el token por HTTP y es diferente de la contraseña de RabbitMQ.

# 3. RabbitMQ
Host: core-uade-2026.duckdns.org
Puerto: 5671
Virtual host: core
Usuario: module_transito
Contraseña: pendiente de entrega por privado.

Usar AMQPS con validación de la cadena del certificado y del nombre core-uade-2026.duckdns.org. No requiere certificado de cliente y no deben desactivar la validación TLS.

Para habilitar la conexión necesitamos la IP pública de salida del backend donde correrá M7. Si hacen pruebas desde otras redes, también necesitamos esas IP.

# 4. Cola y permisos
q.transito existe y configuramos permisos de lectura para module_transito. Deben usar acceso pasivo y consumir la cola existente, sin declaraciones activas ni creación de bindings. Falta verificar la conexión desde su entorno una vez habilitada su IP; todavía no damos esa prueba por realizada.

# 5. Suscripción
streetClosureRequested ya está registrada y activa para transito con maxAttempts: 4. Pueden mantener esa configuración: son cuatro intentos totales, incluido el inicial. No necesitan crear una segunda suscripción.

# 6. Publicaciones
Ya están registradas y activas:
- streetClosureApproved
- streetClosureRejected
- roadAccidentRegistered

No aparecen streetClosureActivated ni streetClosureEnded. Sí aparece streetClousureEnded, con esa diferencia de escritura. Obras está suscripto a streetClosureEnded, por lo que ambos nombres no coinciden ni se tratan automáticamente como alias.

Nos falta acordar la corrección del nombre anterior y confirmar streetClosureActivated contra el contrato. Por ahora no cambiamos ni eliminamos registros. La declaración de publicaciones documenta el mapa de integración; no otorga permisos de publicación.

# 7. Registros repetidos al reiniciar
Según el código actual, repetir un POST para el mismo módulo y evento reutiliza el registro:
- /api/v1/subscriptions lo reactiva y actualiza maxAttempts.
- /api/v1/publications lo reactiva.

Ambos responden 201 incluso si ya existían. Para respetar las pausas del dashboard, conviene consultar primero y crear solamente lo que falte, revisando también si está activo y su configuración. No verificamos altas simultáneas desde varias instancias.

Con la IP habilitada y las credenciales entregadas, coordinamos una prueba completa de publicación y consumo."


-------------------------------------------------------------------------------
## FECHA - 2/11/26

"Ciudadano
- Gmail: testuser@gmail.com
- Password: clave-segura-1
Empleado
- Gmail: admin@municipio.gob.ar
- Password: Empleado.2026
Ambas se utilizan en POST https://ciudadanos-m1-backend.onrender.com/auth/usuario/login, enviando los campos gmail y password.

Avísennos el resultado para cerrar la prueba de integración."
Esto de arriba entre comillas es del módulo de Ciudadano


Acá va otro mensaje:
"Buenas! Somos Core. Revisamos el catálogo compartido. streetClosureEnded ya está registrado y tiene activas las suscripciones de Obras y Ambiente, ambas con maxAttempts: 4.

Falta que Tránsito declare la publicación para ese nombre correcto. Con su token de módulo:

POST https://core-uade-2026.duckdns.org/api/v1/publications

Headers:
Authorization: Bearer <token de transito>
Content-Type: application/json

Body:
{
  "eventType": "streetClosureEnded"
}

Al omitir module, Core utiliza el módulo autenticado. No hace falta recrear el tipo ni las suscripciones. La creación exitosa responde HTTP 201. Si devuelve otro resultado, compártannos el código y el cuerpo de respuesta, sin el token, antes de repetir.

Después coordinamos la prueba con el nombre correcto. Compartan el eventId para verificar recepción y ruteo en Core; Obras y Ambiente deberán confirmar la recepción y el procesamiento en sus aplicaciones.

La publicación histórica streetClousureEnded sigue activa y la conservamos hasta validar esa transición.

También queda señalado que streetClosureEnded figura sin propietario y sin schema de data asociado. La declaración de publicación no equivale por sí sola a completar esos metadatos ni a validar el contrato del payload."



-------------------------------------------------------------------------------
## FECHA 5/11

# Mensaje que envió Ro:
Buenos días, equipo Core, somos el equipo de Tránsito. Les queríamos consultar si es posible si nos pueden mandar el secret que está pendiente de entrega, de esta forma podríamos continuar con el desarrollo para las conexiones sincrónicas y asincrónicas reales. Sin mencionar que el secret es necesario para obtener el token de nuestros módulo y permitir la comunicación.

Además, nos gustaría saber si, en el comunicado en discord hacia los distintos equipos, no fuimos mencionaron por alguna razón, ya que se mencionaron al resto de los módulos excepto el de Tránsito.

Y para finalizar, nos gustaría saber cuándo es posible realizar las pruebas de conexión con otros módulos, así sabremos qué debemos modificar para que la conexión funcione. Necesitamos algo para el momento del testeo de la conexión? 

Gracias por su tiempo
Equipo de Tránsito


# Mensaje de respuesta de Core
¡Buenos días, equipo de Tránsito! ¿Cómo están?

Sí, claro. Como el Core guarda el secret de máquina únicamente hasheado (SHA-256) y por seguridad no enviamos credenciales por canales generales, se lo generamos/rotamos y se lo pasamos por mensaje privado.

Para habilitarles el acceso al entorno desplegado en AWS y entregarles las credenciales hoy mismo, necesitamos que nos confirmen por privado estos dos datos:

La IP pública de salida desde donde se va a conectar su backend (y si es fija o puede cambiar), así la habilitamos en el firewall/Security Group de AWS.
A quién de su equipo le mandamos por privado el paquete de credenciales.
¿Qué credenciales les vamos a entregar?

Van a recibir dos credenciales distintas (no son intercambiables):

Secret de máquina del módulo (transito): para pedir el JWT en POST https://core-uade-2026.duckdns.org/api/v1/auth/module-token enviando {"module": "transito", "secret": "<secret>"}. Ese token (Bearer) lo usan para las conexiones sincrónicas al Core (consultar catálogos globales en /api/v1/catalogos/*, publicar eventos por HTTP en POST /api/v1/events, consultar bitácora/suscripciones/DLQ).
Credencial AMQPS de RabbitMQ (module_transito): para la conexión asincrónica directa al broker.
¿Cuándo podemos hacer las pruebas de conexión y qué necesitan tener listo?

Apenas nos pasen la IP pública y la habilitemos, ya pueden probar tanto la conexión contra el Core como el flujo cruzado con otros módulos.

Para que la prueba salga funcionando a la primera, tengan en cuenta este checklist en su configuración:

Datos de conexión al servidor desplegado:
Base HTTPS (API Core): https://core-uade-2026.duckdns.org
Broker RabbitMQ (AMQPS): host core-uade-2026.duckdns.org, puerto 5671, protocolo amqps://, virtual host core, usuario module_transito.
Importante: usen siempre el dominio (no la IP) y mantengan activa la validación del certificado TLS.
Para publicar eventos (por AMQPS o por HTTP):
Por AMQPS: publicar en el exchange muni.inbox (fanout, routing key vacía ""), con content_type="application/json", mensaje persistente (delivery_mode=2) e incluyendo la propiedad AMQP user_id="module_transito".
Formato del sobre:
eventId: UUID v4 nuevo por evento.
eventType: nombre exacto en camelCase.
eventVersion: "1.0".
occurredAt: ISO-8601 con zona horaria obligatoria (ej. "2026-11-03T10:00:00-03:00" o terminado en Z).
sourceModule: "transito".
data: objeto JSON { ... } (siempre presente).
Para consumir eventos (q.transito):
Su cola ya está creada por el Core: q.transito. -Conéctense usando declaración pasiva (passive=True / checkQueue), sin declarar exchanges ni crear bindings desde su código.
Al procesar bien: ack. Si falla el procesamiento: nack / reject con requeue=false (nunca requeue=true, porque los reintentos con backoff y el envío a DLQ los maneja el Core).
Detalle a revisar antes de probar con Obras y Ambiente:
En el catálogo figura declarado por Tránsito el evento streetClousureEnded (con "u" en Clousure), mientras que Obras está suscripto a streetClosureEnded. Les recomendamos unificar el nombre en streetClosureEnded y actualizar la declaración en POST /api/v1/publications para que al probar llegue a destino.
Recuerden también que hoy hay un único ambiente compartido (vhost core), así que cuando lancen un evento de prueba conviene avisar al módulo suscriptor con el eventId generado para verificar juntos que llegó y se confirmó.

Pásennos la IP por privado cuando quieran y coordinamos la prueba inicial. ¡Gracias!


-------------------------------------------------------------------------------
## FECHA 8/11/26

# Mensaje de respuesta de Core respecto el secreto e IP pública dinámica:
"Buenas, equipo de Tránsito.

Ya habilitamos la IP pública `152.55.177.100/32` en las reglas de seguridad de AWS Core para conexiones AMQPS por el puerto `5671`.

También verificamos que el módulo `transito` se encuentra registrado y activo en Core.

Les compartimos los datos de integración:

**Conexión RabbitMQ (AMQPS)**
- Host: `core-uade-2026.duckdns.org`
- Puerto: `5671` (TLS)
- Virtual host: `core`
- Usuario: `module_transito`
- Cola de consumo: `q.transito`
- Exchange de publicación: `muni.inbox`
- Routing key de publicación: cadena vacía

**API de Core**
- URL base: `https://core-uade-2026.duckdns.org/api/v1`
- Módulo: `transito`
- Endpoint de autenticación: `POST /auth/module-token`
- Secreto de máquina: <REDACTADO — cargado como CORE_MODULE_SECRET en Railway, no se guarda en este documento>

Para autenticarse en la API, deben enviar el nombre del módulo y el secreto de máquina al endpoint indicado. La respuesta proporciona un token que podrán utilizar en las solicitudes HTTP autorizadas.

Aclaramos que el secreto de máquina es independiente de la contraseña de RabbitMQ utilizada para AMQPS. Si no disponen de esta última, avísennos para resolverlo.

Como nos comentaron que su IP pública de salida es dinámica, les pedimos que nos reconfirmen que sigue siendo `152.55.177.100` antes de realizar las pruebas de conexión.

Con esto queda preparada la configuración de acceso del lado de Core. Queda pendiente verificar la conectividad y la autenticación efectivas desde su backend.

Saludos, equipo Core."

# Mensaje del módulo de Rentas:
(este mensaje a continuación me suena que ya lo vimos anteriormente, pero no estoy del todo seguro, así que te lo vuelvo a mandar porque Ro no sabe ni de qué está hablando Rentas)
"Hola equipo M5. Validamos e implementamos en M7 el contrato M5 → M7 consolidado el 27/10.  
Quedaron aceptados paymentRegistered, debtSettled y paymentReversed, con paymentId y debtId como UUID persistentes serializados como string; citizenId opcional; conceptCode: "TRAFFIC_INFRACTION"; y originType: "TRAFFIC_FINE".  
M7 valida que originId e infractionId sean el mismo UUID de una infracción existente de Tránsito, y procesa los eventos de forma idempotente por eventId. También validamos montos positivos y saldo cero al emitir debtSettled.  
De nuestro lado el contrato queda cerrado. Solo queda pendiente realizar la prueba E2E real a través de Core antes de habilitar tráfico entre módulos."

(esto es un mensaje nuevo y el último que me mandó Ro y que ella recibió de Rentas)
"¡Buenas! Somos de M5 Rentas.

Estamos avanzando con la integración de las infracciones y nos falta cerrar el circuito de anulación.

¿Nos pueden compartir el contrato actual del evento que publican cuando se anula una infracción?

Necesitamos confirmar el eventType exacto, eventVersion, payload, identificador de infracción y cómo debemos relacionarlo con la obligación o deuda creada previamente.

Del lado de Rentas estamos contemplando dos situaciones: si la deuda no tiene pagos, poder anularla; si ya tiene pagos imputados, derivarla a revisión sin eliminar los registros económicos.

También necesitamos conservar eventId, correlación y trazabilidad del origen.

Con el contrato confirmado podemos implementar y probar el consumidor. ¡Gracias!"
