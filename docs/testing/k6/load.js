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
  listRequests,
  login,
  parseJson,
  randomPayload,
  sleepRandom,
} from './helpers.js';

import {
  loadThresholds,
} from './thresholds.js';

/**
 * ============================================================
 * Test Options
 * ============================================================
 */

export const options = {
  scenarios: {
    load_test: {
      executor: 'ramping-vus',

      startVUs: 0,

      stages: [
        {
          duration: '1m',
          target: 10,
        },

        {
          duration: '3m',
          target: 10,
        },

        {
          duration: '1m',
          target: 25,
        },

        {
          duration: '5m',
          target: 25,
        },

        {
          duration: '1m',
          target: 0,
        },
      ],

      gracefulRampDown: '30s',

      exec: 'loadScenario',
    },
  },

  thresholds: loadThresholds,

  tags: {
    test_type: 'load',
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
    'Starting load test setup.',
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
 * Load Test Scenario
 * ============================================================
 */

export function loadScenario(data) {
  const token = data.token;

  /**
   * ----------------------------------------------------------
   * Create Request
   * ----------------------------------------------------------
   */

  const payload =
    randomPayload();

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
      'load: request creation accepted':
        (response) =>
          response.status === STATUS.CREATED ||
          response.status === STATUS.ACCEPTED,

      'load: request id exists':
        () =>
          Boolean(requestId),
    });

  if (!requestCreated || !requestId) {
    debug(
      'Load iteration stopped because request creation failed.',
      {
        status:
          createResponse.status,

        response:
          createResponse.body,
      },
    );

    sleepRandom(0.5, 1.5);

    return;
  }

  sleepRandom(0.2, 0.8);

  /**
   * ----------------------------------------------------------
   * Get Request
   * ----------------------------------------------------------
   */

  const getResponse =
    getRequest(
      token,
      requestId,
    );

  check(getResponse, {
    'load: request retrieval succeeded':
      (response) =>
        response.status === STATUS.OK,
  });

  /**
   * ----------------------------------------------------------
   * Optional List Requests
   * ----------------------------------------------------------
   */

  if (__ITER % 5 === 0) {
    sleepRandom(0.1, 0.5);

    const listResponse =
      listRequests(
        token,
        {
          page: 0,
          size: 20,
        },
      );

    check(listResponse, {
      'load: request listing succeeded':
        (response) =>
          response.status === STATUS.OK,
    });
  }

  sleepRandom(0.5, 2);
}

/**
 * ============================================================
 * Test Teardown
 * ============================================================
 */

export function teardown(data) {
  debug(
    'Load test completed.',
    {
      testRunId:
        data.testRunId,
    },
  );
}