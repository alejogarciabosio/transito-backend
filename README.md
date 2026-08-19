# Transito Backend + Frontend

Módulo de Tránsito - Desarrollo de Aplicaciones II (UADE)

## Repos

- Backend: github.com/alejogarciabosio/transito-backend (este repo)
- Frontend: github.com/alejogarciabosio/transito-frontend

Importante: cloná los dos repos en la misma carpeta padre (ej. ~/Desktop/), uno al lado del otro. El docker-compose.yml de este repo asume esa estructura para encontrar el frontend.

## Como levantar el proyecto completo

1. Clona ambos repos, uno al lado del otro:
   git clone https://github.com/alejogarciabosio/transito-backend.git
   git clone https://github.com/alejogarciabosio/transito-frontend.git
2. Pedile a Alejo el archivo .env (credenciales de Supabase) y ponelo en la raiz de transito-backend.
3. Corre: docker compose up
4. Backend disponible en http://localhost:8080
5. Frontend disponible en http://localhost:8081
6. Verificar backend: curl http://localhost:8080/actuator/health

## Requisitos

- Docker Desktop instalado y corriendo
- Importante: si estas en el wifi de la facultad y se cuelga al conectar a la base, cambiate a datos moviles.

## Stack

- Backend: Java 17 + Spring Boot (Maven)
- Frontend: React + Vite
- Base de datos: PostgreSQL (Supabase, Session Pooler)
- CI: GitHub Actions + cobertura minima 85% (Jacoco en backend, Vitest en frontend)
- Hosting: Railway, entornos test (rama develop) y production (rama main), para ambos repos

## URLs desplegadas

Backend test: transito-backend-test.up.railway.app
Backend produccion: transito-backend-prod.up.railway.app
Frontend test: transito-frontend-test.up.railway.app
Frontend produccion: transito-frontend-prod.up.railway.app

## Variables de entorno relevantes

- CORS_ALLOWED_ORIGINS (backend): URL del frontend correspondiente a cada entorno
- VITE_API_URL (frontend): URL del backend correspondiente a cada entorno. Ojo: en Vite esta variable se compila dentro del codigo en el build, cambiarla en Railway requiere un redeploy manual para que tenga efecto.
