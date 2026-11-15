# ADR-005 — Patrón State para el ciclo de vida de cortes de calle e infracciones

- **Estado:** aceptada (14/11/2026, DEF-13). Diseño y diagramas: [docs/arquitectura/patron-state.md](../arquitectura/patron-state.md).
- **Patrón:** State (GoF de comportamiento).

**Contexto.** `StreetClosure` (REQUESTED → APPROVED → ACTIVE → FINISHED, más REJECTED/EXPIRED) e `Infraction` (PENDING_APPEAL → APPEALED → CONFIRMED/ANNULLED) tenían las reglas de transición repartidas en `if`/`switch` dentro de los servicios. Una transición inválida debe responder **409 Problem Details** de forma uniforme, y las actas firmes no se modifican.

**Decisión.** Una interfaz por entidad (`StreetClosureState`, `InfractionState`) cuyas operaciones (`approve`, `reject`, `activate`, `finish`, `expire`; `appeal`, `confirm`, `annul`, `requireEditable`) lanzan por defecto `InvalidStateTransitionException`. Una clase por estado sobrescribe solo lo que admite y devuelve el estado siguiente. `StreetClosureStates.of(...)` / `InfractionStates.of(...)` resuelven el objeto desde el enum persistido. `GlobalExceptionHandler` traduce la excepción a 409.

**Alternativas.** (a) `switch` sobre el enum en el servicio: crece con cada estado y es fácil olvidar un caso. (b) Spring State Machine: dependencia pesada para 6 y 4 estados. (c) Guardar la clase de estado en la base: obligaba a migrar el esquema (congelado).

**Consecuencias.** Sin cambios de esquema ni de contrato REST; la matriz estado × operación se testea completa; agregar un estado es agregar una clase. Más clases pequeñas.

**Evidencia.** `model/state/*` (p. ej. `RequestedState`, `ActiveState`, `InfractionConfirmedState`), `exception/InvalidStateTransitionException.java`, `web/GlobalExceptionHandler.java`. Tests: `model/state/StreetClosureStateTest`, `model/state/InfractionStateTest` (100 % líneas y ramas), `StateTransitionTests`, `integration/SecurityEvidenceTests.problemDetails` (409).
