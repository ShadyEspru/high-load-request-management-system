import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    gateway_echo_500: {
      executor: 'constant-arrival-rate',
      rate: 500,
      timeUnit: '1s',
      duration: '5s',
      preAllocatedVUs: 100,
      maxVUs: 400,
    },
  },
};

export default function () {
  const id = `gateway-echo-${__VU}-${__ITER}-${Date.now()}`;

  const res = http.post(
    'http://localhost:8088/api/v1/perf/echo',
    JSON.stringify({
      source: 'k6-gateway-echo',
      uniqueId: id,
      message: 'performance-test',
    }),
    {
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: '10s',
    }
  );

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response is JSON': (r) => {
      try {
        JSON.parse(r.body);
        return true;
      } catch (_) {
        return false;
      }
    },
  });
}
