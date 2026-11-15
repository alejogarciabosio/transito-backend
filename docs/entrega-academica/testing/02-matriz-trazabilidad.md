# 02 - Matriz de trazabilidad HU -> criterio -> caso -> test -> resultado -> evidencia -> defecto

Fuente de las HU y criterios: Google Doc del TPO, secciones F1-F7 (33 historias de usuario). Los tests se verificaron con `git grep` sobre `main` (commit `65e330b`) el 15/11/2026.

## Leyenda de resultado y evidencia
| Código | Significado |
|---|---|
| PASA | el test existe, está en el build normal y pasó en `./mvnw verify` (EV-1) |
| FALLA | el test existe, se ejecutó y falló: defecto abierto |
| SIN EJECUTAR | el test existe pero está excluido del build (`@Tag("bug")`) y no se corrió |
| NO IMPLEMENTADO | el criterio no tiene función en el backend (verificado con `git grep`) |
| SIN COBERTURA | no hay test automatizado; se indica el caso manual propuesto (en 03) |
| NO VERIFICABLE | no pude comprobarlo con lo que hay en el repo |

| Evidencia | Qué es |
|---|---|
| EV-1 | `./mvnw -q -B verify` sobre `integration/entrega4`: 285 tests ejecutados, 0 fallos, líneas 88,5 % (`reports/i1.md`). La rama es la que hoy está en `main` |
| EV-2 | `npm run test:coverage` del frontend: 59 tests, 93,43 % de líneas (`reports/i2.md`) |
| EV-3 | E2E Playwright 7/7 en 3 corridas (`reports/i2.md`, `reports/a2.md`) |
| EV-4 | Corrida del grupo `bug` hecha por el agente D1 el 15/11/2026: `./mvnw -q -B test -Dbugs.excluded=none -Dgroups=bug -Djacoco.skip=true` -> 22 tests, 16 pasan, 4 fallan y 2 dan error de preparación del test |
| EV-5 | Prueba manual contra producción el 15/11 con M3 (accidente, infracción confirmada, corte aprobado/activado/finalizado), declarada por el equipo. Los `eventId` y capturas no están en el repo: PENDIENTE de adjuntar |

Los nombres `TC-BE-*` son los ids que ya figuran en los `@DisplayName` de los tests. Los `TC-PEND-*` son casos definidos en `03-especificacion-casos.md` que todavía no tienen test.

