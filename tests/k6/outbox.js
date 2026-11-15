// Escenario COMPLEMENTARIO (no reemplaza a load.js): carga sobre un endpoint que publica un evento vía
// Transactional Outbox. POST /api/v1/infractions guarda la infracción y el evento infractionRegistered en
// outbox_events en la misma transacción; el OutboxRelay lo publica después al RabbitMQ LOCAL del compose.
// Se loguea UNA sola vez en setup() y se reutiliza el token (evita el límite de login por usuario/IP).
// Solo contra el compose local: BASE_URL por defecto http://host.docker.internal:8080. Nunca producción.
// Umbrales: los mismos que load.js (p95 < 500 ms y errores < 1 %).
import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Trend } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://host.docker.internal:8080';
if (!/^http:\/\/(host\.docker\.internal|localhost|127\.0\.0\.1):\d+$/.test(BASE)) {
  throw new Error('BASE_URL debe ser el compose local');
}
const API = `${BASE}/api/v1`;
const VUS = parseInt(__ENV.VUS || '50');
const DURATION = __ENV.DURATION || '2m';

const tCreate = new Trend('t_create_infraction', true);

export const options = {
  scenarios: {
    outbox: { executor: 'constant-vus', vus: VUS, duration: DURATION },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    t_create_infraction: ['p(95)<500'],
  },
};

const JSON_HDR = { 'Content-Type': 'application/json' };

export function setup() {
  const r = http.post(`${API}/auth/login`, JSON.stringify({ username: 'inspector1', password: __ENV.STUB_PASSWORD_INSPECTOR1 }), { headers: JSON_HDR });
  if (r.status !== 200) fail(`login de setup: HTTP ${r.status}`);
  return { token: r.json('token') };
}

export default function (data) {
  const body = JSON.stringify({
    licensePlate: `K6${__VU}X${__ITER}`.slice(0, 15),
    infractionType: 'ILLEGAL_PARKING',
    infractionDateTime: '2026-08-24T13:45:00',
    location: { street: 'Av. Cabildo', crossStreet: 'Juramento', latitude: -34.5621, longitude: -58.4563 },
    evidenceUrl: ['https://evidencias.example.org/f1.jpg'],
    offenderNationalId: '30777888',
    baseAmount: 50000,
    aggravatingFactors: ['REINCIDENCE'],
    finalAmount: 75000,
  });
  const res = http.post(`${API}/infractions`, body, {
    headers: { ...JSON_HDR, Authorization: `Bearer ${data.token}` },
    tags: { endpoint: 'create_infraction' },
  });
  tCreate.add(res.timings.duration);
  check(res, { 'alta 201': (r) => r.status === 201 });
  sleep(1);
}
