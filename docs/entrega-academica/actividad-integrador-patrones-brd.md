# Actividad integradora - Patrones, ADR, SOLID y BRD

**Desarrollo de Aplicaciones II - Grupo 9**  
**Módulo:** M7 - Tránsito, Estacionamiento y Seguridad Vial  
**Fecha:** 20 de octubre de 2026

## 1. Diagrama de estados: Corte de calle

El agregado StreetClosure nace al consumir streetClosureRequested de Obras o Ambiente. No existe alta REST directa. StreetClosureService controla las transiciones y una transición no permitida devuelve 409 Conflict.

~~~mermaid
stateDiagram-v2
    [*] --> REQUESTED: streetClosureRequested (M3/M6)
    REQUESTED --> APPROVED: Administrador aprueba
    REQUESTED --> REJECTED: Administrador rechaza
    APPROVED --> ACTIVE: Inspector activa
    APPROVED --> EXPIRED: vence sin activarse
    ACTIVE --> FINISHED: Inspector finaliza
    ACTIVE --> FINISHED: vence fin autorizado
    REJECTED --> [*]
    EXPIRED --> [*]
    FINISHED --> [*]
~~~

| Estado | Transiciones permitidas | Resultado |
|---|---|---|
| REQUESTED | APPROVED, REJECTED | Respuesta a Obras/Ambiente. |
| APPROVED | ACTIVE, EXPIRED | El fin autorizado gobierna la operación. |
| ACTIVE | FINISHED | Finalización manual o automática. |
| REJECTED, EXPIRED, FINISHED | Ninguna | Estados terminales. |

Al aprobar se publica streetClosureApproved. streetClosureActivated es interno de M7. Al finalizar un corte activo se publica streetClosureEnded.

## 2. Patrones aplicados y ADR

### ADR-001 - GRASP Controller para casos de uso REST

**Estado:** Aceptada - 20/10/2026  
**Patrón:** GRASP Controller  
**Componentes:** StreetClosureController, InfractionController y servicios de dominio.

**Contexto.** La API debe recibir HTTP, controlar permisos y aplicar reglas de negocio sin mezclar transporte y dominio.

**Decisión.** Los controladores reciben DTOs, aplican @PreAuthorize, delegan al servicio y devuelven respuestas. Por ejemplo, StreetClosureController delega aprobar, rechazar, activar y finalizar en StreetClosureService.

**Consecuencias.** Los endpoints son delgados y testeables; las reglas se reutilizan desde REST y tareas automáticas. Se aceptan más clases a cambio de menor acoplamiento.

**Alternativa descartada.** Cambiar estados, persistir y publicar eventos dentro de cada controlador. Se descartó por duplicar reglas y mezclar responsabilidades.

### ADR-002 - Adapter para identidad externa

**Estado:** Aceptada - 20/10/2026  
**Patrón:** Adapter (GoF estructural)  
**Componentes:** IdentityValidator, SoapIdentityValidator, StubIdentityValidator.

**Contexto.** M7 necesita validar credenciales, pero M1 ofrece SOAP validateUser. El login de M7 no debe depender de XML, HTTP ni de M1.

**Decisión.** IdentityValidator expresa la necesidad propia de M7. SoapIdentityValidator adapta dni, password, user_id, role y auth a AuthenticatedUser. StubIdentityValidator cubre desarrollo y se selecciona por USERS_SOAP_ENABLED.

**Consecuencias.** JWT y login no dependen de SOAP; timeout de cinco segundos, SOAP Fault y respuestas inválidas se traducen a 503 o 502. La URL final y prueba bilateral con M1 siguen pendientes.

**Alternativa descartada.** Invocar SOAP desde AuthController. Se descartó por acoplar el controlador al proveedor y dificultar pruebas o reemplazos.

### ADR-003 - Observer / Publish-Subscribe para integración

**Estado:** Aceptada - 20/10/2026  
**Patrón:** Observer / Publish-Subscribe  
**Componentes:** EventPublisher, RabbitMqEventPublisher, LoggingEventPublisher e IncomingEventDispatcher.

**Contexto.** Rentas debe conocer una infracción confirmada sin bloquear la respuesta de M7. M7 también recibe eventos de Obras, Ambiente y Rentas.

**Decisión.** Los servicios publican EventEnvelope a través de EventPublisher. En Core, RabbitMqEventPublisher publica a muni.inbox; en desarrollo se usa LoggingEventPublisher. Los entrantes se despachan por IncomingEventDispatcher y se deduplican por eventId en processed_events.

