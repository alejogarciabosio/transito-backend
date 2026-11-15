#!/usr/bin/env bash
# Prueba de carga con k6 (imagen oficial grafana/k6) contra el compose LOCAL.
# Uso: tests/run-k6.sh            (50 VUs, 5 min)    |    DURATION=30s VUS=10 tests/run-k6.sh  (humo)
# Resumen en tests/reports/k6-resumen.txt (se versiona) y JSON en tests/reports/k6-summary.json (ignorado).
set -euo pipefail
cd "$(dirname "$0")/.."
BASE_URL="${BASE_URL:-http://host.docker.internal:8080}"
case "$BASE_URL" in http://host.docker.internal:*|http://localhost:*|http://127.0.0.1:*) ;; *) echo "BASE_URL debe ser el compose local. Abortando." >&2; exit 2;; esac
curl -sf http://localhost:8080/actuator/health >/dev/null || { echo "El backend no responde en localhost:8080." >&2; exit 2; }
mkdir -p tests/reports
set +e
docker run --rm -i --add-host=host.docker.internal:host-gateway \
  -e BASE_URL="$BASE_URL" -e VUS="${VUS:-50}" -e DURATION="${DURATION:-5m}" \
  -v "$PWD/tests:/tests" grafana/k6:latest run --no-color \
  --summary-export /tests/reports/k6-summary.json /tests/k6/load.js 2>&1 | tee tests/reports/k6-salida.log
RC=${PIPESTATUS[0]}
set -e
{
  echo "k6 - $(date -u +%Y-%m-%dT%H:%MZ) - ${VUS:-50} VUs x ${DURATION:-5m} contra compose local (pool Hikari por defecto = 3). exit=$RC (0 = umbrales OK, 99 = umbral incumplido)"
  sed -n '/█ THRESHOLDS/,$p' tests/reports/k6-salida.log
} > tests/reports/k6-resumen.txt
exit $RC
