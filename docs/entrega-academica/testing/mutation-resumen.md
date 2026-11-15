# Mutation testing (PIT 1.20.3) - resumen

## Comandos
- Config del pom (perfil `mutation`, solo `service.*` y 4 clases de test): `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage -Pmutation`
- Alcance ampliado (service + model + listener, toda la suite):
  `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage -Pmutation -DtargetClasses='ar.edu.uade.transito.transito_backend.service.*,ar.edu.uade.transito.transito_backend.model.*,ar.edu.uade.transito.transito_backend.listener.*' -DtargetTests='ar.edu.uade.transito.transito_backend.*'`
- `model.*` (entidades Lombok) no genera mutantes: no hay lógica propia.

## Score
| Alcance | Antes | Después |
|---|---|---|
| Perfil del pom (`service.*`, tests acotados) | 127/299 = 42% | 200/299 = 67% |
| Ampliado (service+model+listener, toda la suite) | 267/348 = 77% | 321/348 = 92% |

Nuevos tests (solo test): `unit/MutationKillingServicesTest` (21) y `unit/MutationKillingOperationsClosuresTest` (6).

## Sobrevivientes aceptados (alcance ampliado, 27)
- `InfractionSuggestionService` (5): aritmética de latencia/logging y límites de sanitización; sin impacto funcional.
- `StreetClosureRequestedHandler.validate` (8) y `eventType()` de handlers (4): validaciones/strings cubiertos por tests de integración que PIT no atribuye; pendiente de test unitario.
- `ParkingSessionService.flagUnpaid`, `RabbitMqIncomingEventListener.receive`, `StreetClosureExpirationScheduler`, `TicketUpdatedHandler` (10): sin cobertura/ligados a infraestructura (Rabbit, scheduler).