## F1 - Infracciones
| HU | Criterio de aceptación | Caso | Test automatizado | Resultado | Evid. | Defecto |
|---|---|---|---|---|---|---|
| HU-INF-01 | Identificar al inspector | TC-BE-INF-01 | `integration/InfractionIntegrationTest.inspectorRegisters`; `ApiContractSmokeTests.creatingInfractionUsesJwtInspectorAndReturnsLocation` | PASA | EV-1 | |
| HU-INF-01 | Patente, tipo, fecha, ubicación | TC-BE-INF-01 | `RequestValidationTests.rejectsInvalidInfractionAmountsAndIncompleteDebtorIdentification`; `SecurityEvidenceTests.massAssignmentInfraction` | PASA | EV-1 | |
| HU-INF-01 | Requerir evidencia o validación | BUG-BE-04 | `bugs/BugReproductionTest.bug04_infractionWithoutEvidence` | FALLA (alta sin evidencia da 201, se esperaba 400) | EV-4 | DEF-16 |
| HU-INF-01 | Calcular importe base automáticamente | TC-PEND-01 | ninguno: `CreateInfractionRequest` recibe `baseAmount` del cliente | NO IMPLEMENTADO | git grep | DEF-19 |
| HU-INF-01 | Agravantes o reincidencia | TC-PEND-02 | ninguno para el cálculo (el campo `aggravatingFactors` se persiste, sin cálculo) | NO IMPLEMENTADO | git grep | DEF-19 |
| HU-INF-01 | `infractionId` único | TC-BE-INF-01 | `InfractionIntegrationTest.inspectorRegisters` (Location) | PASA | EV-1 | |
| HU-INF-01 | Publica `infractionRegistered` | TC-BE-INF-01 | `InfractionIntegrationTest.inspectorRegisters` | PASA | EV-1 | |
| HU-INF-02 | Acta asociada a infracción existente | TC-BE-INF-07/08 | `InfractionIntegrationTest.detailOwnership`, `notFound` | PASA | EV-1 | |
| HU-INF-02 | Contiene datos, inspector y evidencia | TC-BE-INF-07 | `InfractionIntegrationTest.detailOwnership` (datos); referencia de evidencia sin test propio | PASA parcial | EV-1 | |
| HU-INF-02 | Acta cerrada no se modifica | TC-BE-INF-11 | `InfractionIntegrationTest.updateFinal`; `StateTransitionTests.finalInfractionCannotBeModified` | PASA | EV-1 | |
| HU-INF-03 | Solo sus propias infracciones | TC-BE-INF-06/07, SEC-BOLA-01 | `InfractionIntegrationTest.mine`, `detailOwnership`; `SecurityEvidenceTests.bolaInfractions`; front `citizen.test.jsx` "Mis infracciones lista solo las propias"; E2E-1 | PASA | EV-1, EV-2, EV-3 | |
| HU-INF-03 | Tipo, fecha, ubicación, importe, estado | TC-BE-INF-07 | `InfractionIntegrationTest.detailOwnership`; front `citizen.test.jsx` (detalle) | PASA | EV-1, EV-2 | |
| HU-INF-03 | Indicar si permite apelar | TC-PEND-03b | sin test específico | NO VERIFICABLE | | |
| HU-INF-04 | Apelación asociada a `infractionId`, identifica al ciudadano | TC-BE-INF-13/15 | `InfractionIntegrationTest.ownerAppeals`, `appealForbidden`; `InfractionServiceTest.appealMatrix` | PASA | EV-1 | |
| HU-INF-04 | Motivo y fecha | TC-BE-INF-13 | `InfractionIntegrationTest.ownerAppeals`; front `citizen.test.jsx` "presentar un descargo" | PASA | EV-1, EV-2 | |
| HU-INF-04 | Adjuntar documentación | TC-PEND-03 | ninguno: `AppealRequest` sin adjuntos (git grep) | NO IMPLEMENTADO | git grep | DEF-20 |
| HU-INF-04 | `appealId` único | TC-PEND-03 | ninguno: no existe `appealId` en `src/main` | NO IMPLEMENTADO | git grep | DEF-20 |
| HU-INF-04 | Infracción no firme mientras la apelación está pendiente | TC-BE-INF-13/14 | `InfractionIntegrationTest.ownerAppeals`, `appealInvalidState` | PASA | EV-1 | |
| HU-INF-04 | No informar a Rentas con apelación pendiente | TC-BE-INF-03 | `InfractionIntegrationTest.registerDoesNotNotifyRentas` | PASA | EV-1 | |
| HU-INF-05 | Solo usuario autorizado resuelve | TC-BE-INF-19/22 | `InfractionIntegrationTest.confirmForbidden`, `annulRules` | PASA | EV-1 | |
| HU-INF-05 | Resultado y motivo registrados | TC-BE-INF-20 | `InfractionIntegrationTest.supervisorAnnuls`; front `staff.test.jsx` "la anulación exige motivo"; E2E-3 | PASA | EV-1, EV-2, EV-3 | |
| HU-INF-05 | Aceptada -> anulada, registrada internamente | TC-BE-INF-21 | `InfractionIntegrationTest.annulAppealed` | PASA | EV-1 | |
| HU-INF-05 | Rechazada -> firme | TC-BE-INF-17 | `InfractionIntegrationTest.confirmAppealed` | PASA | EV-1 | |
| HU-INF-05 | Anulada no se informa a Rentas | TC-BE-INF-20 | `InfractionIntegrationTest.supervisorAnnuls` (publica solo `infractionAnulled`, evento interno) | PASA | EV-1 | |
| HU-INF-06 | Sin apelación pendiente y firme | TC-BE-INF-16/18 | `InfractionIntegrationTest.confirmPending`, `confirmInvalid`; `InfractionServiceTest.confirmMatrix` | PASA | EV-1 | |
| HU-INF-06 | `debtorId` y `debtorIdType` | TC-BE-INF-01 | `RequestValidationTests.rejectsInvalidInfractionAmountsAndIncompleteDebtorIdentification` | PASA | EV-1 | |
| HU-INF-06 | Datos de infracción, vehículo, importe, inspector, ubicación | TC-BE-INF-16 | `InfractionIntegrationTest.confirmPending` (sobre `infractionConfirmed` completo) | PASA | EV-1 | |
| HU-INF-06 | Publica `infractionConfirmed`; no envía `infractionAnulled` | TC-BE-INF-16, TC-BE-INF-18 | `confirmPending`, `confirmInvalid` (no republica); integración real con M3/Rentas | PASA (automático); real con M3 el 15/11 | EV-1, EV-5 | |

