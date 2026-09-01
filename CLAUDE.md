# Contexto del proyecto: Backend Tránsito

Documento de referencia para el equipo del módulo **Tránsito** (Desarrollo de Aplicaciones II - UADE, TP "Municipalidad UADE"). Pensado para pegar en un chat de Claude (u otra IA) y que tenga contexto completo del stack y cómo trabajar.

## Qué es este proyecto

Backend del módulo 7 (Tránsito, estacionamiento y seguridad vial) del TP grupal de gestión municipal. Roles en el equipo: DEV BE, DEV FE, SCRUM MASTER, DEVOPS, PO — QA/UX-UI complementa a otro rol. Alejo es QA + DevOps.

## Stack

- **Backend:** Java 17 + Spring Boot 4.1.0 (Maven)
- **Base de datos:** PostgreSQL en Supabase, modo **Session Pooler** (puerto 5432 — no usar Transaction Pooler/6543, Hibernate se cuelga con ese modo)
- **Frontend:** todavía sin definir
- **Contenedores:** Docker (`Dockerfile` multi-stage) + `docker-compose.yml`
- **CI/CD:** GitHub Actions + Jacoco (cobertura mínima 85% en líneas)
- **Hosting:** Railway (plan Hobby, facturación por uso)
- **Repo:** `github.com/alejogarciabosio/transito-backend` (privado)

## Estructura de ramas

- **`main`** → entorno de **producción** en Railway. Protegida: PR obligatorio + debe pasar el check de CI. Sin aprobación requerida (equipo chico, un dev por rol).
- **`develop`** → entorno de **test** en Railway. Sin protección, se sube directo.
- Flujo: rama propia → PR a `develop` → una vez probado → PR de `develop` a `main`.

## Cómo levantar el proyecto en local

1. Clonar el repo
2. Pedirle a Alejo el archivo `.env` por canal privado (no está en el repo, estas 4 variables):
   - `DB_URL=jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:5432/postgres?prepareThreshold=0`
   - `DB_USER=postgres.mkesbhwkhoxiryejwrcj`
   - `DB_PASSWORD=` ← **pedir a Alejo directamente, no está en este doc**
   - `SPRING_PROFILES_ACTIVE=dev`
3. Tener Docker Desktop instalado y corriendo
4. `docker compose up`
5. Verificar: `curl http://localhost:8080/actuator/health` → debería devolver `{"status":"UP"}`

## Cosas que te van a trabar si no las sabés (gotchas ya resueltos)

- **Wifi de la facultad bloquea el puerto 5432** (Postgres). Si se cuelga conectando a la base y no tira error, cambiar a datos móviles.
- El JDBC URL necesita el prefijo `jdbc:` y el parámetro `?prepareThreshold=0` al final.
- `spring.datasource.hikari.maximum-pool-size=3` — está bajado a propósito porque Supabase free tier limita a 15 conexiones simultáneas *totales* entre todo el equipo. No lo subas sin avisar.

## Pipeline de CI (`.github/workflows/ci.yml`)

- Corre en cada Pull Request hacia `main` y `develop`
- Ejecuta `./mvnw verify` → corre los tests de JUnit + chequea cobertura con Jacoco
- Si la cobertura no llega al 85%, el build falla y el PR queda bloqueado (en `main`, por la protección de rama; en `develop` no bloquea pero sigue marcando el error)
- Las credenciales están en GitHub → Settings → Secrets and variables → Actions (no en el código)

## Railway

- Un proyecto con 2 entornos: `production` (deploy automático desde `main`) y `test` (deploy automático desde `develop`)
- Hay un límite de gasto configurado (hard limit) en la cuenta — si hace falta subirlo, avisar a Alejo primero
- Para apagar/prender un entorno manualmente: Deployments → los 3 puntos del deployment activo → **Remove** (apaga) / **Redeploy** (prende)
- URLs (confirmar en Railway, pueden cambiar si se edita el subdominio):
  - Producción: `https://transito-backend-production-28d6.up.railway.app`
  - Test: revisar en el dashboard, entorno `test` → Settings → Networking

## Pendiente (no depende de una sola persona)

- Frontend: stack sin decidir
- Broker de eventos (Kafka/RabbitMQ): depende de lo que defina el equipo Core (módulo 9) para toda la plataforma
- Tests reales de la lógica de negocio (ahora mismo la cobertura real es baja, casi no hay código de negocio todavía)

## Reglas de negocio del módulo Tránsito (del enunciado del TP)

- Toda infracción necesita evidencia o validación del inspector
- Infracción apelada no es firme hasta resolver el descargo
- Rentas solo recibe infracciones ya confirmadas (vía evento)
- Vehículo retenido no se libera sin cumplir requisitos
- Actas cerradas no se modifican
- Anulaciones requieren autorización

**Decisión de diseño ya tomada:** el descargo del ciudadano se resuelve **internamente** dentro de Tránsito (no depende de un evento externo de Ciudadanos/Expedientes) — según el resultado, se publica `InfraccionConfirmada` o `InfraccionAnulada`.