**Consecuencias.** M7 se desacopla de otros módulos. infractionConfirmed se emite al confirmar y streetClosureEnded al terminar un corte activo. La entrega real depende de Core; Transactional Outbox sigue como evolución.

**Alternativa descartada.** Llamar HTTP a Rentas durante la confirmación. Se descartó porque una caída de M5 impediría completar el caso de uso local.

## 3. Auditoría SOLID

### Hallazgo: SRP parcialmente vulnerado en StreetClosureService

**Principio.** Single Responsibility Principle: una clase debe tener una razón principal para cambiar.

**Evidencia real.** StreetClosureService actualmente reúne cuatro responsabilidades:

1. Política de transiciones: approve, reject, activate y finish.
2. Persistencia con StreetClosureRepository.
3. Construcción y publicación de payloads de integración.
4. Vencimiento automático de cortes activos y aprobados.

Un cambio de contrato Core, política de vencimiento o transición modifica la misma clase.

**Antes:**

~~~
StreetClosureService
  - valida estados
  - persiste StreetClosure
  - compone/publica payloads M3/M6
  - calcula vencimientos automáticos
~~~

**Refactor propuesto:**

~~~
StreetClosureService                -> coordina caso de uso y persistencia
StreetClosureStatePolicy            -> valida transiciones y terminalidad
StreetClosureEventPublisher         -> compone/publica eventos M3/M6
StreetClosureExpirationService      -> resuelve cierres y expiraciones vencidas
~~~

**Resultado esperado.** Las pruebas de transición no requieren RabbitMQ, las pruebas de payload no requieren base de datos y una modificación del contrato Core no toca la política de estado. Es un refactor propuesto para la próxima iteración; no se presenta como código ya realizado.

## 4. BRD - Requisitos de negocio

### 4.1 Índice

1. Objetivo y alcance
2. Actores y reglas
3. Requisitos de negocio
4. Métricas de éxito
5. Priorización MoSCoW
6. Dependencias y riesgos

### 4.2 Objetivo y actores

M7 gestiona tránsito, estacionamiento y seguridad vial. Este BRD prioriza identidad de Usuarios, cortes solicitados por Obras/Ambiente y deuda de Rentas.

| Actor | Responsabilidad |
|---|---|
| CIUDADANO | Consulta recursos propios y presenta descargos. |
| INSPECTOR | Registra infracciones, accidentes y retenciones; activa/finaliza cortes. |
| ADMINISTRADOR | Confirma/anula infracciones, decide cortes y administra estacionamiento. |
| M1 Usuarios | Valida identidad de forma síncrona. |
| M3 Obras / M6 Ambiente | Solicitan cortes. |
| M5 Rentas | Procesa infracciones confirmadas. |

Reglas transversales: JWT para permisos, titularidad para ciudadanos, envelope Core 1.0 para eventos externos e idempotencia de mensajes entrantes por eventId.

### 4.3 Requisitos y prioridades

| ID | Requisito | Criterio de aceptación | Prioridad |
|---|---|---|---|
| BR-01 | Gestionar el ciclo de corte solicitado por M3/M6. | Pasa de REQUESTED a APPROVED/REJECTED; sólo aprobado se activa; activo vencido finaliza y publica streetClosureEnded; aprobado no activado expira. | MUST |
| BR-02 | Informar a Rentas la infracción confirmada. | Al confirmar desde PENDING_APPEAL o APPEALED, persiste CONFIRMED y emite exactamente un infractionConfirmed con contrato Core. | MUST |
| BR-03 | Validar identidad antes de emitir JWT. | auth=true emite JWT; auth=false devuelve 401; timeout/Fault impide acceso y devuelve 503/502. | SHOULD |

### 4.4 Métricas de éxito

| Métrica | Objetivo | Evidencia |
|---|---|---|
| Transiciones inválidas de corte aceptadas | 0 | Tests de integración y respuestas 409. |
| infractionConfirmed duplicados por confirmación | 0 | Captura de EventPublisher por caso de uso. |
| Tiempo de fallo de Usuarios indisponible | Máximo 5 segundos | Prueba de timeout SOAP. |
| Eventos entrantes reprocesados | 0 | processed_events y logs de deduplicación. |

### 4.5 Dependencias y riesgos

- M1 debe confirmar URL y habilitar prueba SOAP real.
- Core debe entregar contraseña AMQPS, habilitar IP de salida y corregir streetClousureEnded por streetClosureEnded.
- Core/M5 deben confirmar publicación y suscripción de infractionConfirmed.
- Sin Transactional Outbox subsiste el riesgo de persistir dominio y fallar luego la publicación.
