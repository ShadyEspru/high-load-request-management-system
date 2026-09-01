import { login, createRequest } from '../helpers.js';

export const options = {
  scenarios: {
    diagnose_cause: {
      executor: 'constant-arrival-rate',
      rate: 100,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 100,
      maxVUs: 150,
    },
  },
};

export function setup() {
  return {
    token: login(),
  };
}

export default function (data) {
  const response = createRequest(data.token);

  if (response.status === 503) {
    console.error(
      `DIAG_503 vu=${__VU} iter=${__ITER} body=${response.body}`
    );
  }
}