## F2 - Cortes de calle
| HU | Criterio | Caso | Test automatizado | Resultado | Evid. | Defecto |
|---|---|---|---|---|---|---|
| HU-COR-01 | Recibir solicitudes de Obras y Ambiente | TC-BE-SC-01 | `integration/StreetClosureIntegrationTest.createdFromEvent`; `listener/handler/StreetClosureRequestedHandlerTest` | PASA | EV-1 | |
| HU-COR-01 | Identificador y módulo de origen | TC-BE-SC-01/02 | `createdFromEvent`, `unknownModule` | PASA | EV-1 | |
| HU-COR-01 | Datos necesarios para evaluar | TC-BE-SC-01 | `StreetClosureRequestedHandlerTest.rejectsRequestsWithoutTheRequiredWorksPayloadFields`, `keepsTimeAndZoneOfRequestedWindow` | PASA | EV-1 | |
| HU-COR-01 | Evento duplicado no se procesa dos veces | TC-BE-EVT-D | `listener/IncomingEventDispatcherTest.discardsEventAlreadyProcessed` | PASA | EV-1 | |
| HU-COR-02 | Consultar la solicitud | TC-BE-SC-08 | `StreetClosureIntegrationTest.permissions` | PASA | EV-1 | |
| HU-COR-02 | Aprobar publica `streetClosureApproved`; condiciones | TC-BE-SC-03 | `StreetClosureIntegrationTest.approve`; E2E-7; real con M3 | PASA | EV-1, EV-3, EV-5 | |
| HU-COR-02 | Rechazar con motivo publica `streetClosureRejected` | TC-BE-SC-05 | `StreetClosureIntegrationTest.reject`; `AdditionalServiceCoverageTests.streetClosureCanBeRejectedOnlyWhileRequested`; front `staff.test.jsx` | PASA | EV-1, EV-2 | |
| HU-COR-02 | Conserva referencia a la solicitud y al módulo | TC-BE-SC-03/05 | `approve`, `reject` | PASA | EV-1 | |
| HU-COR-02 | Transición no permitida -> 409 | TC-BE-SC-07 | `StreetClosureIntegrationTest.invalidTransitions`; `OtherServicesTest.closureApproveOnlyFromRequested`; `StateTransitionTests.streetClosureCannotActivateBeforeApproval` | PASA | EV-1 | |
| HU-COR-02 | Aprobar sin desvíos devuelve 200 | BUG-BE-02 | `BugReproductionTest.bug02_approveWithoutDetours` | ERROR de preparación del test ("affectedSections es obligatorio"): test desactualizado, no hay veredicto sobre el producto | EV-4 | DEF-18 |
| HU-COR-03 | Calle afectada, fechas, rutas alternativas, condiciones | TC-BE-SC-03/04 | `approve`, `approvalCanSetAuthorizedWindow`; `ApiWorkflowIntegrationTests.streetClosureCompletesItsHttpLifecycle` | PASA | EV-1 | |
| HU-COR-03 | Desvíos asociados al corte | TC-BE-SC-03 | `approve`; `MapperCoverageTests.mapsParkingSessionAndStreetClosureResponses` | PASA | EV-1 | |
| HU-COR-04 | Solo activar un corte aprobado | TC-BE-SC-07 | `invalidTransitions`; `StateTransitionTests.streetClosureCannotActivateBeforeApproval` | PASA | EV-1 | |
| HU-COR-04 | Activar antes de `startDate` | TC-PEND-14 | ninguno: `StreetClosureService.activate` solo valida el estado | NO IMPLEMENTADO (la regla no existe) | git grep; observado en prueba real | DEF-07 |
| HU-COR-04 | Extender el período | TC-BE-SC-EXT | `StreetClosureRequestedHandlerTest.keepsTheReferenceToThePreviousRequestForAnExtension` (la prórroga llega como nuevo evento) | PASA | EV-1 | |
| HU-COR-04 | Finalizar un corte activo, fecha y hora, observaciones | TC-BE-SC-06 | `fullLifecycle`; `AdditionalServiceCoverageTests.streetClosureCompletesTheApprovedLifecycle`; E2E real con M3 | PASA | EV-1, EV-5 | |
| HU-COR-04 | Publica `streetClosureEnded` | TC-BE-SC-06 | `fullLifecycle`, `OtherServicesTest.expireDueClosures` | PASA (`causationId` es null: ver defecto) | EV-1 | DEF-08 |
| HU-COR-04 | Vencimiento automático de ventana | TC-BE-SC-U02 | `OtherServicesTest.expireDueClosures` | PASA | EV-1 | |
| HU-COR-04 | RF-COR-11: publicar `streetClosureActivated` | TC-PEND-14b | `fullLifecycle` afirma que NO se publica la activación | NO IMPLEMENTADO por diseño; a confirmar con el PO | git grep | DEF-21 |
| HU-COR-05 | Recibir servicios urbanos, podas, riesgo de árboles, obras | TC-PEND-04 | ninguno: no hay handlers para esos eventos en `listener/handler/` | NO IMPLEMENTADO | git grep | DEF-20 |
| HU-COR-05 | Identificar zona; disponible para evaluar | TC-PEND-04 | ninguno | NO IMPLEMENTADO | git grep | DEF-20 |

