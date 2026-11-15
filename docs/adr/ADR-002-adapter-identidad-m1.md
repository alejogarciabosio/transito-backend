# ADR-002 — Adapter + Strategy para la identidad de M1 (REST) y fachada SOAP propia

- **Estado:** aceptada; reescrita el 15/11/2026 (reemplaza la versión del 20/10 que hablaba de `SoapIdentityValidator` y `USERS_SOAP_ENABLED`, que ya no existen).
- **Patrones:** Adapter (GoF estructural) + Strategy seleccionada por configuración + Facade SOAP.

**Contexto.** M7 necesita validar credenciales contra M1 (Usuarios). M1 expone **REST** (`POST /auth/usuario/login`, campos `gmail`/`password`) y no emite JWT. La cátedra pide además un contrato SOAP. El login de M7 (`AuthController`, `JwtService`) no debe depender del formato ni de la disponibilidad de M1, y en desarrollo/CI no hay cuentas reales de M1.

**Decisión.**
- `IdentityValidator` expresa la necesidad propia de M7 (credenciales → `AuthenticatedUser`).
- **Adapter:** `RestIdentityValidator` traduce la respuesta REST de M1 (`actorType`, `role`, `citizenId`, `dni`) a `AuthenticatedUser`, con timeout de 5 s (`users.rest.timeout-ms`); 401 → credenciales inválidas, 403 → cuenta no habilitada, timeout/caída → 503, respuesta inválida → 502.
- **Strategy por propiedad:** `users.rest.enabled` (`USERS_REST_ENABLED`) elige la implementación con `@ConditionalOnProperty`: `true` → `RestIdentityValidator`; `false`/ausente → `StubIdentityValidator` (usuarios de prueba con contraseñas por variable `STUB_PASSWORD_*`).
- **Fachada SOAP propia:** `SoapAuthenticationController` publica `/soap/authentication` (WSDL en `?wsdl`, operación `authenticateUser`) y por dentro usa el mismo `IdentityValidator`. M1 no expone SOAP ni conoce este contrato. Se apaga con `users.soap-adapter.enabled=false`. Contrato versionado: `docs/contratos-integracion/authentication.wsdl`.

**Alternativas.** (a) Llamar a M1 desde `AuthController`: acopla el controlador al proveedor. (b) Exigir SOAP a M1 (`SoapIdentityValidator`): M1 no lo ofrece; se eliminó. (c) Perfiles de Spring en vez de propiedad: menos explícito en Railway.

**Consecuencias.** JWT y login no cambian si M1 cambia; se puede operar sin M1 (stub). En producción hoy rige el stub (`USERS_REST_ENABLED` sin definir) hasta probar con cuentas reales de M1. La fachada SOAP agrega una superficie XML (protegida contra XXE).

**Evidencia.** `auth/IdentityValidator.java`, `auth/RestIdentityValidator.java`, `auth/StubIdentityValidator.java`, `web/SoapAuthenticationController.java`, `application.properties` (`users.rest.*`, `users.soap-adapter.enabled`). Tests: `unit/RestIdentityValidatorTest`, `integration/RestIdentityContextTest`, `SecurityIdentityTests`, `integration/SecurityEvidenceTests` (SEC-XXE-01), `integration/SoapWsdlContractTest`. Mock de M1: `docker/wiremock/mappings/m1-login.json`.
