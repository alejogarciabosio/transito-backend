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
   colas y termina), `mocks` (WireMock de M1, de Core y del proveedor de IA) y `backend`. La primera vez tarda unos minutos (descarga
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

| Usuario | Variable con la contraseña | Rol |
|---|---|---|
| `ciudadano1` | `STUB_PASSWORD_CIUDADANO1` | CIUDADANO |
| `inspector1` | `STUB_PASSWORD_INSPECTOR1` | INSPECTOR |
| `admin1` | `STUB_PASSWORD_ADMIN1` | ADMINISTRADOR |

Las contraseñas **no están en el código**. En local salen del `.env` (los valores de desarrollo están en
`.env.example`); en producción se configuran como variables del servicio en Railway (los valores de producción
no se publican). Si una variable falta o está vacía, ese usuario queda deshabilitado (401) y el backend lo avisa
en el log al arrancar. Ver [docs/seguridad/credenciales.md](docs/seguridad/credenciales.md).

```bash
curl -s -H 'content-type: application/json' -d "{\"username\":\"admin1\",\"password\":\"$(grep '^STUB_PASSWORD_ADMIN1=' .env | cut -d= -f2-)\"}" http://localhost:8080/api/v1/auth/login
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

Con el broker caído **no se pierden mensajes** (Transactional Outbox, [ADR-006](docs/adr/ADR-006-transactional-outbox.md)):
la operación de negocio responde 200 igual, el evento queda guardado en `outbox_events` con estado `PENDING` y el relay
(cada 2 s) lo publica cuando el broker vuelve, pasando a `PUBLISHED`. Para verlo, con un token de `admin1`:

```bash
docker compose stop rabbitmq
# aprobar un corte (ver "Inyectar un evento de prueba"): responde 200 aunque no haya broker
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" 'http://localhost:8080/api/v1/admin/outbox?status=PENDING'   # el evento está PENDING
docker compose start rabbitmq
sleep 10
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" 'http://localhost:8080/api/v1/admin/outbox?limit=5'          # ahora PUBLISHED
```

`GET /api/v1/admin/outbox` (roles ADMINISTRADOR y AUDITOR; filtros opcionales `status=PENDING|PUBLISHED|FAILED` y
`limit`) lista los eventos sin el payload. Mientras el broker está caído, `/actuator/health` puede tardar (indicador de
RabbitMQ); al volver se normaliza.

## IA: sugerencia de infracción con el mock del proveedor

La IA está apagada por defecto. Para la demo local **sin clave real**, el contenedor `mocks` imita Groq
`chat/completions` (`docker/wiremock/mappings/groq-chat-completions.json`) y el compose ya apunta `ai.groq.url`
(`AI_GROQ_URL`) a `http://mocks:8080/openai/v1/chat/completions`. Agregar al `.env` `AI_ENABLED=true` y
`GROQ_API_KEY=clave-ficticia-mock` (cualquier texto) y recrear el backend (`docker compose up -d backend`).
`POST /api/v1/infractions/suggestions` (rol INSPECTOR) devuelve entonces
`{"infractionType":"ILLEGAL_PARKING","aggravatingFactors":["ZONA_ESCOLAR"],"source":"AI","promptVersion":"v1"}`.
Con `docker compose stop mocks` la misma llamada responde 200 con `source: MANUAL`. Producción no cambia (URL de Groq,
modelo `openai/gpt-oss-20b`). Detalle: [docs/ia/README-ia.md](docs/ia/README-ia.md).

## Frontend (repo `transito-frontend`)

Con el backend levantado (compose) en `http://localhost:8080`:

```bash
git clone https://github.com/alejogarciabosio/transito-frontend
cd transito-frontend
npm ci
VITE_API_BASE_URL=http://localhost:8080 npm run dev     # o poner VITE_API_BASE_URL en transito-frontend/.env.local
```

Abrir http://localhost:5173 (el puerto que muestra Vite). `CORS_ALLOWED_ORIGINS` del backend debe incluir
`http://localhost:5173` (es el valor sugerido). Alternativa en contenedor: clonar el frontend al lado de este repo,
descomentar el bloque `frontend` del final de `docker-compose.yml` y abrir http://localhost:3000 (agregar
`http://localhost:3000` a `CORS_ALLOWED_ORIGINS`).

## Demo (paso a paso, todo local)

1. `cp .env.example .env`, completar con la tabla de arriba y, para la IA, `AI_ENABLED=true` y `GROQ_API_KEY=clave-ficticia-mock`.
2. `docker compose up --build -d` y esperar `curl http://localhost:8080/actuator/health` → `UP`.
3. Levantar el frontend (sección anterior) e ingresar como `inspector1` / `admin1` / `ciudadano1`.
4. **Integración con Core:** `./scripts/inyectar-street-closure.sh` → el corte aparece `REQUESTED` en `GET /api/v1/street-closures`;
   aprobarlo (`POST /api/v1/street-closures/{id}/approval`) → `streetClosureApproved` en `q.transito.inspeccion`.
5. **Patrón State (409):** volver a aprobar el mismo corte → 409 Problem Details.
6. **Broker caído sin pérdida:** `docker compose stop rabbitmq`, aprobar otro corte (200), ver `PENDING` en
   `GET /api/v1/admin/outbox`, `docker compose start rabbitmq` y ver `PUBLISHED`.
7. **IA:** como `inspector1`, `POST /api/v1/infractions/suggestions` con `{"description":"Auto en doble fila frente a la escuela"}`
   → `ILLEGAL_PARKING` + `ZONA_ESCOLAR`, `source: AI`; `docker compose stop mocks` → `source: MANUAL`.
8. **Contratos:** Swagger en http://localhost:8080/swagger-ui/index.html; WSDL en http://localhost:8080/soap/authentication?wsdl.
9. Apagar y borrar todo: `docker compose down -v`.

## Contratos: OpenAPI, WSDL y ADR

- `openapi.yaml` (raíz) y `src/main/resources/static/openapi.yaml` (servido en `/openapi.yaml`) son **idénticos** y se
  generan desde springdoc. `OpenApiStaticContractTest` falla si se desalinean de `/v3/api-docs` o de los controladores.
  Para regenerar: con el compose arriba, `curl -s http://localhost:8080/v3/api-docs > api-docs.json`, convertir a YAML
  y copiarlo a los dos archivos.
- WSDL de la fachada SOAP: [docs/contratos-integracion/authentication.wsdl](docs/contratos-integracion/authentication.wsdl)
  (exportado de `/soap/authentication?wsdl`; `SoapWsdlContractTest` verifica que coincide con el servido).
- Decisiones de arquitectura y patrones: [docs/adr/](docs/adr/README.md). HU pendientes y workaround:
  [docs/entrega-academica/hu-pendientes.md](docs/entrega-academica/hu-pendientes.md).

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
- El frontend está en el repo `transito-frontend` (ver sección "Frontend").
