# Tránsito Backend (M7)

Backend del módulo de Tránsito, Estacionamiento y Seguridad Vial del TPO de Desarrollo de Aplicaciones II (UADE).
Java 17, Spring Boot 4, PostgreSQL, RabbitMQ (eventos con Core), REST en `/api/v1`.

## Levantar todo desde cero (un solo comando, menos de 15 minutos)

Requisitos: Docker Desktop corriendo (con `docker compose`) y `curl`. Nada más: no hace falta Java, Maven ni cuentas externas.

1. Clonar el repo y entrar a la carpeta.
2. Crear el `.env` y completar **cada** variable (el archivo `.env.example` explica cada una; `.env` no se versiona):

   ```bash
   cp .env.example .env
   ```

   Valores sugeridos para local (son ficticios, solo desarrollo):

   | Variable | Qué poner |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://db:5432/transito` |
   | `DB_USER` | `transito` |
   | `DB_PASSWORD` | cualquier texto (ej. `dev-local-db`) |
   | `JWT_SECRET` | texto de 32 caracteres o más |
   | `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` |
   | `USERS_REST_ENABLED` | `false` (ver "Usuarios" más abajo) |
   | `CORE_RABBITMQ_PASSWORD` | cualquier texto (ej. `dev-local-rabbit`); es la clave del RabbitMQ local |
   | `CORE_MODULE_SECRET` | cualquier texto no vacío; es un "secret" ficticio para el mock de Core |

   Si falta una variable, `docker compose` corta con un mensaje del tipo `Falta DB_URL en .env`.
3. Ejecutar el único comando:

   ```bash
   docker compose up --build
   ```

   Levanta cinco contenedores: `db` (PostgreSQL), `rabbitmq` (broker local), `rabbitmq-init` (carga el exchange y las
   colas y termina), `mocks` (WireMock de M1 y de Core) y `backend`. La primera vez tarda unos minutos (descarga
   imágenes y compila); después arranca en menos de un minuto.
4. Verificar: `curl http://localhost:8080/actuator/health` devuelve `{"status":"UP",...}`.

Para apagar y borrar todo: `docker compose down -v`.

Nunca se llama al Core real ni a producción: el compose fija el broker (`rabbitmq:5672`, sin TLS), `CORE_BASE_URL`
(`http://mocks:8080`) y la URL de M1 a los contenedores locales.

| URL | Qué es |
|---|---|
| http://localhost:8080/swagger-ui/index.html | Swagger (contrato en `/openapi.yaml`) |
| http://localhost:8080/actuator/health | Health |
| http://localhost:15672 | Consola de RabbitMQ (usuario `module_transito`, clave = `CORE_RABBITMQ_PASSWORD`) |
| http://localhost:8081/__admin/requests | Pedidos que recibieron los mocks |

## Usuarios para ingresar

Con `USERS_REST_ENABLED=false` (valor recomendado) rige el validador local. Login: `POST /api/v1/auth/login`
con `{"username":"...","password":"..."}`.

| Usuario | Clave | Rol |
|---|---|---|
| `ciudadano1` | `ciudadano123` | CIUDADANO |
| `inspector1` | `inspector123` | INSPECTOR |
| `admin1` | `admin123` | ADMINISTRADOR |

```bash
curl -s -H 'content-type: application/json' -d '{"username":"admin1","password":"admin123"}' http://localhost:8080/api/v1/auth/login
```

### Mock de M1 (login REST)

El contenedor `mocks` simula `POST /auth/usuario/login` de M1 (contrato en
`docs/contratos-integracion/contratos-validacion/M1_a_M7_Adaptador_SOAP_REST_2026-10-31.md`), expuesto en
`http://localhost:8081/auth/usuario/login` y, dentro de la red de Docker, en `http://mocks:8080/auth/usuario/login`.
Cuentas ficticias (campos `gmail` y `password`):

