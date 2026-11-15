# 02 - Matriz de trazabilidad HU -> criterio -> caso -> test -> resultado -> evidencia -> defecto

Fuente de las HU y criterios: Google Doc del TPO, secciones F1-F7: **31 historias de usuario** (INF 6, COR 5, OPE 4, ACC 4, VEH 4, EST 5, IND 3), contadas por id en las tablas de abajo; HU-COR-05 está en alcance aunque no está implementada. El borrador anterior decía "33 HU": era un error de conteo. Los tests se verificaron con `git grep` sobre `main` (commit `57b2765`, estado final desplegado) el 15/11/2026.

## Leyenda de resultado y evidencia
| Código | Significado |
|---|---|
| PASA | el test existe, está en el build normal y pasó en `./mvnw verify` (EV-1) |
| FALLA | el test existe, se ejecutó y falló: defecto abierto |
| SIN EJECUTAR | el test existe pero está excluido del build (`@Tag("bug")`); se indica el resultado de la corrida aparte (EV-4) |
| NO IMPLEMENTADO | el criterio no tiene función en el backend (verificado con `git grep`) |
| SIN COBERTURA | no hay test automatizado; se indica el caso manual propuesto (en 03) |
| NO VERIFICABLE | no pude comprobarlo con lo que hay en el repo |

| Evidencia | Qué es |
|---|---|
| EV-1 | `./mvnw -q -B verify` sobre `main` `57b2765` (15/11 ~18:43, agente D3): exit 0, **497 tests**, 0 fallos, 0 errores, 0 omitidos; líneas **93,74 %** (2007/2141), ramas 81,97 %. Coincide con el job `test` del CI de `main` (497 tests) |
| EV-2 | Frontend `main` `1f0187d`: `vitest run --coverage` 60 tests, 93,48 % de líneas (corrida D3 del 15/11 y CI de `main`) |
| EV-3 | E2E Playwright 7/7 en el CI de `main` del frontend; 3 corridas sin inestables (`reports/i2.md`) |
| EV-4 | Grupo `bug` sobre `57b2765` (D3, 15/11): `./mvnw -q -B test -Dbugs.excluded=none -Dgroups=bug -Djacoco.skip=true` -> 19 tests, 16 pasan, 1 falla (`bug09a`) y 2 dan error de preparación (`bug02`, `bug14`); las 3 son pruebas desactualizadas (DEF-17, DEF-18) |
| EV-5 | Pruebas con otros módulos en producción del 15/11, con `eventId` confirmados por Core y por cada módulo (`04-informe-ejecucion.md` sección 7, `pruebas-modulos-15-11.md`) |
| EV-6 | Newman en el job `api-tests` del CI de `main`: 273 requests, 695 aserciones, 0 fallas; los 6 casos `[KI-n]` informan "corregido" |

Los nombres `TC-BE-*` son los ids que ya figuran en los `@DisplayName` de los tests. Los `TC-PEND-*` son casos definidos en `03-especificacion-casos.md` que todavía no tienen test.

## F1 - Infracciones
| HU | Criterio de aceptación | Caso | Test automatizado | Resultado | Evid. | Defecto |
|---|---|---|---|---|---|---|
| HU-INF-01 | Identificar al inspector | TC-BE-INF-01 | `integration/InfractionIntegrationTest.inspectorRegisters`; `ApiContractSmokeTests.creatingInfractionUsesJwtInspectorAndReturnsLocation` | PASA | EV-1 | |
| HU-INF-01 | Patente, tipo, fecha, ubicación | TC-BE-INF-01 | `RequestValidationTests.rejectsInvalidInfractionAmountsAndIncompleteDebtorIdentification`; `SecurityEvidenceTests.massAssignmentInfraction` | PASA | EV-1 | |
| HU-INF-01 | Requerir evidencia o validación del inspector | TC-BE-INF-EV | `InfractionEvidenceRuleTests.withEvidenceIsCreated`, `inspectorWithoutEvidenceIsCreated`, `foreignInspectorIdInBodyIsIgnored`, `adminCannotCreate`; `unit/InfractionValidationRuleTest` (8 casos: sin evidencia ni inspector -> 400) | PASA | EV-1 | DEF-16 (cerrado) |
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
| HU-INF-06 | Publica `infractionConfirmed`; no envía `infractionAnulled` | TC-BE-INF-16, TC-BE-INF-18 | `confirmPending`, `confirmInvalid` (no republica); integración real con Core/M5 (Rentas) | PASA (automático; fecha con offset: `InfractionConfirmedOffsetTest`). Real: Core entregó a `q.rentas`; procesamiento por M5 PENDIENTE | EV-1, EV-5 | DEF-25 (cerrado) |

