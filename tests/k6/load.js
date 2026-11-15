// Prueba de carga: 50 VUs x 5 minutos sobre 3 endpoints críticos del compose LOCAL.
//   1) POST /api/v1/auth/login            (autenticación, sincrónica)
//   2) GET  /api/v1/infractions           (listado de infracciones, INSPECTOR)
//   3) GET  /api/v1/street-closures/current (consulta pública de cortes vigentes)
// Umbrales: p(95)<500 ms y http_req_failed<1 %.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const API = `${BASE}/api/v1`;
const VUS = parseInt(__ENV.VUS || '50');
const DURATION = __ENV.DURATION || '5m';
// Contraseña de inspector1 del compose local: la pasa tests/run-k6.sh desde .env (STUB_PASSWORD_INSPECTOR1).

const tLogin = new Trend('t_login', true);
const tList = new Trend('t_list_infractions', true);
const tCuts = new Trend('t_street_closures_current', true);

export const options = {
  scenarios: {
    carga: { executor: 'constant-vus', vus: VUS, duration: DURATION },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    t_login: ['p(95)<500'],
    t_list_infractions: ['p(95)<500'],
    t_street_closures_current: ['p(95)<500'],
  },
};

const JSON_HDR = { 'Content-Type': 'application/json' };

export default function () {
  // Cada iteración loguea a inspector1 (credenciales de desarrollo del compose local).
  const login = http.post(`${API}/auth/login`, JSON.stringify({ username: 'inspector1', password: __ENV.STUB_PASSWORD_INSPECTOR1 }), { headers: JSON_HDR, tags: { endpoint: 'login' } });
  tLogin.add(login.timings.duration);
  const ok = check(login, { 'login 200': (r) => r.status === 200 });
  if (!ok) { sleep(1); return; }
  const auth = { headers: { Authorization: `Bearer ${login.json('token')}` } };

  const list = http.get(`${API}/infractions`, { ...auth, tags: { endpoint: 'list_infractions' } });
  tList.add(list.timings.duration);
  check(list, { 'listado 200': (r) => r.status === 200 });

  const cuts = http.get(`${API}/street-closures/current`, { tags: { endpoint: 'street_closures_current' } });
  tCuts.add(cuts.timings.duration);
  check(cuts, { 'cortes 200': (r) => r.status === 200 });

  sleep(1);
}
