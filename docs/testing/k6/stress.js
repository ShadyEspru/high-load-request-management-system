import { check } from 'k6';

import {
  STATUS,
  TEST_RUN_ID,
  debug,
} from './config.js';

import {
  createRequest,
  extractRequestId,
  getRequest,
  login,
  parseJson,
  randomPayload,
  sleepRandom,
} from './helpers.js';

import {
  stressThresholds,
} from './thresholds.js';

/**
 * ============================================================
 * Test Options
 * ============================================================
 */

export const options = {
  scenarios: {
    stress_test: {
      executor: 'ramping-vus',

      startVUs: 0,

      stages: [
        {
          duration: '1m',
          target: 25,
        },

        {
          duration: '3m',
          target: 25,
        },

        {
          duration: '1m',
          target: 50,
        },

        {
          duration: '3m',
          target: 50,
        },

        {
          duration: '1m',
          target: 100,
        },

        {
          duration: '3m',
          target: 100,
        },

        {
          duration: '1m',
          target: 150,
        },

        {
          duration: '3m',
          target: 150,
        },

        {
          duration: '2m',
          target: 0,
        },
      ],

      gracefulRampDown: '30s',

      exec: 'stressScenario',
    },
  },

  thresholds: stressThresholds,

  tags: {
    test_type: 'stress',
    test_run_id: TEST_RUN_ID,
  },
};

/**
 * ============================================================
 * Test Setup
 * ============================================================
 */

export function setup() {
  debug(
    'Starting stress test setup.',
    {
      testRunId: TEST_RUN_ID,
    },
  );

  const token = login();

  return {
    token,
    testRunId: TEST_RUN_ID,
  };
}

/**
 * ============================================================
 * Stress Test Scenario
 * ============================================================
 */

export function stressScenario(data) {
  const token = data.token;

  /**
   * ----------------------------------------------------------
   * Create Request
   * ----------------------------------------------------------
   */

  const payload =
    randomPayload({
      payload: {
        scenario: 'stress',
      },
    });

  const createResponse =
    createRequest(
      token,
      payload,
    );

  const createBody =
    parseJson(createResponse);

  const requestId =
    extractRequestId(createBody);

  const requestCreated =
    check(createResponse, {
      'stress: request creation accepted':
        (response) =>
          response.status === STATUS.CREATED ||
          response.status === STATUS.ACCEPTED,

      'stress: request id exists':
        () =>
          Boolean(requestId),
    });

  if (!requestCreated || !requestId) {
    debug(
      'Stress iteration stopped because request creation failed.',
      {
        status:
          createResponse.status,

        response:
          createResponse.body,
      },
    );

    sleepRandom(0.1, 0.5);

    return;
  }

  /**
   * ----------------------------------------------------------
   * Request Retrieval
   * ----------------------------------------------------------
   */

  sleepRandom(0.1, 0.4);

  const getResponse =
    getRequest(
      token,
      requestId,
    );

  check(getResponse, {
    'stress: request retrieval succeeded':
      (response) =>
        response.status === STATUS.OK,
  });

  /**
   * ----------------------------------------------------------
   * Think Time
   * ----------------------------------------------------------
   */

  sleepRandom(0.2, 1);
}

/**
 * ============================================================
 * Test Teardown
 * ============================================================
 */

export function teardown(data) {
  debug(
    'Stress test completed.',
    {
      testRunId:
        data.testRunId,
    },
  );
}