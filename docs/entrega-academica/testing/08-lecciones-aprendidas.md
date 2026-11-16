# 08 - Lecciones aprendidas

Tres defectos que llegaron tarde (detectados recién en la 4.ª entrega, el 15/11, aunque el código llevaba semanas). Para cada uno: qué pasó, por qué no se detectó antes y qué prueba lo habría atrapado.

## 1. El adaptador REST de M1 nunca arrancaba (DEF-03)
- **Qué pasó**: con `USERS_REST_ENABLED=true` el backend no levantaba: el validador inyectaba el `ObjectMapper` de Jackson 2 y Spring Boot 4 provee el de `tools.jackson`. Lo vio A1 al seguir el README con el compose (`reports/a1.md`).
- **Por qué no se detectó antes**: el valor por defecto era `false`, así que ningún arranque ni test cargaba el bean. `RestIdentityValidatorTest` probaba la clase aislada, sin contexto de Spring. La cobertura de líneas (88 %) no avisa de un bean que nunca se instancia. Además la integración con M1 real estaba "pendiente de coordinación externa", por lo que nadie la ejecutó. Cuando llegaron las cuentas de M1 (15/11) el adaptador se probó en local contra M1 real y respondió 200 para ciudadano y empleado.
- **Qué lo habría atrapado**: una prueba de contexto con la propiedad en `true` (hoy `integration/RestIdentityContextTest`) y el job de CI con el compose levantado y `USERS_REST_ENABLED=true` contra el mock. Regla nueva: todo bean condicional por propiedad tiene una prueba de contexto con la propiedad activada.

## 2. `/actuator/health` colgado con el broker caído (DEF-02)
- **Qué pasó**: con RabbitMQ pausado el health no respondía en 40 s. Riesgo potencial (no observado): si la plataforma usa ese endpoint como sonda, podría reiniciar o sacar de servicio la app por un problema de un tercero. Se vio al probar el compose (A1) y se midió en B4.
- **Por qué no se detectó antes**: las pruebas del build usan el publicador de logs y no levantan broker; los fallos de Core se habían tratado como "dependencia externa pendiente", no como falla de resiliencia propia. Probar "broker caído" estaba en el checklist como PENDIENTE.
- **Qué lo habría atrapado**: una prueba de resiliencia con broker caído o pausado (hoy `HealthWithBrokerDownTest`, y la prueba manual de parar y arrancar RabbitMQ en el compose). Mejor aún, un test de Testcontainers con RabbitMQ real: **hecho** en el PR #50 (`OutboxRabbitContainerIntegrationTest` y `ConsumerRabbitContainerIntegrationTest`, TC-BE-TC-01..05: outbox con broker caído, idempotencia, DLQ y log sin cuerpo).

## 3. Eventos publicados sin registro (DEF-04) y con payload completo en logs (DEF-05)
- **Qué pasó**: M7 publica eventos a Core sin guardar un registro propio de lo publicado (regla 9.3 del TP); el único rastro es el log, y el publicador de logs imprime el payload completo, con datos personales. El propio TPO ya marcaba SEC-D01 como "No cumple" en el checkpoint 3, y el plan de la 4.ª entrega lo listó entre los defectos a reportar; seguía abierto el 15/11.
- **Por qué no se detectó antes** (análisis, no medición): las pruebas verifican que el evento llega al `EventPublisher`, pero ninguna comprueba que quede una traza ni qué datos se escriben en el log. La regla 9.3 no figura trazada a ninguna historia ni caso en la matriz del Google Doc (9 filas de HU-01 a HU-03).
- **Qué lo habría atrapado**: un caso por cada regla de la especificación Core (registro y auditoría de publicaciones) en la matriz de trazabilidad desde el inicio, y una prueba que capture los logs durante un flujo y busque valores sensibles.
- **Cómo terminó**: DEF-05 se cerró con `EventLoggingSinPiiTest` e `IncomingEventLoggingSinPiiTest` (capturan la salida con DNI y patente de prueba) y DEF-04 junto con DEF-14 con el Transactional Outbox (`OutboxIntegrationTest`, `GET /api/v1/admin/outbox`), desplegados el 15/11. El outbox destapó a su vez un defecto menor de configuración: `show-sql` imprimía su consulta cada 2 s en los logs de producción (DEF-32, cerrado en el PR #50).

## Lecciones generales
1. Un "verify" en verde no equivale a "sin defectos": las pruebas etiquetadas `bug` están excluidas del build. Mientras `jwtHasNoPii` y `bolaParkingSession` estuvieron en ese grupo fallaban sin que el build lo mostrara; se corrigieron (DEF-06, DEF-15) y hoy corren en el build normal. La corrida del grupo debe ser parte del informe (hoy: 19 tests, solo fallan 3 reproducciones desactualizadas).
2. Los reportes de los agentes pueden contradecirse (313 contra 285; 496 contra 497): contrastar con el CI y decir de dónde sale cada número.
3. Probar por ejecución, no por lectura (el script `prueba-prod.sh`, ver 07).
4. Traza explícita desde cada regla del Core y cada HU hasta un caso; los huecos se declaran (02) en vez de esconderse.
5. Las integraciones externas "pendientes" necesitan un simulador propio desde el primer día (WireMock del compose) para no descubrir defectos propios tarde.
6. Corregir un contrato puede romper a un consumidor que dependía del error: al rellenar `causationId` (DEF-08) el consumidor v1.0 de M3 empezó a rechazar el evento (DEF-33). Los cambios de envelope, aunque sean correcciones, se anuncian a los consumidores y se re-prueban con Core monitoreando antes de dar el flujo por cerrado.
