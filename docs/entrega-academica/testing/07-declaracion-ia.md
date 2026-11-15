# 07 - Declaración de uso de IA

Fecha: 15/11/2026. Regla del equipo: todo test o texto generado con IA lleva autor humano responsable y debe poder explicarse; **ningún "CUMPLE" de IA sin evidencia ejecutada**. Este documento mismo fue redactado por un agente de IA (D1) y debe ser revisado y firmado por una persona del equipo antes de entregarse. Los nombres de quienes aprueban cada punto: PENDIENTE (los completa el equipo).

## 1. Herramientas
| Herramienta | Uso | Datos que recibe |
|---|---|---|
| Claude Code (modelos Claude de Anthropic), con subagentes que trabajan en carpetas separadas (A1, A2, A3, B2, B2b, B3b, B3f, B4, I1, I2, D1, T1, T2) | escribir y revisar código, tests, scripts, documentación de QA; ejecutar comandos locales (`./mvnw verify`, `npm run test:coverage`, Docker, gitleaks) | el código de los repos y los reportes. Reglas de trabajo: sin secretos, sin tocar producción sin permiso |
| Groq (modelo por defecto `llama-3.1-8b-instant`) | proveedor de la **feature de IA del producto** (sugerencia de tipo y agravantes de infracción). Apagada por defecto (`AI_ENABLED=false`) | solo el texto libre del inspector, con datos personales enmascarados (`PiiMasker`) |

No se usó IA para decidir veredictos de seguridad ni para firmar resultados sin ejecución.

## 2. Para qué se usó (por tarea)
| Agente | Tarea | Qué entregó |
|---|---|---|
| A1 | compose completo (G1) | `docker-compose.yml`, RabbitMQ y mocks WireMock, `.env.example`, README |
| A2 | E2E Playwright | 7 escenarios y job `e2e` en CI |
| A3 | responsive y accesibilidad | CSS/ARIA y scripts axe/Lighthouse |
| B2 / B2b | secretos en historial | gitleaks, `.gitleaksignore`, placeholders |
| B3b / B3f | IA del producto | gateway, endpoint de sugerencias, UI "Sugerir con IA", 88 + 11 tests |
| B4 | fixes de backend | PR #23 (Jackson en REST M1) y #24 (health) |
| I1 / I2 | integración | PR #25 (backend) y #8 (frontend) |
| D1 | documentos de QA | este directorio |

## 3. Prompts representativos (resumidos)
1. "Reproducí el compose siguiendo solo el README, desde `down -v`, y reportá lo que falle. No cambies `src/main`." (A1; descubrió el defecto del adaptador M1).
2. "Armá una puerta de IA con interfaz, adaptador al proveedor, timeout 5 s, circuito, enmascarado de datos personales, flag de apagado y tests con el proveedor simulado." (B3b).
3. "Agregá los test de seguridad BOLA/BFLA/JWT/mass assignment/XXE/429 con evidencia." (generó `SecurityEvidenceTests`).
4. "Integrá los PR #22, #24, #23, #20, #21 en una rama, resolvé conflictos conservando ambas intenciones y corré `verify`." (I1).
5. "Redactá los 8 entregables del QA; escribí solo lo verificable; donde no haya evidencia, PENDIENTE o NO VERIFICABLE." (D1).

## 4. Qué se aceptó, qué se corrigió y qué se descartó
| Resultado de IA | Decisión | Motivo / verificación |
|---|---|---|
| Fixes #23 y #24 de B4 | Aceptado | tests nuevos que fallan sin el cambio (`HealthWithBrokerDownTest` falla con 503 sin el fix) y verificación real con el compose |
| Pruebas de seguridad de `SecurityEvidenceTests` | Aceptado, con 2 pruebas etiquetadas `bug` que fallan a propósito | la corrida del 15/11 las muestra en rojo: son defectos reales, no se ocultaron |
| Casos de estacionamiento `bug01` | Reclasificado | pasa hoy; hay que quitarle la etiqueta `bug` |
| `bug02`, `bug14`, `bug09a` | Descartados como evidencia de producto | están desactualizados: no se tomaron como defecto sin confirmar |
| Reporte B3b: "313 tests" | Corregido (ver caso b) | conteo inconsistente |
| README viejo con nota de limitación REST | Corregido por I1 | la limitación se arregló con #23 |
| Catálogo de agravantes de IA | Aceptado con alerta | usa el catálogo del frontend; un test viejo manda "zona escolar" libre |

