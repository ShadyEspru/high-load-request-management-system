import http from 'k6/http';
import { Counter } from 'k6/metrics';

const status201 = new Counter('status_201');
const status202 = new Counter('status_202');
const status400 = new Counter('status_400');
const status401 = new Counter('status_401');
const status429 = new Counter('status_429');
const status500 = new Counter('status_500');
const status502 = new Counter('status_502');
const status503 = new Counter('status_503');
const statusOther = new Counter('status_other');

export const options = {
  scenarios: {
    diagnose: {
      executor: 'constant-arrival-rate',
      rate: 100,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 100,
      maxVUs: 150,
    },
  },
};

export default function () {
  const key = `diag-${__VU}-${__ITER}-${Date.now()}`;

  const response = http.post(
    `${__ENV.BASE_URL}/api/v1/requests`,
    JSON.stringify({
      requestType: 'STANDARD',
      payload: JSON.stringify({
        source: 'k6-diagnostic',
        uniqueId: key,
      }),
    }),
    {
      headers: {
        Authorization: `Bearer ${__ENV.AUTH_TOKEN}`,
        'Content-Type': 'application/json',
        'Idempotency-Key': key,
        'X-Correlation-ID': key,
      },
    }
  );

  switch (response.status) {
    case 201: status201.add(1); break;
    case 202: status202.add(1); break;
    case 400: status400.add(1); break;
    case 401: status401.add(1); break;
    case 429: status429.add(1); break;
    case 500: status500.add(1); break;
    case 502: status502.add(1); break;
    case 503: status503.add(1); break;
    default:
      statusOther.add(1);
      console.error(
        `Unexpected status=${response.status} body=${response.body}`
      );
  }
}