## F3 - Operativos
| HU | Criterio | Caso | Test automatizado | Resultado | Evid. | Defecto |
|---|---|---|---|---|---|---|
| HU-OPE-01 | Tipo, zona, fecha y horario; `operationId` único | TC-BE-OP-01 | `integration/TrafficOperationIntegrationTest.create_publishesEvent`; `ApiWorkflowIntegrationTests.operationCompletesItsHttpLifecycle` | PASA | EV-1 | |
| HU-OPE-01 | Publica `trafficOperationCreated` | TC-BE-OP-01 | `create_publishesEvent` | PASA | EV-1 | |
| HU-OPE-02 | Asignar inspectores y móviles; consultarlos | TC-BE-OP-03 | `TrafficOperationIntegrationTest.update`; `ServiceBusinessRulesTests.operationPatchKeepsUnspecifiedFields` | PASA | EV-1 | |
| HU-OPE-03 | Registrar controles y resultado del control | TC-PEND-06 | ninguno: el modelo solo tiene `resolution` al cerrar | NO IMPLEMENTADO como entidad "control" | git grep | DEF-20 |
| HU-OPE-03 | Cierre con resultado; cerrado no se edita | TC-BE-OP-04/06 | `closeSetsEnd`, `closedIsFinal`; `StateTransitionTests.closedOperationCannotBeClosedAgain` | PASA | EV-1 | |
| HU-OPE-04 | Retención asociada al operativo con `originType=OPERATION` y `originId` | TC-BE-IMP-01 | `integration/VehicleImpoundmentIntegrationTest.register_publishesEvent` (usa `Payloads` con origen OPERATION); front `staff.test.jsx` "ingreso desde un operativo" | PASA | EV-1, EV-2 | |
| HU-OPE-04 | Publica `vehicleImpounded` | TC-BE-IMP-01 | `register_publishesEvent` | PASA | EV-1 | |
| (soporte) | Ticket de M2 derivado a M7 crea operativo | TC-BE-EVT-01..03 | `TicketEventListenerIntegrationTest.*`; `unit/TicketUpdatedHandlerTest.*` | PASA | EV-1 | |

