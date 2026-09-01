import { login, createRequest } from '../helpers.js';

let loggedFailure = false;

export const options = {
  scenarios: {
    diagnose_500: {
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
  return {
    token: login(),
  };
}

export default function (data) {
  const response = createRequest(data.token);

  if (
    response.status !== 201 &&
    response.status !== 202 &&
    !loggedFailure
  ) {
    loggedFailure = true;

    console.error(
      `FIRST_FAILURE vu=${__VU} iter=${__ITER} ` +
      `status=${response.status} body=${response.body}`
    );
  }
}
