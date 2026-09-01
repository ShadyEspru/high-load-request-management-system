import http from 'k6/http';
import { check } from 'k6';

const TOKEN = __ENV.AUTH_TOKEN;

export const options = {
  vus: 20,
  iterations: 100,
};

export default function () {
  const id = `jwt-warmup-${__VU}-${__ITER}-${Date.now()}`;

  const res = http.post(
    'http://localhost:8088/api/v1/perf/requests',
    JSON.stringify({
      requestType: 'STANDARD',
      payload: JSON.stringify({
        source: 'jwt-single-parse-warmup',
        uniqueId: id,
      }),
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${TOKEN}`,
        'X-Correlation-ID': id,
        'Idempotency-Key': id,
      },
    }
  );

  check(res, {
    'status is 201': (r) => r.status === 201,
  });
}
