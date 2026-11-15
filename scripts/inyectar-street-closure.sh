#!/usr/bin/env bash
# Inyecta un streetClosureRequested de prueba en q.transito del RabbitMQ LOCAL (docker compose).
# Uso: ./scripts/inyectar-street-closure.sh [sourceRef]   (por defecto OT-LOCAL-<timestamp>)
# Lee CORE_RABBITMQ_PASSWORD del .env sin imprimirla. Publica por la API de administración (localhost:15672).
set -euo pipefail
cd "$(dirname "$0")/.."

PASS="$(grep -E '^CORE_RABBITMQ_PASSWORD=' .env | head -1 | cut -d= -f2-)"
[ -n "$PASS" ] || { echo "Falta CORE_RABBITMQ_PASSWORD en .env" >&2; exit 1; }

uuid() { uuidgen | tr 'A-Z' 'a-z'; }
REF="${1:-OT-LOCAL-$(date +%s)}"
NOW="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
FROM="$(date -u -v+1H +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -d '+1 hour' +%Y-%m-%dT%H:%M:%SZ)"
TO="$(date -u -v+9H +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -d '+9 hours' +%Y-%m-%dT%H:%M:%SZ)"
EVENT_ID="$(uuid)"

EVENT=$(cat <<JSON
{"eventId":"$EVENT_ID","eventType":"streetClosureRequested","eventVersion":"1.0","occurredAt":"$NOW",
 "sourceModule":"obras","correlationId":"$(uuid)","causationId":null,
 "data":{"closureRequestId":"$(uuid)","sourceModule":"obras","sourceRef":"$REF","reason":"Reparación de calzada (prueba local)",
  "affectedSections":[{"street":"Av. Rivadavia","from":"Av. La Plata","to":"Muñiz","direction":"BOTH"}],
  "requestedFrom":"$FROM","requestedTo":"$TO","closureType":"PARTIAL","requestedAt":"$NOW"}}
JSON
)
# Publica en el exchange por defecto con routing key q.transito (cola de la app).
BODY=$(printf '%s' "$EVENT" | python3 -c 'import json,sys; e=sys.stdin.read(); print(json.dumps({"properties":{"content_type":"application/json","delivery_mode":2},"routing_key":"q.transito","payload":e,"payload_encoding":"string"}))')
RESP=$(curl -sS -u "module_transito:$PASS" -H 'content-type: application/json' \
  -X POST "http://localhost:15672/api/exchanges/%2F/amq.default/publish" -d "$BODY")
echo "eventId=$EVENT_ID sourceRef=$REF -> $RESP"
echo "Verificar: curl -s -H 'Authorization: Bearer <login admin1>' http://localhost:8080/api/v1/street-closures"
