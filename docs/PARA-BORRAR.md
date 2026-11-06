# Candidatos a borrar — revisión 2026-11-06

> Yo no borré nada. Esto es una propuesta para que decidas vos. Si estás de acuerdo, los eliminás y después borrás este archivo también.

## Para borrar directamente (contenido superado, sin valor histórico claro)

- **`docs/archivo/estado-integraciones-2026-10.md`** — Superado por `docs/contratos-integracion/dependencias-externas-pendientes.md` y `docs/contratos-integracion/contrato-m3-m7-v1.md`. Además tiene información que ya quedó **contradicha** por el código actual (dice que el contrato SOAP "solo falta publicar la URL real de M1" — hoy ya existen tanto el cliente SOAP como el cliente REST contra M1, y menciona `TRAFFIC_FINE` como el `originType` de Rentas, cuando en otro lado del código se confirmó `TRAFFIC_INFRACTION`). Dejarlo puede confundir a alguien que lo lea sin saber que está desactualizado.

- **`docs/archivo/pr-validacion-negocio-2026-10.md`** — Era el texto que armamos para pegar en la descripción de un Pull Request puntual (el del cherry-pick de validación de negocio). Ese PR ya se aceptó y se fusionó hace semanas; el documento cumplió su función y ya no es información de referencia para nadie, es historial de un trámite cerrado.

## Para revisar con más calma (menos seguro, decisión tuya)

- **`docs/archivo/cambios-frontend-2026-10.md`** — Era un "delta" puntual para tu compañero de Frontend, de mediados de octubre. Desde entonces pasaron bastantes cambios más (reducción a 3 roles, nuevos endpoints, etc.) que este documento no cubre — ya no es un resumen completo de lo que cambió. Si tu compañero ya lo incorporó en su momento, no tiene mucho sentido mantenerlo; si pensás armar un delta nuevo y más actual, podrías directamente reemplazarlo en vez de sumar otro archivo.

- **`docs/archivo/modulo7-crud-endpoints.md`** — El propio archivo arranca diciendo *"MODELO INICIAL - ACTUALMENTE DESACTUALIZADO"*. Ya se autodeclaró obsoleto; lo dejé en el archivo histórico en vez de borrarlo directamente porque alguien lo escribió a propósito así, pero si nadie lo necesita para el contexto de cómo arrancó el proyecto, se puede borrar.

## No tocar — aunque parezcan viejos, están guardados a propósito

Estos NO son candidatos a borrar, aunque están en `docs/archivo/`: el propio contenido de otros documentos vigentes los menciona explícitamente como historial que se conserva a propósito (trazabilidad de contratos anteriores):

- `docs/archivo/guia-integracion-frontend.md` (se autodeclara "documento archivado", citado desde el delta del frontend)
- `docs/archivo/modulo7-payloads-v3-pulidos.md` (el documento que lo reemplazó dice textualmente que "queda como historial de definiciones previas")
- `docs/archivo/historial-payloads/` (ya vive en una carpeta que se llama "historial", es la intención original)