| Cuenta | Clave | Respuesta |
|---|---|---|
| `ciudadano.mock@example.com` | `Ciudadano123!` | 200, `actorType` CIUDADANO, `role` CIUDADANO, con `citizenId` y DNI |
| `empleado.mock@example.com` | `Empleado123!` | 200, `actorType` EMPLEADO, `role` MESA_ENTRADAS (INSPECTOR en M7) |
| cualquier otra | cualquiera | 401 |

Con `USERS_REST_ENABLED=true` el backend hace el login contra este mock (el `RestIdentityValidator` ya usa Jackson 3,
corregido en #23).

### Mock de Core (HTTP)

El mock también responde lo que el backend pide por HTTP a Core: `POST /api/v1/auth/module-token` (token ficticio) y
`POST /api/v1/publications` (201). Al arrancar, el backend registra sus eventos y se ve en
`docker compose logs backend | grep "registrada en Core"`.

## RabbitMQ local

- Exchange `muni.inbox` (fanout) y cola `q.transito` quedan creados por `docker/rabbitmq/definitions.json`
  (la app no crea colas: las verifica en modo pasivo al arrancar).
- `q.transito` es la cola que consume M7 (eventos que "envía Core"). No está enlazada a `muni.inbox`, así M7 no se
  consume a sí mismo.
- `q.transito.inspeccion` está enlazada a `muni.inbox`: recibe una copia de todo lo que publica M7 (por ejemplo
  `streetClosureApproved`). Nadie la consume; se mira desde la consola (Queues, `q.transito.inspeccion`, Get messages) o con:

  ```bash
  curl -s -u module_transito:<CORE_RABBITMQ_PASSWORD> -H 'content-type: application/json' \
    -d '{"count":5,"ackmode":"ack_requeue_true","encoding":"auto"}' \
    http://localhost:15672/api/queues/%2F/q.transito.inspeccion/get
  ```

- Comprobar que el consumidor está conectado: `docker compose exec rabbitmq rabbitmqctl list_queues name consumers`
  (`q.transito` con 1 consumidor) y en el log `[core-rabbitmq] conexión verificada, cola 'q.transito' accesible`.

### Inyectar un evento de prueba

```bash
./scripts/inyectar-street-closure.sh            # publica un streetClosureRequested en q.transito
./scripts/inyectar-street-closure.sh OT-1234    # con un sourceRef propio
```

Lee la clave del `.env` sin mostrarla. Luego, con un token de `admin1`, `GET /api/v1/street-closures` muestra el corte
en estado `REQUESTED`; se puede aprobar con `POST /api/v1/street-closures/{id}/approval`
(`{"detours":[],"conditions":["señalizar"]}`) y aparece `streetClosureApproved` en `q.transito.inspeccion`.

### Simular "broker caído y vuelve"

```bash
docker compose stop rabbitmq     # el broker se cae
docker compose start rabbitmq    # vuelve; la app se reconecta sola (unos segundos), sin reiniciar el backend
docker compose exec rabbitmq rabbitmqctl list_queues name consumers   # q.transito vuelve a tener 1 consumidor
```

Mientras el broker está caído, `/actuator/health` puede tardar o no responder (indicador de RabbitMQ); al volver se
normaliza. Una publicación hecha en ese lapso se pierde hasta que exista el outbox (ver plan de la entrega).

## Tests locales

Los tests usan H2/Testcontainers y no requieren el compose:

```bash
./mvnw -q -B verify
```

## Estado de integraciones

- Autenticación JWT y roles; fachada SOAP propia (`/soap/authentication`) que delega en el REST de M1.
- Eventos con Core por RabbitMQ (en producción AMQPS con las credenciales reales, cargadas como variables `CORE_*`
  en Railway; nunca en este repo).
- Consumo idempotente (`processed_events`), cierre automático de cortes, Problem Details, ETag.
- El frontend está en el repo `transito-frontend`; en el compose hay un bloque comentado para levantarlo al lado.
