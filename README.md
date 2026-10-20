# Tránsito Backend

Backend del módulo de Tránsito, Estacionamiento y Seguridad Vial del trabajo práctico de Desarrollo de Aplicaciones II (UADE).

## API

- URL local: `http://localhost:8080`
- Prefijo REST: `/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Contrato OpenAPI: `http://localhost:8080/openapi.yaml`
- Health check: `http://localhost:8080/actuator/health`

La documentación consolidada de la entrega está en `docs/actividad-api-rest-entrega.md`.

## Ejecución con Docker

Crear un archivo `.env` en la raíz con las variables entregadas por el equipo:

```dotenv
DB_URL=jdbc:postgresql://host:5432/database
DB_USER=usuario
DB_PASSWORD=contraseña
JWT_SECRET=un-secreto-de-al-menos-32-bytes
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Luego ejecutar:

```bash
docker compose up --build
```

## Verificación local

Los tests usan una base H2 en memoria y no requieren credenciales externas:

```bash
./mvnw test
./mvnw package
```

En Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

## Estado de integraciones

- Autenticación JWT y autorización por roles: implementadas.
- Proveedor local de identidad: implementado para desarrollo.
- Integración SOAP con Usuarios: cliente `validateUser` implementado; se habilita con `USERS_SOAP_ENABLED=true` y `USERS_SOAP_URL`.
- RabbitMQ con Core: implementado y configurable; queda habilitarlo con las credenciales reales de Core.
- Consumo de eventos: cortes de Obras/Ambiente y pagos de Rentas implementados con idempotencia persistida.
- Cierre automático de cortes: implementado al vencer el horario autorizado.
- Frontend: login integrado; pantallas restantes en etapa de prototipo mock.

### RabbitMQ de Core

La integración permanece desactivada por defecto. Para el ambiente compartido de Core,
una vez que Core habilite la IP pública de salida y entregue la contraseña por un canal
privado, configurar estas variables fuera del repositorio:

```dotenv
CORE_RABBITMQ_ENABLED=true
CORE_RABBITMQ_HOST=core-uade-2026.duckdns.org
CORE_RABBITMQ_PORT=5671
CORE_RABBITMQ_VHOST=core
CORE_RABBITMQ_USERNAME=module_transito
CORE_RABBITMQ_PASSWORD=<entregada-por-core>
CORE_RABBITMQ_TLS_ENABLED=true
```

M7 usa AMQPS con validación del certificado y del hostname activadas. No requiere
certificado de cliente. La cola `q.transito` se verifica mediante acceso pasivo: M7 no
crea colas, bindings ni suscripciones en Core.
