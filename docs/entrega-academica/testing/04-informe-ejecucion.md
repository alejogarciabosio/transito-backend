# 04 - Informe de ejecución de pruebas

Fecha del informe: 15/11/2026. Versión probada: rama `main` del backend en el commit `65e330b` (merge del PR #25) y `main` del frontend (PR #8). Entorno de las pruebas automáticas: máquina local de los agentes, base en memoria y una prueba con PostgreSQL 16 por Testcontainers.
Regla: solo figuran datos que ya existen en los reportes de los agentes o que corrió el agente D1. Lo demás dice **PENDIENTE**. No se estimaron ni redondearon números.

## 1. Resumen ejecutivo
| Indicador | Valor | Fuente |
|---|---|---|
| Backend `./mvnw verify` | exit 0; 285 tests ejecutados; 0 fallos; 0 errores | `reports/i1.md` (rama de integración, hoy en `main`) |
| Backend, grupo `bug` (excluido del build) | 22 ejecutados; 16 pasan; 4 fallan; 2 errores de preparación | corrida del agente D1, 15/11 |
| Cobertura backend (líneas) | 88,5 % (mínimo exigido 85 %) | `reports/i1.md` |
| Cobertura backend (instrucciones / ramas) | 87 % / 70 % (medición del 13/11 anterior a los PR de integración) | `docs/entrega-academica/checklist-entrega4.md` |
| Frontend `npm run test:coverage` | 59 tests, 0 fallos; 93,43 % líneas; 79 % ramas (medición de B3f sobre su rama) | `reports/i2.md`, `reports/b3f.md` |
| E2E Playwright | 7/7 en cada una de 3 corridas | `reports/i2.md` |
| Accesibilidad axe | 0 violaciones en 3 anchos (360/768/1280) | `reports/i2.md`, `reports/a3.md` |
| Lighthouse (modo snapshot) | 100 en 48 combinaciones | `reports/a3.md` (antes de la integración) |
| gitleaks | 0 con `.gitleaksignore` sobre historial completo; 4 sin él | `reports/b2b.md`, `reports/i1.md` |
| Integraciones reales con M3 (15/11) | accidente registrado; infracción confirmada; corte aprobado, activado y finalizado | declaración del equipo; EV-5, ids PENDIENTES de adjuntar |
| k6, mutation, ZAP, Sonar, dependencias | PENDIENTE | otros agentes están midiendo |

Aclaración sobre cantidades: el backend tiene 247 métodos de prueba (210+28+8+1 por rama, ver `07-declaracion-ia.md`); Surefire informa 285 ejecuciones. La diferencia es compatible con las pruebas parametrizadas (cada valor cuenta como una ejecución), pero no la verifiqué una por una: es una inferencia.

## 2. Resultados por nivel
| Nivel | Ejecutados | Pasados | Fallados | Bloqueados | Fuente / nota |
|---|---|---|---|---|---|
| Unitario backend (`unit/`, `ai/`, `listener/`) | PENDIENTE (el reporte I1 no separa por nivel) | | | | Los 285 tests totales son la suma de todos los niveles de backend |
| Integración backend (`integration/`, Testcontainers) | PENDIENTE de desglose | | | | idem |
| Backend total (build normal) | 285 | 285 | 0 | 0 | `reports/i1.md` |
| Backend grupo `bug` (reproducciones y 2 de seguridad) | 22 | 16 | 4 | 2 (errores de preparación) | corrida D1 |
| Frontend unitario/componentes (Vitest) | 59 | 59 | 0 | 0 | `reports/i2.md` |
| E2E (Playwright, API simulada) | 7 por corrida, 3 corridas | 21 | 0 | 0 | `reports/i2.md` |
| Contrato (Newman/Postman) | PENDIENTE | | | | otro agente |
| Carga (k6) | PENDIENTE | | | | ver 4.4 |
| Seguridad dinámica (ZAP) | PENDIENTE | | | | ver 4.5 |
| Integración real con M3 (manual) | accidente 1; infracción confirmada 1; corte 1 (aprobar, activar, finalizar) | declarado correcto | 0 declarados | | EV-5; PENDIENTE adjuntar `eventId` y capturas |

Detalle de la corrida del grupo `bug` (comando: `./mvnw -q -B test -Dbugs.excluded=none -Dgroups=bug -Djacoco.skip=true`):
| Test | Resultado | Lectura |
|---|---|---|
| `SecurityEvidenceTests.bolaParkingSession` | FALLA: lectura 200 y cierre 200 con otro ciudadano | defecto real (DEF-15) |
| `SecurityEvidenceTests.jwtHasNoPii` | FALLA: el payload del JWT contiene el claim `nationalId` | defecto real (DEF-06) |
| `BugReproductionTest.bug04_infractionWithoutEvidence` | FALLA: 201 en lugar de 400 | defecto real (DEF-16) |
| `BugReproductionTest.bug09a_patchInfractionPartial` | FALLA: 400 en lugar de 200 | probable test desactualizado (`finalAmount=1` queda menor que el importe base tras el merge); no confirmado como defecto (DEF-17) |
| `BugReproductionTest.bug02_approveWithoutDetours`, `bug14_affectedStreetAlwaysEmpty` | ERROR: "affectedSections es obligatorio" al preparar el corte | test desactualizado respecto del handler; no hay veredicto sobre el producto (DEF-18) |
| Otros 16 (incluye `bug01_flagUnpaidAlways500`, `bug06`, `bug15`) | PASAN | el 500 de estacionamiento impago ya no se reproduce |

## 3. Cobertura
| Medición | Valor | Estado |
|---|---|---|
| Backend líneas (JaCoCo, gate 85 %) | 88,5 % | medido (I1) |
| Backend instrucciones / ramas | 87 % / 70 % | medido el 13/11, antes de IA y fixes; PENDIENTE de repetir |
| Backend por capa (servicio, web, auth, event, ai) | PENDIENTE | lo mide otro agente |
| Frontend líneas | 93,43 % | medido (I2) |
| Frontend ramas | 79 % | medido (B3f) |
| Mutation (pitest, perfil `mutation`) | PENDIENTE (objetivo >= 60 %) | |
| Tests flaky (suite repetida 3 veces) | Backend PENDIENTE; E2E 3/3 sin inestables | |

## 4. No funcional
### 4.1 Tiempos
| Medición | Valor | Fuente |
|---|---|---|
| `/actuator/health` con broker pausado, antes del fix | sin respuesta en 40 s | `reports/b4.md` |
| `/actuator/health` con broker pausado o detenido, después del fix | 200 UP en ~3 ms (local) | `reports/b4.md` |
| `/actuator/health` en producción tras el deploy del 15/11 | 0,7 s, UP | `TABLERO.md` |
| Arranque del compose local con imágenes en caché | 23 s (build en frío no medido) | `reports/a1.md` |
| Reconexión al broker tras stop/start (local) | ~9 s | `reports/a1.md` |
| Duración total del `verify` y de cada nivel | PENDIENTE | |

### 4.2 Performance (k6)
PENDIENTE. Escenario previsto: 50 usuarios, 5 minutos, 3 endpoints críticos, p95 < 500 ms, contra el compose local (no producción; el pool de base es de 3 conexiones). No se escribe ningún valor hasta tener el resultado.

### 4.3 Quality Gate
| Puerta | Umbral | Resultado |
|---|---|---|
| Cobertura backend | >= 85 % líneas | 88,5 %: cumple |
| Cobertura frontend | >= 85 % líneas | 93,43 %: cumple |
| Tests del build | 0 fallos | 0 fallos (285): cumple |
| E2E | >= 5 escenarios en CI | 7 escenarios; job `e2e` agregado al workflow; ejecución en runner de GitHub: ver CI del PR #8 (PENDIENTE de confirmar) |
| SonarCloud Quality Gate | aprobado | PENDIENTE (cuenta no creada) |
| Mutation | >= 60 % | PENDIENTE |
| Secretos | 0 | cumple con `.gitleaksignore` (4 falsos positivos o riesgos aceptados, ver DEF-12) |
| ZAP | sin alertas High | PENDIENTE |
| Dependencias | sin CVE crítica sin mitigación | PENDIENTE |
| CI verde sobre `v2.0-entrega4` | verde | PENDIENTE: el tag no existe |

### 4.4 Seguridad dinámica y dependencias
ZAP baseline y activo: PENDIENTE. Trivy (backend) y `npm audit` (frontend): PENDIENTE. Resultados en `06-informe-seguridad.md` cuando existan.

## 5. Defectos encontrados en esta ronda
Ver `05-registro-defectos.md`. En esta ronda, la ejecución del grupo `bug` confirmó tres defectos reales que no estaban en el listado de partida (DEF-15 BOLA de estacionamiento, DEF-16 evidencia no exigida, y la brecha de cálculo de importes DEF-19 detectada al revisar el código).

## 6. Conclusión provisoria
El build normal está en verde y supera los umbrales de cobertura, pero el informe NO puede declararse completo: faltan carga, mutación, ZAP, Sonar, dependencias, CI sobre tag y adjuntar la evidencia de la integración con M3. La condición de salida de defectos (0 críticos, máximo 3 mayores) no se cumple hoy (ver "Riesgo de salida" en 05).
