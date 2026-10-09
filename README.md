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
- Integración SOAP con Usuarios: planificada.
- Publicación real mediante broker y Transactional Outbox: planificada.
- Frontend: login integrado; pantallas restantes en etapa de prototipo mock.
