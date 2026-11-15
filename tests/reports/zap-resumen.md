# Resumen OWASP ZAP

Ejecutado el 15/11/2026 con `ghcr.io/zaproxy/zaproxy:2.17.0` contra el compose local (baseline + API scan activo autenticado).
**0 High** en ambos escaneos. API scan: Medium 1 (Buffer Overflow → 500 por varchar(255) sin `@Size`), Low 2, Info 3.
Detalle, tratamiento y reportes HTML/JSON: [`docs/entrega-academica/testing/evidencias/zap/`](../../docs/entrega-academica/testing/evidencias/zap/README.md).