## F4 - Accidentes
| HU | Criterio | Caso | Test automatizado | Resultado | Evid. | Defecto |
|---|---|---|---|---|---|---|
| HU-ACC-01 | Fecha, hora, ubicación, inspector, `accidentId` | TC-BE-ACC-01 | `integration/RoadAccidentIntegrationTest.register_publishesEvent`; `SecurityIdentityTests.accidentRegistrationUsesAuthenticatedInspectorInsteadOfEntityValue`; `ApiContractSmokeTests.creatingAccidentIgnoresSpoofedInspectorAndUsesJwtIdentity` | PASA | EV-1 | |
| HU-ACC-01 | Disponible para continuar su gestión | TC-BE-ACC-03 | `RoadAccidentIntegrationTest.updatePreliminary` | PASA | EV-1 | |
| HU-ACC-02 | Personas, lesión, vehículos, daños, infraestructura | TC-BE-ACC-01/01c | `register_publishesEvent`; `register_withInfrastructureDamageWithoutDescription_isRejected`; `RequestValidationTests.requiresDamageDescriptionWhenAccidentDamagesInfrastructure` | PASA | EV-1 | |
| HU-ACC-02 | `roadAccidentRegistered` solo con daño de infraestructura | TC-BE-ACC-01/01b | `register_publishesEvent`, `register_withoutInfrastructureDamage_doesNotPublishEvent`; real con M3 | PASA | EV-1, EV-5 | |
| HU-ACC-03 | Acciones asociadas; cierre solo al finalizar atención | TC-BE-ACC-03/04/05 | `updatePreliminary`, `verifiedIsFinal`, `preliminaryStatusIgnored`; `StateTransitionTests.verifiedAccidentCannotBeModified` | PASA | EV-1 | |
| HU-ACC-03 | Conservar historial de la actuación | TC-PEND-03c | sin test: no hay entidad de historial | NO VERIFICABLE | | |
| HU-ACC-04 | Ticket que originó la actuación; asociación; actualizaciones de M2 | TC-BE-EVT-01..03 | `TicketEventListenerIntegrationTest.routedToTransito`, `otherArea`, `otherUpdateTypes`; `TicketUpdatedHandlerTest.*` | PASA | EV-1 | |
| HU-ACC-04 | Informar evolución a M2 (`updateTicketStatus`) | TC-BE-EVT-01 | `TicketUpdatedHandlerTest.routedTicketForM7CreatesOperationAndPublishesStartedUsingCoreEnvelope`; `TrafficOperationIntegrationTest.closeSetsEnd` | PASA | EV-1 | |
| (soporte) | Accidente propio del ciudadano (`/mine`) | TC-BE-ACC-CIU-01/02 | `RoadAccidentIntegrationTest.mine`, `mine_asVehicleOwnerOnly`; `SecurityEvidenceTests.bolaAccidents` | PASA | EV-1 | |

## F5 - Vehículos retenidos
| HU | Criterio | Caso | Test automatizado | Resultado | Evid. | Defecto |
|---|---|---|---|---|---|---|
| HU-VEH-01 | Patente, motivo, depósito, fecha de ingreso, `impoundmentId` | TC-BE-IMP-01 | `VehicleImpoundmentIntegrationTest.register_publishesEvent`; `AdditionalServiceCoverageTests.vehicleImpoundmentCanBeRegisteredUpdatedAndReleased` | PASA | EV-1 | |
| HU-VEH-01 | `originType` y `originId` | TC-BE-IMP-01 | `register_publishesEvent` (assert sobre `originType`) | PASA | EV-1 | |
| HU-VEH-01 | Publica `vehicleImpounded` | TC-BE-IMP-01 | `register_publishesEvent` | PASA | EV-1 | |
| HU-VEH-02 | Costo de acarreo y estadía calculado según permanencia | TC-PEND-07 | ninguno: `towingAndStorageFee` llega en el pedido de liberación; no hay cálculo | NO IMPLEMENTADO | git grep | DEF-19 |
| HU-VEH-03 | Ver requisitos cumplidos y pendientes | TC-BE-IMP-04 | `VehicleImpoundmentIntegrationTest.release` (requisitos verificados) | PASA parcial: no hay vista de pendientes en el backend | EV-1 | |
| HU-VEH-03 | Requisito pendiente impide liberar | TC-BE-IMP-04, BUG-BE-06 | `ServiceBusinessRulesTests.vehicleReleaseRequiresPaymentReferenceWhenThereIsAFee`; `RequestValidationTests.requiresPaymentReferenceForPaidVehicleRelease`; `bug06_releaseWithoutRequirements` | PASA (reglas); `bug06` SIN EJECUTAR en el build, pasó en EV-4 | EV-1, EV-4 | |
| HU-VEH-03 | Validar pago de Rentas / reversión de pago | TC-PEND-15 | `unit/InfractionPaymentEventServiceTest.*` (registra pago, saldo, reverso); efecto sobre la liberación no probado | PASA parcial | EV-1 | |
| HU-VEH-04 | Liberar con todo cumplido; fecha; requisitos; `paymentReference`; `releasedBy` | TC-BE-IMP-04 | `VehicleImpoundmentIntegrationTest.release` (`releasedBy` desde el token); front `staff.test.jsx` "libera con requisitos y pago" | PASA | EV-1, EV-2 | |
| HU-VEH-04 | Liberado no se libera de nuevo | TC-BE-IMP-05/U01 | `releasedIsFinal`; `OtherServicesTest.impoundmentReleaseTwice` | PASA | EV-1 | |
| HU-VEH-04 | Publica `vehicleReleased` | TC-BE-IMP-04 | `release` | PASA | EV-1 | |

