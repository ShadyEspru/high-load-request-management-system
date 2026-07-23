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
  sleepRandom,
} from './helpers.js';

import {
  smokeThresholds,
} from './thresholds.js';

/**
 * ============================================================
 * Test Options
 * ============================================================
 */

export const options = {
  vus: 1,

  iterations: 1,

  thresholds: smokeThresholds,

  tags: {
    test_type: 'smoke',
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
    'Starting smoke test setup.',
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
 * Smoke Test Scenario
 * ============================================================
 */

export default function (data) {
  const token = data.token;

  /**
   * ----------------------------------------------------------
   * Create Request
   * ----------------------------------------------------------
   */

  const createResponse =
    createRequest(token);

  const createBody =
    parseJson(createResponse);

  const requestId =
    extractRequestId(createBody);

  const requestCreated =
    check(createResponse, {
      'smoke: request was created':
        (response) =>
          response.status === STATUS.CREATED ||
          response.status === STATUS.ACCEPTED,

      'smoke: request id exists':
        () =>
          Boolean(requestId),
    });

  if (!requestCreated || !requestId) {
    debug(
      'Smoke test stopped because request creation failed.',
      {
        status:
          createResponse.status,

        response:
          createResponse.body,
      },
    );

    return;
  }

  sleepRandom(0.5, 1);

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
    'smoke: request can be retrieved':
      (response) =>
        response.status === STATUS.OK,
  });

  sleepRandom(0.5, 1);

  /**
   * ----------------------------------------------------------
   * List Requests
   * ----------------------------------------------------------
   */

  const listResponse =
    listRequests(
      token,
      {
        page: 0,
        size: 10,
      },
    );

  check(listResponse, {
    'smoke: requests can be listed':
      (response) =>
        response.status === STATUS.OK,
  });

  debug(
    'Smoke test iteration completed.',
    {
      requestId,
      testRunId:
        data.testRunId,
    },
  );
}

/**
 * ============================================================
 * Test Teardown
 * ============================================================
 */

export function teardown(data) {
  debug(
    'Smoke test completed.',
    {
      testRunId:
        data.testRunId,
    },
  );
}