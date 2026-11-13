# Checklist de preentrega 4 — M7 Tránsito

Fecha de actualización: 13/11/2026. Esta lista separa evidencia verificada de trabajo pendiente; no debe marcarse como completo un ítem sin enlace, captura, log o ejecución reproducible.

## Bloqueantes de integración

| Prioridad | Ítem | Estado | Evidencia / próximo paso |
| --- | --- | --- | --- |
| P0 | Conectividad AMQPS M7 → Core | Bloqueado | Railway intenta `core-uade-2026.duckdns.org:5671` y recibe `SocketTimeoutException`. Core debe revisar la allowlist/Security Group para el egress actual de Railway. |
| P0 | Registro del catálogo Core | Pendiente | Cuando AMQPS quede disponible, confirmar en logs el token HTTP y el registro de `infractionConfirmed`, `streetClosureApproved`, `streetClosureRejected`, `streetClosureEnded`, `roadAccidentRegistered` y `updateTicketStatus`. |
| P0 | E2E M3 → M7 → M3 | Pendiente | Pedir a Core reencolar o a M3 reenviar `streetClosureRequested` (eventId `1a0d2881-908a-4c40-986b-1e920cd20659`); guardar los eventId de solicitud y respuesta. Probar aprobación, rechazo, finalización/vencimiento y prórroga. |
| P0 | E2E M7 ↔ M5 | Pendiente | Probar `infractionConfirmed` → alta de deuda y los retornos `paymentRegistered`, `debtSettled`, `paymentReversed`. `infractionAnulled` es interno de M7 y no integra el catálogo Core/M5. |
| P1 | E2E de identidad M1 | Pendiente | Activar el adaptador REST en ambiente de integración y ejecutar login con una cuenta de ciudadano y una de empleado; conservar evidencia de ambas respuestas. |

## Build, calidad y pruebas

| Ítem | Estado | Evidencia / próximo paso |
| --- | --- | --- |
| Build reproducible | Verificado localmente | `./mvnw verify` pasó el 13/11/2026. Verificar también el recorrido exacto documentado en README desde un ambiente limpio. |
| CI | Parcialmente verificado | `.github/workflows/ci.yml` ejecuta `mvn verify` y gitleaks en pushes/tags. Confirmar ejecución verde del commit final y luego del tag. |
| Cobertura backend | Verificada | JaCoCo local: 87% de instrucciones y 70% de ramas. El mínimo configurado es 85% de líneas. Guardar el reporte de `target/site/jacoco/index.html`. |
| Cobertura frontend | Pendiente | Ejecutar y guardar el reporte de cobertura del frontend; la comisión exige 85%. |
| Mutation testing | Pendiente | Ejecutar `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage -Pmutation` y documentar score >= 60%. |
| Sonar Quality Gate | Pendiente de confirmar | Ejecutar análisis/configurar proyecto y guardar enlace o captura del gate aprobado. |
| Testcontainers | Verificado localmente | `PostgresContainerIntegrationTest` levanta PostgreSQL 16 real y ejecuta el login de la aplicación. Ejecución 13/11/2026: 1 test, sin fallos. Incluirlo en CI y ampliar luego con un flujo de negocio persistente. |
| Prueba de carga | Pendiente | Ejecutar 50 usuarios concurrentes y guardar resultado con p95 < 500 ms. |
| Tests flaky | Pendiente de confirmación | Repetir suite/CI y registrar que no hubo resultados intermitentes. |

## Integración, resiliencia y seguridad

| Ítem | Estado | Evidencia / próximo paso |
| --- | --- | --- |
| Idempotency-Key y ETag | Implementado; falta evidencia | Ejecutar casos reales y guardar request/response: repetición de POST, `If-None-Match` y `If-Match`. |
| Problem Details | Implementado; falta evidencia | Guardar respuestas 400, 401, 403, 404, 409 y 412. |
| Consumidores idempotentes / DLQ | Implementado; pendiente Core | Con AMQPS disponible, repetir un `eventId`, provocar error controlado y verificar DLQ/reintento en Core. |
| Broker caído / recuperación | Pendiente | Probar indisponibilidad, verificar comportamiento y evidencia de no pérdida/reintento según diseño acordado. |
| Timeout/fallback externo | Pendiente de evidencia | Ejecutar caída o timeout del servicio M1 y guardar la respuesta controlada. |
| BOLA/IDOR, BFLA, JWT, mass assignment, inyección y fuerza bruta | Implementado/testeado localmente; falta evidencia de demo | Preparar guion de requests y capturas para la demo. |
| ZAP y dependencias | Pendiente | Ejecutar ZAP y escaneo de dependencias; no puede quedar alerta High ni CVE crítica sin mitigación. |
| Secretos | Pendiente de revisión final | Ejecutar gitleaks sobre historial; nunca versionar `CORE_MODULE_SECRET` ni contraseñas AMQPS. Solicitar rotación de toda credencial que haya quedado expuesta en chats. |

## Documentación y presentación

| Ítem | Estado | Próximo paso |
| --- | --- | --- |
| BRD, historias y Gherkin | Pendiente de consolidación | Revisar que cada historia comprometida tenga criterios y escenario Gherkin ejecutable. |
| ADR | Parcial | Consolidar al menos tres patrones justificadamente, incluido State en la entidad central. |
| OpenAPI / WSDL | Parcial | Verificar que describan la API desplegada y cualquier adaptador SOAP vigente. |
| Manual de usuario | Pendiente de prueba | Ejecutar cada flujo descrito; el docente elegirá uno al azar. |
| Plan de pruebas, trazabilidad e informe | Pendiente de consolidación | Mapear requisito → test → evidencia → resultado. |
| IA generativa | Pendiente de evidencia | Documentar gateway, pruebas funcionales, prompt injection, fuga de datos, fuera de dominio y caída del proveedor; declarar el uso de IA. |
| PDF y presentación Teams | Pendiente | Preparar y subir antes del horario de exposición. |

## Cierre y tag

1. Repetir `./mvnw verify` con el árbol limpio.
2. Confirmar CI verde, cobertura, escaneos y evidencia de integraciones.
3. Revisar que no haya defectos bloqueantes/críticos y que los mayores tengan workaround documentado.
4. Crear `v2.0-entrega4` **solo cuando** lo anterior esté cerrado; los commits posteriores no cuentan para la entrega.
