Este documento contiene el mensaje del módulo de Core hacia el resto de módulos en general:

---------------------------------------------------
¡Buenas! Acá les dejo las preguntas y respuestas que les mencioné el martes. Cualquier otra consulta que tengan o si alguna respuesta no logro ser lo suficientemente esclarecedora, dejenlo en el canal ⁠general y vemos como podemos mejorar. Todas las preguntas nuevas que surgan van a ir siendo contestadas por acá. Muchas gracias!!!


# 1. Estructura definitiva del envelope.
Campo | Tipo | Obligatorio? | Regla.
eventId | UUID | Sí | Nuevo por evento, es la clave de idempotencia.
eventType | String(<=120) | Sí | camelCase.
eventVersion | String(<=20) | No, default "1.0" | Versión del contrato.
occuredAt | ISO-8601 | Sí | Offset obligatorio.
sourceModule | String(<=60) | Sí | Su identificador (ej: "obras")
correlationId | UUID | No | El hilo del trámite.
causationId | UUID | No | el eventId que disparó este.
data | object | Sí | Es el payload del módulo correspondiente, por tanto es libre y el core no lo interpreta

NO SE ACEPTAN CAMPOS EXTRA, todo lo suyo va en "data"


# 2. Identificador único del evento."eventId", es un UUIDv4. Si se reenvia el mismo eventId el core responde 200 con "duplicate: true" y no vuelve a rutear.


# 3. Nombre y versión del contrato."eventType" en inglés y camelCase.


# 4. Módulo productor."sourceModule", tiene que coincidir con el módulo del token, sino es 403.
El token lo obtienen con:
  
  POST <URL aún a confirmar>/api/v1/auth/module-token
  { "module": "obras", "secret": "<el secret de maquina>" }
  
