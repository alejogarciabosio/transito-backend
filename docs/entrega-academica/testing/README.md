# Entregables del QA (ISO/IEC/IEEE 29119-3) - M7 Tránsito

Borrador del 15/11/2026, redactado por un agente de IA (D1) con evidencia verificada contra `main`; debe ser revisado por una persona del equipo antes de exportarlo al PDF `Clase15_Grupo9_Transito_Testing.pdf`. Regla de honestidad: donde no hay evidencia ejecutada dice **PENDIENTE** o **NO VERIFICABLE**.

| N.º | Documento | Contenido |
|---|---|---|
| 01 | [01-plan-y-estrategia.md](01-plan-y-estrategia.md) | alcance, niveles, entornos, datos, criterios de entrada y salida, riesgos (P x I) |
| 02 | [02-matriz-trazabilidad.md](02-matriz-trazabilidad.md) | HU F1-F7 -> criterio -> caso -> test -> resultado -> evidencia -> defecto; huecos marcados |
| 03 | [03-especificacion-casos.md](03-especificacion-casos.md) | 112 filas de casos; 70,5 % negativos o de borde |
| 04 | [04-informe-ejecucion.md](04-informe-ejecucion.md) | tablas por nivel, cobertura, tiempos, Quality Gate; PENDIENTE: k6, mutation, ZAP, Sonar, dependencias |
| 05 | [05-registro-defectos.md](05-registro-defectos.md) | 21 defectos y "Riesgo de salida" (excede el límite) |
| 06 | [06-informe-seguridad.md](06-informe-seguridad.md) | matriz SEC y criterios adicionales, con estado y evidencia |
| 07 | [07-declaracion-ia.md](07-declaracion-ia.md) | herramientas, errores de la IA detectados, plan de IA (8.2) |
| 08 | [08-lecciones-aprendidas.md](08-lecciones-aprendidas.md) | tres defectos tardíos y qué prueba los habría atrapado |
| - | [CORRECCIONES-DOC-GOOGLE.md](CORRECCIONES-DOC-GOOGLE.md) | lista exacta de correcciones al Google Doc del TPO |

## Qué falta (resumen)
k6 (p95), mutation (pitest), ZAP, SonarCloud, escaneo de dependencias, CI verde sobre el tag `v2.0-entrega4`, evidencia (`eventId`, capturas) de las integraciones reales con M3, corrida real de la evaluación de IA con Groq, y la repetición de la suite 3 veces para flaky en backend.

## Cómo reproducir lo ejecutado
- Backend: `./mvnw -q -B verify` (cobertura mínima 85 %).
- Grupo de reproducciones (excluido del build): `./mvnw -q -B test -Dbugs.excluded=none -Dgroups=bug -Djacoco.skip=true`.
- Frontend: `npm run lint`, `npm run test:coverage`, `npm run build`, `npm run test:e2e`.
