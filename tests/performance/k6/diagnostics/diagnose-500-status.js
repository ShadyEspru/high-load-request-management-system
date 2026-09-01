import { Counter } from 'k6/metrics';

import {
  createRequest,
  login,
} from '../helpers.js';

const status0   = new Counter('status_0');
const status201 = new Counter('status_201');
const status202 = new Counter('status_202');
const status400 = new Counter('status_400');
const status401 = new Counter('status_401');
const status408 = new Counter('status_408');
const status409 = new Counter('status_409');
const status429 = new Counter('status_429');
const status500 = new Counter('status_500');
const status502 = new Counter('status_502');
const status503 = new Counter('status_503');
const status504 = new Counter('status_504');
const statusOther = new Counter('status_other');

export const options = {
  scenarios: {
    diagnose_500: {
      executor: 'constant-arrival-rate',

      rate: Number(
        __ENV.DIAG_RATE || 500
      ),

      timeUnit: '1s',

      duration:
        __ENV.DIAG_DURATION || '5s',

      preAllocatedVUs: Number(
        __ENV.DIAG_VUS || 3000
      ),

      maxVUs: Number(
        __ENV.DIAG_VUS || 3000
      ),

      gracefulStop: '30s',
    },
  },

  summaryTrendStats: [
    'avg',
    'min',
    'med',
    'p(90)',
    'p(95)',
    'p(99)',
    'max',
  ],
};

export function setup() {
  return {
    token: login(),
  };
}

export default function (data) {
  const response =
    createRequest(data.token);

  switch (response.status) {
    case 0:
      status0.add(1);
      break;

    case 201:
      status201.add(1);
      break;

    case 202:
      status202.add(1);
      break;

    case 400:
      status400.add(1);
      break;

    case 401:
      status401.add(1);
      break;

    case 408:
      status408.add(1);
      break;

    case 409:
      status409.add(1);
      break;

    case 429:
      status429.add(1);
      break;

    case 500:
      status500.add(1);
      break;

    case 502:
      status502.add(1);
      break;

    case 503:
      status503.add(1);
      break;

    case 504:
      status504.add(1);
      break;

    default:
      statusOther.add(1);
      break;
  }
}