La respuesta devuelta es:

  { "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 900, "kind": "module" }

Dura 15 minutos y no hay refresh: cuando expira se pide otro. El secret vive en la configuración del backend de cada equipo y no lo usa ninguna persona. 


# 5. Fecha y zona horaria."occuredAt" en ISO-8601 con offset de zona horaria obligatorio.
Es el momento en que ocurrió en el módulo, el tiempo en que el core lo recibió es "receivedAt"


# 6. correlationId y, si corresponde, causationId.
si su evento sale de haber consumido otro, copien el correlationId del que consumieron y pongan su eventId en su causationId . Si el evento nace de una acción directa en Obras y no viene de ningún otro, generan un correlationId nuevo.


# 7. Estructura del campo de datos.
Totalmente libre.


# 8. Uso de UUID, camelCase, fechas ISO 8601 y JSON Schema.
Se responde con la tabla del 1.


# 9. Canal o tópico que debe utilizar cada evento.
No eligen canal. Publican todo al mismo lugar y nosotros ruteamos.


# 10. Reintentos, idempotencia, orden, auditoría y Dead Letter Queue.
Sobre los reintentos... Si su consumidor hace nack(requeue=false), reintentamos con backoff:
15 s → 1 m → 5 m → 15 m → Dead Letter Queue
Nunca usen requeue=true . La cadena la controlamos nosotros; si reencolan, el mensaje gira en su cola sin backoff y sin quedar auditado.

Sobre la idenpotencia: De nuestro lado, el Core no rutea dos veces el mismo eventId . Del lado de ustedes: la entrega es at-least-once. Un mensaje les puede llegar más de una vez (un nack reintentado, o una caída de conexión después de procesar pero antes del eventId que ya aplicaron).

Sobre el orden: No está garantizado. Dentro de la cola se respeta, pero un mensaje que falla y se reintenta vuelve más tarde, después de los que venían atrás. Si tienen lógica que depende del orden, no se apoyen en el orden de llegada: usen occurredAt o los datos del evento.

El Core guarda el sobre tal cual llegó y el estado de cada entrega. Consultable:
  GET /api/v1/events/{eventId}                    ¿llegó? ¿a quién se le entregó? 
  GET /api/v1/events?sourceModule=obras           lo que publicaron
  GET /api/v1/dashboard                           sus estadísticas

DLQ (Dead Letter Queue):
  Lo que agota los reintentos queda ahí, no se descarta. Pueden ver la cola de su módulo con:
  GET /api/v1/dlq?targetModule=obras
  Cada entrada trae el motivo, el payload crudo y el historial de reintentos. El reintento manual queda auditado con la persona que lo hizo.


# 11.Cómo registrar productores y consumidores en el catálogo.
Se autoadministran, no hace falta pedirnos nada. 

Declarar que publican un tipo
POST /api/v1/publications { "eventType": "workOrderCompleted" }
Es documentación, no un permiso: publicar un evento no declarado funciona igual. 
Existe para que el mapa de integración detecte los agujeros.


Suscribirse a un tipo
  POST /api/v1/subscriptions { "eventType": "streetClosureApproved", "maxAttempts": 4 } 
  La cola y su binding se declaran en el broker en el acto. 
  Pueden suscribirse a un tipo que todavía nadie publicó. 

Registrar un tipo de evento nuevo 
  POST /api/v1/event-types {  "name": "workOrderReassigned",  "ownerModule": "obras",  "description": "Una orden de trabajo cambio de cuadrilla.",  "jsonSchema": null } 
  jsonSchema opcional. 

Para activar o quitar la validación después: 
  PUT /api/v1/event-types/{name}/schema 
  Un tipo que nadie declaró no se rechaza: el Core lo auto-registra al verlo por primera vez y lo marca como discovered en el dashboard. 
  Es a propósito — los equipos todavía están alineando nombres y trabar la integración por un typo sería peor que dejarlo pasar y mostrarlo. 

Retirar un tipo DELETE 
  /api/v1/subscriptions/{id}

dejar de recibirlo 
  DELETE /api/v1/publications/{id}
  dejar de declararlo Los tipos de evento no se borran: hay eventos históricos que los referencian.
  Se marcan avisan en el catálogo.


# 12. Tecnología definitiva: Azure Event Grid, Kafka u otra.
RabbitMQ 4.3


# 13. Tópicos, colas o canales que existirán.
Para publicar — uno solo, compartido 
muni.inbox        fanout, durable 
Todos los módulos publican ahí. No eligen destino: el ruteo lo decide el Core leyendo las suscripciones.

  Para recibir — una cola por módulo 
  q.ciudadanos   q.atencion-ciudadana   q.obras      q.habilitaciones 
  q.rentas       q.ambiente             q.transito   q.desarrollo-social 
  En su cola les llega solo lo que pidieron, no todo el tráfico. 
  Pero es una cola con varios tipos adentro, no una por tipo: hay que despachar por eventType.


# 14. Cómo se registra cada productor y consumidor.
Cada equipo se autoadministra, no hace falta pedirle nada al Core. 
POST   /api/v1/publications    { "eventType": "workOrderCompleted" } 
POST   /api/v1/subscriptions   { "eventType": "ticketCreated", "maxAttempts": 4 } 
DELETE /api/v1/subscriptions/{id} 
GET    /api/v1/event-types/map                  quién publica y quién consume cada tipo 
Al crear una suscripción, el Core declara la cola y su binding en el broker en el acto. 
No hay que coordinar nada antes. 
Declaren su cola en modo pasivo ( passive=True / checkQueue ). 
La crea el Core; si la declaran con argumentos distintos, RabbitMQ cierra el canal con PRECONDITION_FAILED .
Declarar una publicación es documentación, no un permiso: publicar un tipo no declarado funciona igual. 
Existe para que el mapa detecte los agujeros de integración.


# 15. Formato oficial del sobre del evento.
La misma respuesta a la pregunta 1, pero dejo acá un ejemplo:{"eventId": "3f6c1b7e-9d24-4a1f-9f2a-2b0f0c7d5e11","eventType": "workOrderCompleted","eventVersion": "1.0","occurredAt": "2026-09-29T18:00:00Z","sourceModule": "obras","correlationId": "8a1f0c22-5d3e-4b77-9c10-6e2b4a90f3d5","causationId": null,"data": {}}


# 16. Si se usarán CloudEvents de manera nativa.
No lo usamos de forma nativa. El sobre es propio.


# 17. Autenticación entre módulos.
Los módulos no se autentican entre sí: nadie llama a nadie directamente. Todo pasa por el broker, y el broker autentica con las credenciales de RabbitMQ.
Más información en la pregunta 4.


# 18. Claims y validación de JWT.
Algoritmo RS256. Claims del token de módulo: 
Claim | Ejemplo | Qué es
sub | module:obras | El sujeto.
module | obras | El módulo. Es el que se compara con el sourceModule al publicar. 
kind | module | module (backend) o user (persona en el dashboard). 
isAdmin | false | Solo el equipo 9. Habilita ver el tráfico de todos. 
displayName | Obras Publicas | Para mostrar. 
iss | muni-core | Emisor.
aud | muni-platform | Audiencia. 
iat · exp  | epoch | Emisión y expiración (15 min). 
jti | — | Identificador del token.

  Solo el Core valida. Los módulos no necesitan validar nada:
  los tokens que emitimos sirven para llamarnos a nosotros, y ustedes nunca reciben uno.


# 19. URLs de los ambientes.
Se colocarán acá una vez que esten desplegados los ambientes de testing y de producción (Aprox 13/10/2026)


# 20. Forma segura de entregar credenciales.
Aún siendo definido, seguramente sea un documento al PO de cada módulo.