## F2 - Cortes de calle
| HU | Criterio | Caso | Test automatizado | Resultado | Evid. | Defecto |
|---|---|---|---|---|---|---|
| HU-COR-01 | Recibir solicitudes de Obras y Ambiente | TC-BE-SC-01 | `integration/StreetClosureIntegrationTest.createdFromEvent`; `listener/handler/StreetClosureRequestedHandlerTest` | PASA | EV-1 | |
| HU-COR-01 | Identificador y módulo de origen | TC-BE-SC-01/02 | `createdFromEvent`, `unknownModule` | PASA | EV-1 | |
| HU-COR-01 | Datos necesarios para evaluar | TC-BE-SC-01 | `StreetClosureRequestedHandlerTest.rejectsRequestsWithoutTheRequiredWorksPayloadFields`, `keepsTimeAndZoneOfRequestedWindow` | PASA | EV-1 | |
| HU-COR-01 | Evento duplicado no se procesa dos veces | TC-BE-EVT-D | `listener/IncomingEventDispatcherTest.discardsEventAlreadyProcessed` | PASA | EV-1 | |
| HU-COR-02 | Consultar la solicitud | TC-BE-SC-08 | `StreetClosureIntegrationTest.permissions` | PASA | EV-1 | |
| HU-COR-02 | Aprobar publica `streetClosureApproved`; condiciones | TC-BE-SC-03 | `StreetClosureIntegrationTest.approve`; E2E-7; real con M3 y M6 | PASA | EV-1, EV-3, EV-5 | |
| HU-COR-02 | Rechazar con motivo publica `streetClosureRejected` | TC-BE-SC-05 | `StreetClosureIntegrationTest.reject`; `AdditionalServiceCoverageTests.streetClosureCanBeRejectedOnlyWhileRequested`; front `staff.test.jsx` | PASA | EV-1, EV-2 | |
| HU-COR-02 | Conserva referencia a la solicitud y al módulo | TC-BE-SC-03/05 | `approve`, `reject` | PASA | EV-1 | |
| HU-COR-02 | Transición no permitida -> 409 | TC-BE-SC-07 | `StreetClosureIntegrationTest.invalidTransitions`; `OtherServicesTest.closureApproveOnlyFromRequested`; `StateTransitionTests.streetClosureCannotActivateBeforeApproval` | PASA | EV-1 | |
| HU-COR-02 | Aprobar sin desvíos devuelve 200 | BUG-BE-02 | `BugReproductionTest.bug02_approveWithoutDetours` | ERROR de preparación del test ("affectedSections es obligatorio"): test desactualizado, no hay veredicto sobre el producto | EV-4 | DEF-18 |
| HU-COR-03 | Calle afectada, fechas, rutas alternativas, condiciones | TC-BE-SC-03/04 | `approve`, `approvalCanSetAuthorizedWindow`; `ApiWorkflowIntegrationTests.streetClosureCompletesItsHttpLifecycle` | PASA | EV-1 | |
| HU-COR-03 | Desvíos asociados al corte | TC-BE-SC-03 | `approve`; `MapperCoverageTests.mapsParkingSessionAndStreetClosureResponses` | PASA | EV-1 | |
| HU-COR-04 | Solo activar un corte aprobado | TC-BE-SC-07 | `invalidTransitions`; `StateTransitionTests.streetClosureCannotActivateBeforeApproval` | PASA | EV-1 | |
| HU-COR-04 | Activar antes de `startDate` | TC-PEND-14 | ninguno: `StreetClosureService.activate` solo valida el estado | NO IMPLEMENTADO (la regla no existe) | git grep; observado en prueba real | DEF-07 |
| HU-COR-04 | Extender el período | TC-BE-SC-EXT | `StreetClosureRequestedHandlerTest.keepsTheReferenceToThePreviousRequestForAnExtension` (la prórroga llega como nuevo evento) | PASA | EV-1 | |
| HU-COR-04 | Finalizar un corte activo, fecha y hora, observaciones | TC-BE-SC-06 | `fullLifecycle`; `AdditionalServiceCoverageTests.streetClosureCompletesTheApprovedLifecycle`; real con M3 y M6 | PASA | EV-1, EV-5 | |
| HU-COR-04 | Publica `streetClosureEnded` con `causationId` | TC-BE-SC-06, TC-BE-SC-09 | `fullLifecycle`, `StreetClosureIntegrationTest.responsesKeepCorrelationAndCausation`, `OtherServicesTest.expireDueClosures` | PASA. Real: re-prueba con M3 PENDIENTE (su consumidor v1.0 rechaza `causationId` no nulo) | EV-1 | DEF-08 (cerrado), DEF-33 |
| HU-COR-04 | Vencimiento automático de ventana | TC-BE-SC-U02 | `OtherServicesTest.expireDueClosures` | PASA | EV-1 | |
| HU-COR-04 | RF-COR-11: publicar `streetClosureActivated` | TC-PEND-14b | `fullLifecycle` afirma que NO se publica la activación | NO IMPLEMENTADO por diseño; a confirmar con el PO | git grep | DEF-21 |
| HU-COR-05 | Recibir servicios urbanos, podas, riesgo de árboles, obras | TC-PEND-04 | ninguno: no hay handlers para esos eventos en `listener/handler/` (5 handlers: pagos, deuda, ticket, solicitud de corte, reverso) | NO IMPLEMENTADO | git grep | DEF-20 |
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
| (soporte) | Ticket de M2 derivado a M7 crea operativo; `updateTicketStatus` cumple el esquema de Core | TC-BE-EVT-01..03, TC-BE-EVT-SCH | `TicketEventListenerIntegrationTest.*`; `unit/TicketUpdatedHandlerTest.*`; `CoreSchemaContractTest` (5) | PASA. Real con M2: PENDIENTE (falta su `ticketUpdated`) | EV-1 | DEF-31 (cerrado) |

