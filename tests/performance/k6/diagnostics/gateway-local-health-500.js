import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    gateway_local_500: {
      executor: 'constant-arrival-rate',
      rate: 500,
      timeUnit: '1s',
      duration: '5s',
      preAllocatedVUs: 200,
      maxVUs: 800,
    },
  },
};

export default function () {
  const res = http.get(
    'http://localhost:8088/actuator/health',
    { timeout: '10s' }
  );

  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
