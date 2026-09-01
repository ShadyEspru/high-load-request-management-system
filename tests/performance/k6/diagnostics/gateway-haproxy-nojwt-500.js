import http from 'k6/http';
import { check } from 'k6';

const USER_ID = __ENV.DIRECT_USER_ID;
const USER_EMAIL = __ENV.DIRECT_USER_EMAIL;
const USER_ROLES = __ENV.DIRECT_USER_ROLES;
const TEST_RUN_ID = __ENV.TEST_RUN_ID || `nojwt-${Date.now()}`;

export const options = {
  scenarios: {
    gateway_nojwt_500: {
      executor: 'constant-arrival-rate',
      rate: 500,
      timeUnit: '1s',
      duration: '5s',
      preAllocatedVUs: 3000,
      maxVUs: 3000,
    },
  },
};

export default function () {
  const uniqueId =
    `${TEST_RUN_ID}-${__VU}-${__ITER}-${Date.now()}`;

  const body = JSON.stringify({
    requestType: 'STANDARD',
    payload: JSON.stringify({
      source: 'k6-gateway-haproxy-nojwt',
      operation: 'performance-test',
      testRunId: TEST_RUN_ID,
      uniqueId: uniqueId,
    }),
  });

  const res = http.post(
    'http://localhost:8088/api/v1/perf/requests',
    body,
    {
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': USER_ID,
        'X-User-Email': USER_EMAIL,
        'X-User-Roles': USER_ROLES,
        'Idempotency-Key': uniqueId,
        'X-Correlation-ID': uniqueId,
      },
      timeout: '15s',
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
