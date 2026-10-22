import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 200 },  // ramp up to 200 users
    { duration: '1m', target: 1000 },   // peak at 1000 users
    { duration: '2m', target: 1000 },   // stay at 1000 users
    { duration: '30s', target: 0 },     // ramp down
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],    // http errors should be less than 1%
    http_req_duration: ['p(95)<1500'], // 95% of requests should be below 1500ms
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  // Register a benchmark user and get token
  const email = `load_test_${Date.now()}@example.com`;
  const password = 'BenchmarkPassword123!';
  const signupRes = http.post(`${BASE_URL}/api/auth/signup`, JSON.stringify({ email, password }), {
    headers: { 'Content-Type': 'application/json' },
  });
  const token = signupRes.json('token');
  return { token };
}

export default function (data) {
  const params = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${data.token}`,
    },
  };

  const res = http.get(`${BASE_URL}/api/homepage?lat=19.15&lon=77.30`, params);
  check(res, {
    'status is 200': (r) => r.status === 200,
    'widgets returned': (r) => {
      try {
        const body = JSON.parse(r.body);
        return Array.isArray(body.widgets);
      } catch (e) {
        return false;
      }
    },
  });

  sleep(1);
}
