# Transito Backend

Backend del módulo de Tránsito - Desarrollo de Aplicaciones II (UADE)

## Cómo levantar el proyecto

1. Pedile a Alejo el archivo `.env` (tiene las credenciales de la base) y ponelo en la raíz de este proyecto.
2. Corré: `docker compose up`
3. El backend queda disponible en `http://localhost:8080`
4. Verificar que anda: `curl http://localhost:8080/actuator/health` → debería devolver `{"status":"UP"}`

## Requisitos

- Docker Desktop instalado y corriendo
- **Importante:** si estás en el wifi de la facultad y se cuelga al conectar a la base, cambiate a datos móviles — ese wifi bloquea el puerto de Postgres.

## Stack

- Java 17 + Spring Boot
- PostgreSQL (Supabase, Session Pooler)
