#!/usr/bin/env bash
# Corre la colección Postman con Newman contra el compose LOCAL (nunca producción).
# Uso: tests/run-newman.sh            (requiere `docker compose up -d` y un .env con JWT_SECRET y CORE_RABBITMQ_PASSWORD)
# Variables opcionales: BASE_URL (def. http://localhost:8080), RABBIT_URL (def. http://localhost:15672)
set -euo pipefail
cd "$(dirname "$0")/.."

BASE_URL="${BASE_URL:-http://localhost:8080}"
RABBIT_URL="${RABBIT_URL:-http://localhost:15672}"
case "$BASE_URL" in http://localhost:*|http://127.0.0.1:*) ;; *) echo "BASE_URL debe ser local (localhost/127.0.0.1). Abortando." >&2; exit 2;; esac

# Lee un valor de .env sin imprimirlo ni hacer source del archivo.
envval() { [ -n "${!1:-}" ] && { printf '%s' "${!1}"; return; }; [ -f .env ] && grep -E "^$1=" .env | head -1 | cut -d= -f2- || true; }
JWT_SECRET="$(envval JWT_SECRET)"
RABBIT_PASS="$(envval CORE_RABBITMQ_PASSWORD)"
[ -n "$JWT_SECRET" ] && [ -n "$RABBIT_PASS" ] || { echo "Falta JWT_SECRET o CORE_RABBITMQ_PASSWORD (en .env o en el entorno)." >&2; exit 2; }

curl -sf "$BASE_URL/actuator/health" >/dev/null || { echo "El backend no responde en $BASE_URL. Levantá el compose primero." >&2; exit 2; }

RUN_ID="$(date +%s | tail -c 7)$RANDOM"
mkdir -p tests/reports

# Siembra 2 pedidos de corte (A: aprobar/activar/finalizar; R: rechazar) por el broker RabbitMQ local,
# como lo haría M3 vía Core. Los cortes no se crean por REST.
publish_closure() { # $1 = sufijo
  RUN_ID="$RUN_ID" SUF="$1" node -e '
    const id=require("crypto").randomUUID();
    const data={closureRequestId:"CR-NM-"+process.env.RUN_ID+"-"+process.env.SUF,sourceModule:"M3",sourceRef:"OBRA-NM",reason:"Corte de prueba (Newman)",
      affectedSections:[{street:"Av. Test",from:"A",to:"B",direction:"BOTH"}],requestedFrom:"2026-12-01T08:00:00-03:00",requestedTo:"2026-12-02T08:00:00-03:00",
      closureType:"PARTIAL",requestedAt:"2026-11-15T10:00:00-03:00"};
    process.stdout.write(JSON.stringify({properties:{},routing_key:"q.transito",payload:JSON.stringify({eventId:id,eventType:"streetClosureRequested",data}),payload_encoding:"string"}));' |
  curl -sf -u "module_transito:$RABBIT_PASS" -H 'content-type: application/json' -X POST \
    "$RABBIT_URL/api/exchanges/%2F/amq.default/publish" -d @- >/dev/null
}
publish_closure A
publish_closure R
sleep 3

if command -v newman >/dev/null 2>&1; then NEWMAN=(newman); else NEWMAN=(npx --yes -p newman@6.2.1 -p newman-reporter-htmlextra@1.23.1 newman); fi

set +e
"${NEWMAN[@]}" run tests/postman/transito-api.postman_collection.json \
  -e tests/postman/transito-local.postman_environment.json \
  --env-var "base_url=$BASE_URL" --env-var "run_id=$RUN_ID" --env-var "jwt_secret=$JWT_SECRET" \
  --reporters cli,junit,htmlextra \
  --reporter-junit-export tests/reports/newman-junit.xml \
  --reporter-htmlextra-export tests/reports/newman-report.html \
  --reporter-htmlextra-title "Tránsito M7 - pruebas de API" \
  --color off --timeout-request 20000
RC=$?
set -e
echo "Reportes: tests/reports/newman-junit.xml y tests/reports/newman-report.html (exit=$RC)"
exit $RC
