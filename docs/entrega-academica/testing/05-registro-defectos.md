# 05 - Registro de defectos

Fecha: 15/11/2026. Fuente: código de `main` (`git grep`), reportes de agentes y la corrida del grupo `bug` del agente D1.

## Severidades usadas
No tuve a mano el PDF de la cátedra con las definiciones del profesor; usé estas definiciones de trabajo y **hay que contrastarlas con las del profesor antes de entregar**:
| Severidad | Definición de trabajo |
|---|---|
| Bloqueante | impide usar o desplegar el sistema, o corrompe datos; sin workaround |
| Crítica | riesgo grave de seguridad o de pérdida de integridad en producción, o falla de un flujo principal sin workaround |
| Mayor | incumple un requisito o criterio importante o degrada el servicio; existe workaround |
| Menor | desvío cosmético, de bajo riesgo o de documentación |

Estados: Abierto, Corregido (en `main`, verificado con test), Aceptado (riesgo aceptado por decisión documentada).

## Tabla resumen
| Id | Título | Severidad propuesta | Estado |
|---|---|---|---|
| DEF-01 | Credenciales de desarrollo fijas activas en producción (stub de login) | Crítica | Abierto |
| DEF-02 | `/actuator/health` colgado con el broker caído | Mayor | Corregido (PR #24, en #25) |
| DEF-03 | Adaptador REST de M1 no arrancaba (Jackson) | Mayor | Corregido (PR #23, en #25) |
| DEF-04 | Sin registro de eventos publicados (regla 9.3 del TP) | Mayor | Abierto |
| DEF-05 | Logs con payload completo de eventos (`LoggingEventPublisher`) | Mayor | Abierto |
| DEF-06 | DNI (`nationalId`) dentro del JWT | Mayor | Abierto |
| DEF-07 | Un corte se activa antes de `startDate` | Menor | Abierto |
| DEF-08 | `causationId` null en `streetClosureEnded` | Menor (a confirmar con Core) | Abierto |
| DEF-09 | Swagger público en producción | Menor | Abierto |
| DEF-10 | 200 con `auth=false` de M1 devuelve 502 | Menor | Abierto (comportamiento testeado) |
| DEF-11 | Estacionamiento impago devolvía 500 | Mayor | Corregido (verificado el 15/11) |
| DEF-12 | Contraseña de cuentas de prueba de otro módulo (M1) en el historial de Git | Menor | Aceptado |
| DEF-13 | State no implementado como patrón en el corte de calle | Mayor (criterio académico) | Abierto |
| DEF-14 | Outbox transaccional no implementado | Mayor | Abierto |
| DEF-15 | BOLA: un ciudadano lee y cierra la sesión de estacionamiento de otro | Mayor | Abierto |
| DEF-16 | Infracción sin evidencia se acepta (201) | Mayor | Abierto |
| DEF-17 | `PATCH /infractions` parcial devuelve 400 en la reproducción BUG-BE-09a | Menor (probable test desactualizado) | A confirmar |
| DEF-18 | Pruebas de reproducción BUG-BE-02 y 14 desactualizadas ("affectedSections es obligatorio") | Menor (deuda de tests) | Abierto |
| DEF-19 | Importe base, reincidencia y costo de acarreo/estadía no se calculan en el servidor | Mayor | Abierto |
| DEF-20 | HU sin implementar: COR-05, OPE-03 (controles), EST-03/04 (detección, descarte), INF-04 (adjuntos, `appealId`), IND (filtros e indicadores del backend) | Mayor | Abierto |
| DEF-21 | `streetClosureActivated` no se publica (RF-COR-11) | Menor | Abierto (a confirmar con el PO) |

## Detalle
### DEF-01 Credenciales fijas activas en producción
- **Pasos**: en producción, `POST /api/v1/auth/login` con una cuenta del stub de desarrollo.
- **Esperado**: producción valida contra M1 y no acepta cuentas fijas.
- **Obtenido (código)**: `StubIdentityValidator` está activo con `matchIfMissing = true` cuando `users.rest.enabled` no es `true`. El tablero del 15/11 informa que `USERS_REST_ENABLED` no está definido en Railway, por lo que el stub sigue activo (inferencia a partir del tablero; **no volví a probarlo en producción**, está prohibido sin permiso).
- **Evidencia**: `src/main/.../auth/StubIdentityValidator.java`; `TABLERO.md`; checklist de preentrega.
- **Workaround**: definir en Railway `USERS_REST_ENABLED=true` y la URL de M1 (el fix de Jackson ya está en `main`, DEF-03). Es solo configuración, pero afecta a los otros módulos: requiere decisión humana y la prueba contra M1 real, hoy NO VERIFICABLE. Las cuentas de M1 de prueba están en el historial (DEF-12).
- **Estado**: Abierto. **Es el único Crítico**: supera el máximo admitido (0).

### DEF-02 Health colgado con el broker caído (Corregido)
- **Pasos**: pausar el broker y pedir `/actuator/health`. **Esperado**: 200 UP rápido. **Obtenido**: sin respuesta en 40 s (con el broker detenido, 503 DOWN).
- **Evidencia**: `reports/b4.md`; prueba `integration/HealthWithBrokerDownTest.healthIsUpFastWithBrokerDown`; producción tras el deploy: UP en 0,7 s.
- **Causa raíz**: el indicador de salud de RabbitMQ de Actuator esperaba al broker sin cota de tiempo. Se puso `management.health.rabbit.enabled=false`. Efecto lateral aceptado: el health ya no refleja el broker.

### DEF-03 Adaptador M1 no arrancaba (Corregido)
- **Pasos**: arrancar con `USERS_REST_ENABLED=true`. **Esperado**: arranca y usa el validador REST. **Obtenido**: falla al inyectar `ObjectMapper`.
- **Evidencia**: `reports/a1.md` (hallazgo), `reports/b4.md`; prueba `integration/RestIdentityContextTest` (8 casos).
- **Causa raíz**: Spring Boot 4 provee `tools.jackson.databind.ObjectMapper` y la clase inyectaba el de Jackson 2. Ningún test levantaba el contexto con la propiedad en `true`.

### DEF-04 Sin registro de eventos publicados
- **Esperado**: regla 9.3 del TP, trazabilidad de lo publicado. **Obtenido**: no existe entidad o tabla de eventos publicados (solo `ProcessedEvent` para los recibidos); `git grep` no encuentra repositorio de publicados.
- **Evidencia**: listado de `model/` y `repository/`. **Workaround**: logs de Railway y la consulta de auditoría de Core (sección 21 de la especificación Core). **Estado**: Abierto.

### DEF-05 Logs con payload completo
- **Obtenido**: `LoggingEventPublisher` registra `data={}` completo con `log.info` (línea 17 de `event/LoggingEventPublisher.java`).
- **Matiz verificado**: ese bean solo se activa con `core.rabbitmq.enabled=false`; `RabbitMqEventPublisher` no registra el payload (solo `debug` de eventos internos). Si Railway tiene `CORE_RABBITMQ_ENABLED=true` (el tablero dice que conectó con Core, pero no revisé las variables), el defecto aplica a local y desarrollo y no a producción: en ese caso bajaría a Menor. **Estado**: Abierto. **Workaround**: no usar el publicador de logs en entornos con datos reales. Falta una prueba de logs (SEC-D01).

### DEF-06 DNI en el JWT
- **Pasos**: login y decodificar el payload. **Obtenido**: el payload incluye el claim `nationalId` (corrida del 15/11 de `SecurityEvidenceTests.jwtHasNoPii`: FALLA). **Esperado**: sin datos personales (SEC-09). **Workaround**: ninguno técnico simple; el frontend y los filtros por titular usan el claim. **Estado**: Abierto.

### DEF-07 Activación antes de `startDate`
- **Obtenido**: `StreetClosureService.activate` solo valida el estado `APPROVED`; la prueba real mostró activación 7 s antes (plan de entrega). **Workaround**: operativo (activar dentro de la ventana). **Estado**: Abierto.

### DEF-08 `causationId` null en `streetClosureEnded`
- **Obtenido**: `finish(...)` publica con `null` como causationId (línea de `StreetClosureService.finish`). **Esperado**: lo que defina el contrato Core (secciones 9 y 26). Severidad a confirmar con Core; M3 recibió el evento el 15/11. **Estado**: Abierto.

### DEF-09 Swagger público
- **Obtenido**: `SecurityConfig` permite sin autenticación `/swagger-ui/**`, `/v3/api-docs/**` y `/openapi.yaml`. No hay perfil que lo desactive. **Estado**: Abierto; decisión de diseño pendiente (la cátedra pide documentar la API).

### DEF-10 `auth=false` de M1 devuelve 502
- **Obtenido**: respuesta 200 con `auth=false` se traduce a 502 (probado en `RestIdentityContextTest.loginAuthFalse`). M1 real y el mock devuelven 401 para clave mala, por lo que hoy no se dispara. **Estado**: Abierto, a decidir con M1.

### DEF-11 Estacionamiento impago 500 (Corregido)
- **Evidencia**: `BugReproductionTest.bug01_flagUnpaidAlways500` **pasa** en la corrida del 15/11 (antes: 500 por `location` nulo). `ParkingIntegrationTest.flagUnpaidRules` cubre los rechazos. El commit exacto del arreglo no lo identifiqué. El test sigue excluido del build normal: conviene quitarle la etiqueta `bug`.

### DEF-12 Contraseña de M1 en el historial
- **Evidencia**: gitleaks `--all` da 4 hallazgos en el backend (1 contraseña de cuentas de prueba de M1 en `docs/Respuestas de Ro.md`, 3 ejemplos); `.gitleaksignore` los documenta. En HEAD ya están redactados (B2b). **Decisión del humano (15/11)**: no avisar a M1 ni rotar (ambiente académico). **No es un falso positivo puro** (B2 lo evaluó como credencial real de otro equipo): es riesgo aceptado, severidad Menor por el ambiente.

### DEF-13 State no implementado como patrón
- **Obtenido**: `StreetClosureStatus` es un enum y `StreetClosureService.requireStatus` valida transiciones. El checklist exige State con transiciones inválidas probadas. **Estado**: Abierto (asignado a Lautaro). Hay pruebas del 409, pero no del patrón.

### DEF-14 Outbox no implementado
- **Obtenido**: `git grep -i outbox` en `src` no encuentra nada. Con el broker caído un evento publicado se pierde. **Workaround**: reenvío manual desde Core o los otros módulos; no hay prueba de no pérdida. **Estado**: Abierto.

### DEF-15 BOLA en sesiones de estacionamiento
- **Pasos**: ciudadano A crea sesión; ciudadano B hace `GET /parking-sessions/{id}` y `POST /parking-sessions/{id}/closure`. **Esperado**: 403/404. **Obtenido**: 200 y 200 (`SecurityEvidenceTests.bolaParkingSession`, corrida del 15/11).
- **Workaround**: los identificadores son UUID no enumerables, lo que reduce la explotación pero no la elimina. **Estado**: Abierto.

### DEF-16 Evidencia no exigida
- **Obtenido**: alta de infracción con `evidenceUrl=[]` devuelve 201 (`BugReproductionTest.bug04`: FALLA el 15/11). HU-INF-01 y RF-INF-05 exigen evidencia o validación. **Workaround**: el frontend podría exigirla (NO VERIFICADO). **Estado**: Abierto.

### DEF-17 y DEF-18 Pruebas desactualizadas
- `bug09a` envía `finalAmount=1` y espera 200; recibe 400, coherente con `ServiceBusinessRulesTests.infractionPatchRejectsAnInvalidFinalAmountAfterMerge`. Probable test desactualizado: no lo di por defecto del producto. `bug02` y `bug14` fallan al preparar el corte por un cambio de contrato en el handler. Acción: actualizarlos o retirarlos.

### DEF-19 Importes no calculados
- **Obtenido**: `CreateInfractionRequest` recibe `baseAmount` y `finalAmount`; `towingAndStorageFee` llega en la liberación. No hay tabla de importes por tipo, ni reincidencia, ni cálculo por días de estadía. Incumple RF-INF-02/03 y RF-VEH-03 / HU-VEH-02. **Workaround**: carga manual por el personal.

### DEF-20 HU sin implementar
- Ver la matriz 02: HU-COR-05, controles de operativo, detección y descarte de incumplimientos de estacionamiento, adjuntos de apelación, filtros e indicadores del backend. **Estado**: Abierto; se podría reclasificar como "fuera de alcance acordado" solo con decisión del PO.

### DEF-21 `streetClosureActivated`
- **Obtenido**: `StreetClosureActivatedPayload` existe sin uso; el test `fullLifecycle` afirma que no se publica. El catálogo de Core registrado tiene 6 tipos y no incluye la activación. La Parte A del Google Doc dice que `streetClosureActivated` "se utiliza internamente", pero `git grep` no muestra ninguna emisión interna: inconsistencia entre documento y código. RF-COR-11 pide publicarlo. Confirmar con el PO.

## Riesgo de salida
Máximo admitido: 0 Bloqueantes, 0 Críticos, 3 Mayores abiertos con workaround.
| Severidad | Abiertos hoy | Límite |
|---|---|---|
| Bloqueante | 0 (ninguno identificado) | 0 |
| Crítica | **1** (DEF-01) | 0 |
| Mayor | **9** (DEF-04, 05, 06, 13, 14, 15, 16, 19, 20) | 3 |

**La clasificación honesta EXCEDE el límite.** No lo oculté. Lo que se puede hacer para bajar el recuento con criterios defendibles, sin cambiar la severidad por conveniencia:
1. DEF-01: configurar M1 real en Railway y verificarlo cierra el Crítico (requiere decisión humana y prueba).
2. DEF-05 baja a Menor solo si se confirma `CORE_RABBITMQ_ENABLED=true` en producción.
3. DEF-15 y DEF-16 son arreglos de código chicos (ownership en `ParkingSessionService`; validar lista no vacía).
4. DEF-19 y DEF-20 son brechas de requisito: el PO puede decidir formalmente acotar el alcance, pero eso debe documentarse y avisarse al profesor.
5. DEF-13 y DEF-14 están asignados a Lautaro.
Aun con todo eso, hoy no se cumple la condición de salida y no debe entregarse un informe que diga lo contrario.