## F4 - Accidentes
| HU | Criterio | Caso | Test automatizado | Resultado | Evid. | Defecto |
|---|---|---|---|---|---|---|
| HU-ACC-01 | Fecha, hora, ubicación, inspector, `accidentId` | TC-BE-ACC-01 | `integration/RoadAccidentIntegrationTest.register_publishesEvent`; `SecurityIdentityTests.accidentRegistrationUsesAuthenticatedInspectorInsteadOfEntityValue`; `ApiContractSmokeTests.creatingAccidentIgnoresSpoofedInspectorAndUsesJwtIdentity` | PASA | EV-1 | |
| HU-ACC-01 | Disponible para continuar su gestión | TC-BE-ACC-03 | `RoadAccidentIntegrationTest.updatePreliminary` | PASA | EV-1 | |
| HU-ACC-02 | Personas, lesión, vehículos, daños, infraestructura | TC-BE-ACC-01/01c | `register_publishesEvent`; `register_withInfrastructureDamageWithoutDescription_isRejected`; `RequestValidationTests.requiresDamageDescriptionWhenAccidentDamagesInfrastructure` | PASA | EV-1 | |
| HU-ACC-02 | `roadAccidentRegistered` solo con daño de infraestructura | TC-BE-ACC-01/01b | `register_publishesEvent`, `register_withoutInfrastructureDamage_doesNotPublishEvent`; real con M3 (PROCESSED, 1 proyección) | PASA | EV-1, EV-5 | |
| HU-ACC-03 | Acciones asociadas; cierre solo al finalizar atención | TC-BE-ACC-03/04/05 | `updatePreliminary`, `verifiedIsFinal`, `preliminaryStatusIgnored`; `StateTransitionTests.verifiedAccidentCannotBeModified` | PASA | EV-1 | |
| HU-ACC-03 | Conservar historial de la actuación | TC-PEND-03c | sin test: no hay entidad de historial | NO VERIFICABLE | | |
| HU-ACC-04 | Ticket que originó la actuación; asociación; actualizaciones de M2 | TC-BE-EVT-01..03 | `TicketEventListenerIntegrationTest.routedToTransito`, `otherArea`, `otherUpdateTypes`; `TicketUpdatedHandlerTest.*` | PASA | EV-1 | |
| HU-ACC-04 | Informar evolución a M2 (`updateTicketStatus`) | TC-BE-EVT-01, TC-BE-EVT-SCH | `TicketUpdatedHandlerTest.routedTicketForM7CreatesOperationAndPublishesStartedUsingCoreEnvelope`; `TrafficOperationIntegrationTest.closeSetsEnd`; `CoreSchemaContractTest.resolvedPayloadsMatchSchema` | PASA (contra el esquema de Core); real con M2 PENDIENTE | EV-1 | DEF-31 (cerrado) |
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
| HU-EST-05 | Infracción asociada, inspector identificado, importe válido | TC-BE-PS-05, TC-BE-PS-05c, BUG-BE-01 | `ValidationAndHeadersDefectsTest.unpaidFlagValidAmountStillWorks`, `unpaidFlagRejectsInvalidAmount`; `InfractionEvidenceRuleTests.flagUnpaidStillWorks`; `bug01_flagUnpaidAlways500` | PASA (`bug01` pasa también en EV-4) | EV-1, EV-4 | DEF-11, DEF-29 (cerrados) |
| HU-EST-05 | No generar dos infracciones por la misma sesión | TC-BE-PS-05 | `ServiceBusinessRulesTests.parkingSessionCannotGenerateMoreThanOneUnpaidInfraction`; `bug15_flagUnpaidTwice` | PASA | EV-1, EV-4 | |
| HU-EST-05 | Sesión ajena no accesible por otro ciudadano (BOLA) | SEC-BOLA-04 | `SecurityEvidenceTests.bolaParkingSession`; `ParkingIntegrationTest.Ownership` (`otherCitizenGets404`, `closedForeignSessionGets404`, `ownerNotSettableFromBody` y 3 más) | PASA (404 a ciudadano ajeno, sin cambiar estado) | EV-1, EV-6 | DEF-15 (cerrado) |

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
| Login válido / inválido (HU-03 de la Parte B, BR-03) | TC-BE-AUTH, SEC-01..03 | `AuthAndSecurityIntegrationTest.loginOk`, `loginFails`; `ApiContractSmokeTests.invalidCredentialsReturnProblemDetails`; E2E-5 | PASA. Producción: login real de las 3 cuentas verificado por el equipo tras el deploy de #39 | EV-1, EV-3 | DEF-01 (cerrado) |
| BR-03: M1 no responde en 5 s -> 503 sin token (el Google Doc dice "sin prueba": hoy SÍ existe) | TC-BE-AUTH-M1 | `unit/RestIdentityValidatorTest.timeout`, `connectionRefused`, `errorStatuses`, `invalidResponses`; `integration/RestIdentityContextTest.loginTimeout`, `loginUpstreamError`, `loginConnectionRefused`, `loginAuthFalse` | PASA (con M1 simulado). Contra M1 real: NO VERIFICABLE (adaptador sin activar en Railway) | EV-1 | DEF-10 |
| Roles y permisos por endpoint (BFLA), 403 antes que 400 | SEC-BFLA | `SecurityEvidenceTests.forbiddenRolesAndAnonymous` (403 exacto); `ValidationAndHeadersDefectsTest.authorizationBeforeValidation`; `ApiWorkflowIntegrationTests.citizenCannotCreateInfraction` | PASA | EV-1, EV-6 | DEF-27 (cerrado) |
| Evento duplicado / inválido en consumo | TC-BE-EVT-D | `IncomingEventDispatcherTest` (4 tests: nuevo, ya procesado, sin handler, fallo del handler no se registra) | PASA | EV-1 | |
| Idempotency-Key, ETag/If-Match/412, Problem Details | REST-IDEM-01, REST-ETAG-01/02, REST-ERR-01 | `SecurityEvidenceTests.idempotency`, `etag`, `notModified`, `problemDetails`; `EtagServerTest.optimisticConcurrency`; `ValidationAndHeadersDefectsTest.missingRequestParamIsProblemDetails`, `typeMismatchIsProblemDetails` | PASA | EV-1, EV-6 | DEF-28 (cerrado) |
| Registro de eventos publicados (regla 9.3 del TP) | TC-BE-OUTBOX-REG | `OutboxContractTest.filaConMetadatosDelSobre`; `OutboxIntegrationTest.endpointRegistro` (`GET /api/v1/admin/outbox`) | PASA | EV-1 | DEF-04 (cerrado) |
| Sin pérdida de eventos con el broker caído (Transactional Outbox) | TC-BE-OUTBOX | `OutboxIntegrationTest.confirmarYPublicar`, `brokerCaidoYVuelve`, `rollbackNoDejaEvento`, `failedTrasNIntentosSinBloquear`, `sinDuplicadosEnParalelo`; `OutboxContractTest.contratoIdentico`; manual en compose (s12, i5) | PASA | EV-1 | DEF-14 (cerrado) |
| Sin DNI en el JWT; DNI solo por `GET /auth/me` | SEC-DATA-01 | `SecurityEvidenceTests.jwtHasNoPii`; `JwtSinDniIntegrationTest` (5) | PASA | EV-1 | DEF-06 (cerrado) |
| Health con broker caído | TC-BE-HEALTH | `integration/HealthWithBrokerDownTest.healthIsUpFastWithBrokerDown` | PASA | EV-1 | DEF-02 (cerrado) |
| IA: sugerencia, apagada por defecto, modo manual, inyección, PII | TC-IA-01..08 (en 03) | `integration/InfractionSuggestionIntegrationTest.*`; `InfractionSuggestionDisabledIntegrationTest.*`; `ai/*Test` | PASA con proveedor simulado; con Groq real: PENDIENTE | EV-1 | |
| Frontend IA (carga, error, manual) | TC-IA-09 | `suggestions.test.jsx` (8 tests) | PASA | EV-2 | |

## Resumen de cobertura (contado sobre las tablas de arriba)
- 31 HU de F1-F7: todas tienen al menos una fila. Ninguna HU queda sin caso: las que no tienen test tienen un caso `TC-PEND-*` en 03.
- HU con algún criterio sin implementar o sin test (marcadas arriba): HU-INF-01 (importe y reincidencia), HU-INF-03 (un criterio), HU-INF-04 (adjuntos, `appealId`), HU-COR-04 (activación temprana, evento de activación), HU-COR-05, HU-OPE-03, HU-ACC-03 (historial), HU-VEH-02, HU-VEH-03 (parcial), HU-EST-03, HU-EST-04, HU-IND-01, HU-IND-02, HU-IND-03 (parcial).
- Criterios con test que FALLA hoy en el build normal: **ninguno** (EV-1: 0 fallos). En el grupo `bug` (EV-4) solo fallan 3 reproducciones desactualizadas (`bug09a`, `bug02`, `bug14`; DEF-17 y DEF-18), que no son veredicto sobre el producto.
- Pendiente de prueba real con otros módulos (EV-5): re-prueba de cortes con M3 (DEF-33), procesamiento de `infractionConfirmed` por M5, `ticketUpdated` de M2, adaptador de M1.