## F6 - Estacionamiento medido
| HU | Criterio | Caso | Test automatizado | Resultado | Evid. | Defecto |
|---|---|---|---|---|---|---|
| HU-EST-01 | Zonas, horarios y tarifa asociada | TC-BE-PZ-01..05, TC-BE-PT-01..05 | `integration/ParkingIntegrationTest.create`, `update`, `logicalDelete`, `createAndQuery`, `deleteWithOpenSessions`; `ApiWorkflowIntegrationTests.parkingZoneAndTariffCompleteAdministrativeLifecycle`; `RequestValidationTests.rejectsParkingTariffWithInvertedValidityRange` | PASA | EV-1 | |
| HU-EST-02 | Vehículo, zona, inicio, tarifa aplicada | TC-BE-PS-01 | `ParkingIntegrationTest.start`; `SecurityEvidenceTests.massAssignmentSession` | PASA | EV-1 | |
| HU-EST-02 | Disponible para control posterior | TC-BE-PS-02/03 | `ParkingIntegrationTest.queries`, `close` | PASA | EV-1 | |
| HU-EST-03 | Detectar falta de pago o vencimiento | TC-PEND-08 | ninguno: no hay detección automática; solo el inspector marca impago | NO IMPLEMENTADO | git grep | DEF-20 |
| HU-EST-03 | Incumplimiento no genera infracción sola | TC-BE-PS-05 | `ParkingIntegrationTest.flagUnpaidRules` | PASA | EV-1 | |
| HU-EST-04 | Validar o descartar el incumplimiento | TC-PEND-09 | ninguno: `ParkingSessionController` solo tiene `unpaid-flag` | NO IMPLEMENTADO (no existe "descartar") | git grep | DEF-20 |
| HU-EST-05 | Infracción asociada, inspector identificado | TC-BE-PS-05, BUG-BE-01 | `BugReproductionTest.bug01_flagUnpaidAlways500` | SIN EJECUTAR en el build; PASA en EV-4 (el 500 ya no se reproduce) | EV-4 | DEF-11 (cerrado) |
| HU-EST-05 | No generar dos infracciones por la misma sesión | TC-BE-PS-05 | `ServiceBusinessRulesTests.parkingSessionCannotGenerateMoreThanOneUnpaidInfraction`; `bug15_flagUnpaidTwice` | PASA | EV-1, EV-4 | |
| HU-EST-05 | Sesión ajena no accesible por otro ciudadano (BOLA) | SEC-BOLA-04 | `SecurityEvidenceTests.bolaParkingSession` | FALLA: otro ciudadano lee (200) y cierra (200) la sesión | EV-4 | DEF-15 |

## F7 - Consultas e indicadores
| HU | Criterio | Caso | Test automatizado | Resultado | Evid. | Defecto |
|---|---|---|---|---|---|---|
| HU-IND-01 | Consultar infracciones, operativos, accidentes, retenidos | TC-BE-INF-05, TC-BE-OP-02, TC-BE-ACC-02, TC-BE-IMP-02 | `InfractionIntegrationTest.listByRole`; `TrafficOperationIntegrationTest.permissions`; `RoadAccidentIntegrationTest.permissions`; `VehicleImpoundmentIntegrationTest.ownership`; front `staff.test.jsx` | PASA | EV-1, EV-2 | |
| HU-IND-01 | Filtrar infracciones por tipo, estado, fecha y zona | TC-PEND-10 | backend: ninguno (`InfractionController` no recibe filtros). Frontend: `staff.test.jsx` "filtra por estado" (filtro de pantalla, solo por estado) | NO IMPLEMENTADO en el backend; parcial en el frontend | git grep, EV-2 | DEF-20 |
| HU-IND-01 | Información coincide con los registros | TC-BE-INF-05 | `listByRole` | PASA | EV-1 | |
| HU-IND-02 | Indicadores de infracciones, siniestralidad, retenidos, ocupación, cumplimiento de pago | TC-PEND-11 | backend: ninguno (sin endpoint de indicadores). Frontend: `staff.test.jsx` "panel con indicadores reales" | NO VERIFICABLE: no verifiqué cómo calcula el frontend los indicadores ni si cubre ocupación y cumplimiento de pago | EV-2 | DEF-20 |
| HU-IND-03 | Tablero con principales indicadores, claro y organizado | TC-PEND-11 | front `staff.test.jsx` (panel con indicadores), `citizen.test.jsx` "contadores con datos reales"; axe 0 violaciones | PASA parcial (contenido y datos); claridad visual NO VERIFICABLE | EV-2 | |