## 5. Errores de la IA detectados (casos reales)
### a) `prueba-prod.sh` imprimía el token por el canal equivocado
La función `login` del script de pruebas de producción devolvía el token por la salida estándar y los mensajes de error por la salida de error; en una versión previa el token iba por el canal equivocado y el script cortaba antes de registrar el accidente. Lo detectó **la ejecución real** (el accidente no se registraba), no la lectura del código. Corrección: el token sale solo por `stdout` capturado con `$(...)`, los avisos por `stderr`, y cada comando verifica `[ -n "$T" ]` antes de seguir. Lección: un script generado por IA se prueba ejecutándolo, no solo leyéndolo.

### b) Conteo de tests inconsistente (285 vs 313)
Un agente informó 313 tests (B3b: 225 previos + 88) y otro 285 (I1, tras integrar). El número de B3b mezclaba una base de 225 ya desactualizada (B4 mostró que `main` ejecutaba 262). Se resolvió **contando métodos por rama**: 247 = 210 + 28 + 8 + 1 (`TABLERO.md`, suma verificada). Surefire informa 285 ejecuciones porque las pruebas parametrizadas cuentan por valor (inferencia, no verificada una a una). En este informe se muestran ambos números con su significado.

### c) Afirmaciones sobre qué credenciales usaba producción
Una afirmación inicial ("producción usa las cuentas fijas") era una inferencia a partir del código y del hecho de que `USERS_REST_ENABLED` no estaba definida. Se marcó como **inferido**: no se probó en producción (no está permitido). En `05-registro-defectos.md` DEF-01 conserva esa aclaración.

### d) Otros desvíos propios de D1 (corregidos antes de entregar)
- Escribí en el primer borrador que no había WSDL publicado; el código tiene `GET /soap/authentication?wsdl`. Lo corregí.
- Las pruebas `jwtHasNoPii` y `bolaParkingSession` están excluidas del build normal por la etiqueta `bug`, así que `verify` en verde NO prueba que cumplan; solo la corrida del grupo `bug` lo muestra (ambas fallan). Esto se aclara en 04 y 06.

## 6. Cómo se verificó el trabajo de IA
- `./mvnw verify` y `npm run test:coverage` por cada rama; sin cambios de `src/main` no solicitados (`git diff --stat main`).
- Contraste de afirmaciones contra el código con `git grep`.
- Ejecución real del compose y del script contra el entorno de prueba.
- Revisión de reportes cruzados (conteos, cobertura).
- Para la feature de IA del producto: proveedor simulado en los tests (éxito, JSON roto, tipo inválido, caída, circuito abierto, inyección, datos personales); la evaluación con Groq real (`docs/ia/eval-set.json`, 20 casos) está PENDIENTE.

## 7. Autoría de tests
Regla acordada: todo test escrito por un agente debe llevar en el commit su origen de IA y quién lo revisó. **Estado actual: NO VERIFICABLE** que todos los commits cumplan la regla (no lo auditamos); el equipo debe revisarlo. Cada persona del equipo debe poder explicar los tests de su módulo: PENDIENTE de ensayo (preguntas tipo: por qué `bruteForce` espera 429; por qué `HealthWithBrokerDownTest` falla sin el fix).

## 8. Plan de IA por usar (punto 8.2)
| Uso previsto | Objetivo medible | Riesgo | Control |
|---|---|---|---|
| Pruebas visuales (capturas de las pantallas y comparación con una línea base) | detectar diferencias en los 3 anchos; objetivo: 0 diferencias no explicadas en 16 pantallas x 3 anchos por release | falsos positivos por cambios de fuente o animaciones; juicio estético de la IA | humano revisa cada diferencia; la línea base se aprueba a mano |
| Generación de casos desde las HU | objetivo: al menos 1 caso por criterio de aceptación y >= 40 % negativos o de borde (hoy 70,5 % en 03); medir cuántos casos propuestos se aceptan sin cambios | casos duplicados o que prueban lo que el código hace, no lo que la HU exige | cada caso se contrasta con la HU; los TC-PEND son la lista de pendientes |
| Triage de defectos (clasificar severidad y duplicados) | objetivo: coincidir con la clasificación humana en >= 80 % sobre una muestra de 20 defectos | sobreestimar o subestimar severidad (en esta entrega la clasificación excede el límite y no debe suavizarse) | decisión final humana; se compara la sugerencia con el criterio de la cátedra |
| Evaluación de la feature de IA (eval set de 20 casos, 5 adversariales) | tasa de salida válida >= 95 % y 0 acciones automáticas | datos reales enviados a un proveedor externo | enmascarado, apagado por defecto, humano confirma |

Nada de lo anterior está ejecutado: son objetivos, no resultados.
