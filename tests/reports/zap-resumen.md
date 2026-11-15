# Resumen OWASP ZAP

Ejecutado el 15/11/2026 con `ghcr.io/zaproxy/zaproxy:2.17.0` contra el compose local (baseline + API scan activo autenticado).
**0 High** en todos los escaneos. 1.ª corrida del API scan: Medium 1 (500 por textos >255 / byte NUL), Low 2, Info 3.
Tras agregar `@Size(max = 255)` y rechazar NUL (400): **Medium 0**, Low 1 (cabecera CORP), Info 3.
Swagger/api-docs públicos en prod: riesgo aceptado (integración entre módulos).
Detalle, tratamiento y reportes HTML/JSON: [`docs/entrega-academica/testing/evidencias/zap/`](../../docs/entrega-academica/testing/evidencias/zap/README.md).
