import http from 'k6/http';
import { check, fail } from 'k6';

const USER_ID = __ENV.DIRECT_USER_ID;
const USER_EMAIL = __ENV.DIRECT_USER_EMAIL;
const USER_ROLES = __ENV.DIRECT_USER_ROLES;

const TEST_RUN_ID =
  __ENV.TEST_RUN_ID || `gateway-nojwt-${Date.now()}`;

export const options = {
  scenarios: {
    gateway_nojwt_500: {
      executor: 'constant-arrival-rate',
      rate: 500,
      timeUnit: '1s',
      duration: '5s',
      preAllocatedVUs: 600,
      maxVUs: 800,
    },
  },
};

export function setup() {
  if (!USER_ID || !USER_EMAIL || !USER_ROLES) {
    fail('DIRECT_USER_* variables are missing');
  }
}

export default function () {
  const id =
    `${TEST_RUN_ID}-${__VU}-${__ITER}-${Date.now()}`;

  const payload = JSON.stringify({
    requestType: 'STANDARD',
    payload: JSON.stringify({
      source: 'k6-gateway-nojwt',
      testRunId: TEST_RUN_ID,
      uniqueId: id,
    }),
  });

  const res = http.post(
    'http://localhost:8088/api/v1/perf/requests',
    payload,
    {
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': USER_ID,
        'X-User-Email': USER_EMAIL,
        'X-User-Roles': USER_ROLES,
        'X-Correlation-ID': id,
        'Idempotency-Key': id,
      },
      timeout: '10s',
    }
  );

  check(res, {
    'status is 201': (r) => r.status === 201,
    'response is JSON': (r) => {
      try {
        JSON.parse(r.body);
        return true;
      } catch (_) {
        return false;
      }
    },
    'response contains id': (r) => {
      try {
        return !!JSON.parse(r.body).id;
      } catch (_) {
        return false;
      }
    },
  });
}