## Transversales (seguridad, integración, IA)
| Requisito | Caso | Test automatizado | Resultado | Evid. | Defecto |
|---|---|---|---|---|---|
| Login válido / inválido (HU-03 de la Parte B, BR-03) | TC-BE-AUTH, SEC-01..03 | `AuthAndSecurityIntegrationTest.loginOk`, `loginFails`; `ApiContractSmokeTests.invalidCredentialsReturnProblemDetails`; E2E-5 | PASA | EV-1, EV-3 | DEF-01 (stub activo en producción) |
| BR-03: M1 no responde en 5 s -> 503 sin token (el Google Doc dice "sin prueba": hoy SÍ existe) | TC-BE-AUTH-M1 | `unit/RestIdentityValidatorTest.timeout`, `connectionRefused`, `errorStatuses`, `invalidResponses`; `integration/RestIdentityContextTest.loginTimeout`, `loginUpstreamError`, `loginConnectionRefused`, `loginAuthFalse` | PASA (con M1 simulado). Contra M1 real: NO VERIFICABLE (adaptador sin activar en Railway) | EV-1 | DEF-10 |
| Roles y permisos por endpoint (BFLA) | SEC-BFLA | `SecurityEvidenceTests.forbiddenRolesAndAnonymous`; `ApiWorkflowIntegrationTests.citizenCannotCreateInfraction` | PASA | EV-1 | |
| Evento duplicado / inválido en consumo | TC-BE-EVT-D | `IncomingEventDispatcherTest` (4 tests: nuevo, ya procesado, sin handler, fallo del handler no se registra) | PASA | EV-1 | |
| Idempotency-Key, ETag/If-Match/412, Problem Details | REST-IDEM-01, REST-ETAG-01/02, REST-ERR-01 | `SecurityEvidenceTests.idempotency`, `etag`, `notModified`, `problemDetails`; `EtagServerTest.optimisticConcurrency` | PASA | EV-1 | |
| Registro de eventos publicados (regla 9.3 del TP) | TC-PEND-13 | ninguno | NO IMPLEMENTADO | git grep | DEF-04 |
| Health con broker caído | TC-BE-HEALTH | `integration/HealthWithBrokerDownTest.healthIsUpFastWithBrokerDown` | PASA | EV-1 | DEF-02 (cerrado) |
| IA: sugerencia, apagada por defecto, modo manual, inyección, PII | TC-IA-01..08 (en 03) | `integration/InfractionSuggestionIntegrationTest.*`; `InfractionSuggestionDisabledIntegrationTest.*`; `ai/*Test` | PASA con proveedor simulado; con Groq real: PENDIENTE | EV-1 | |
| Frontend IA (carga, error, manual) | TC-IA-09 | `suggestions.test.jsx` (8 tests) | PASA | EV-2 | |

## Resumen de cobertura (contado a mano sobre las tablas de arriba)
- 33 HU de F1-F7: todas tienen al menos una fila. Ninguna HU queda sin caso: las que no tienen test tienen un caso `TC-PEND-*` en 03.
- HU con algún criterio sin implementar o sin test (marcadas arriba): HU-INF-01, HU-INF-03 (un criterio), HU-INF-04, HU-COR-04 (activación temprana, evento de activación), HU-COR-05, HU-OPE-03, HU-ACC-03 (historial), HU-VEH-02, HU-VEH-03 (parcial), HU-EST-03, HU-EST-04, HU-IND-01, HU-IND-02, HU-IND-03 (parcial).
- Criterios con test que FALLA hoy: SEC-BOLA-04 (sesión de estacionamiento), SEC-DATA-01 (DNI en el JWT), BUG-BE-04 (evidencia), BUG-BE-09a (ver DEF-17).
