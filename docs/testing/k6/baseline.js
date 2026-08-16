import {
  TEST_RUN_ID,
} from './config.js';

import {
  createRequest,
  login,
} from './helpers.js';

export const options = {
  scenarios: {
    baseline_100_rps: {
      executor: 'constant-arrival-rate',

      rate: Number(
        __ENV.BASELINE_RATE || 100
      ),

      timeUnit: '1s',

      duration:
        __ENV.BASELINE_DURATION || '30s',

      preAllocatedVUs: Number(
        __ENV.BASELINE_VUS || 150
      ),

      gracefulStop: '30s',

      exec: 'createOnly',

      tags: {
        test_type: 'baseline',
        test_run_id: TEST_RUN_ID,
      },
    },
  },

  thresholds: {
    http_req_failed: [
      'rate<0.01',
    ],

    'http_req_duration{endpoint:create-request}': [
      'p(95)<500',
      'p(99)<1000',
    ],

    checks: [
      'rate>0.99',
    ],

    business_error_rate: [
      'rate<0.01',
    ],

    request_creation_failures: [
      'count<1',
    ],

    unexpected_status_codes: [
      'count<1',
    ],

    dropped_iterations: [
      'count<1',
    ],
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
  const token = login();

  return {
    token,
    testRunId: TEST_RUN_ID,
  };
}

export function createOnly(data) {
  createRequest(data.token);
}
