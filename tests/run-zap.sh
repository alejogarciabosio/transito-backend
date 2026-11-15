#!/usr/bin/env bash
# OWASP ZAP (imagen oficial) contra el compose LOCAL: baseline + escaneo ACTIVO autenticado del API (INSPECTOR).
# Uso: tests/run-zap.sh [baseline|api|todo]     (def.: todo).  El escaneo activo ataca el backend local: puede tardar y llena la base local.
# Nunca apunta a producción: el OpenAPI se descarga del backend local y se reescribe `servers` al host local.
set -euo pipefail
cd "$(dirname "$0")/.."
MODE="${1:-todo}"
IMG="ghcr.io/zaproxy/zaproxy:stable"
TARGET="http://host.docker.internal:8080"
OUT="$PWD/tests/reports/zap"
mkdir -p "$OUT"; chmod 777 "$OUT"
curl -sf http://localhost:8080/actuator/health >/dev/null || { echo "El backend no responde en localhost:8080." >&2; exit 2; }

if [ "$MODE" = baseline ] || [ "$MODE" = todo ]; then
  echo "== ZAP baseline (pasivo, sin autenticar) =="
  docker run --rm --add-host=host.docker.internal:host-gateway -v "$OUT:/zap/wrk:rw" "$IMG" \
    zap-baseline.py -t "$TARGET" -I -r zap-baseline.html -J zap-baseline.json -w zap-baseline.md || true
fi

if [ "$MODE" = api ] || [ "$MODE" = todo ]; then
  echo "== ZAP API scan activo autenticado (INSPECTOR) =="
  # Token de inspector1 (credenciales de desarrollo del compose local). No se imprime.
  ZAP_AUTH_HEADER_VALUE="Bearer $(curl -sf -H 'content-type: application/json' -d '{"username":"inspector1","password":"inspector123"}' http://localhost:8080/api/v1/auth/login | sed 's/.*"token":"\([^"]*\)".*/\1/')"
  export ZAP_AUTH_HEADER_VALUE
  # OpenAPI completo generado por springdoc (incluye todos los endpoints); servers forzado al host local.
  curl -sf http://localhost:8080/v3/api-docs | node -e '
    let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{const o=JSON.parse(s);o.servers=[{url:"http://host.docker.internal:8080"}];process.stdout.write(JSON.stringify(o));});' > "$OUT/openapi-local.json"
  grep -q 'railway' "$OUT/openapi-local.json" && { echo "El OpenAPI menciona Railway; abortando por seguridad." >&2; exit 3; }
  docker run --rm --add-host=host.docker.internal:host-gateway -v "$OUT:/zap/wrk:rw" \
    -e ZAP_AUTH_HEADER=Authorization -e ZAP_AUTH_HEADER_VALUE "$IMG" \
    zap-api-scan.py -t /zap/wrk/openapi-local.json -f openapi -I -r zap-api.html -J zap-api.json -w zap-api.md || true
fi
echo "Reportes en tests/reports/zap/ (no versionados). Resumen manual: tests/reports/zap-resumen.md"
