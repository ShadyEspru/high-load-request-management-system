import http from 'k6/http';
import { check } from 'k6';

const BASE_URL =
  __ENV.DIRECT_RS_URL || 'http://localhost:8080';

const USER_ID = __ENV.DIRECT_USER_ID;
const USER_EMAIL = __ENV.DIRECT_USER_EMAIL;
const USER_ROLES = __ENV.DIRECT_USER_ROLES;

const TEST_RUN_ID =
  __ENV.TEST_RUN_ID || `direct-rs-${Date.now()}`;

export const options = {
  scenarios: {
    direct_request_service_500: {
      executor: 'constant-arrival-rate',
      rate: 500,
      timeUnit: '1s',
      duration: '5s',
      preAllocatedVUs: 600,
      maxVUs: 800,
    },
  },
};

export default function () {
  const unique =
    `${TEST_RUN_ID}-${__VU}-${__ITER}-${Date.now()}`;

  const payload = JSON.stringify({
    requestType: 'STANDARD',
    payload: JSON.stringify({
      source: 'k6-direct-request-service',
      testRunId: TEST_RUN_ID,
      uniqueId: unique,
    }),
  });

  const response = http.post(
    `${BASE_URL}/api/v1/requests`,
    payload,
    {
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': USER_ID,
        'X-User-Email': USER_EMAIL,
        'X-User-Roles': USER_ROLES,
        'X-Correlation-ID': unique,
        'Idempotency-Key': unique,
      },
      tags: {
        endpoint: 'direct-request-service',
      },
      timeout: '10s',
    }
  );

  check(response, {
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
