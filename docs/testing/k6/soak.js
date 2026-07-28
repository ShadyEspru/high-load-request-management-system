import { check } from 'k6';

import {
  STATUS,
  TEST_RUN_ID,
  debug,
} from './config.js';

import {
  createRequest,
  extractRequestId,
  login,
  parseJson,
  pollRequestUntilCompleted,
  randomPayload,
  sleepRandom,
} from './helpers.js';

import {
  soakThresholds,
} from './thresholds.js';

/**
 * ============================================================
 * Soak Test Configuration
 * ============================================================
 *
 * يختبر هذا السيناريو استقرار النظام تحت حمل ثابت
 * لفترة زمنية طويلة.
 *
 * الهدف هو اكتشاف مشكلات مثل:
 *
 * - Memory Leaks
 * - Connection Leaks
 * - Thread Pool Exhaustion
 * - Database Connection Exhaustion
 * - انخفاض الأداء التدريجي
 * - تراكم الرسائل داخل RabbitMQ
 */

const SOAK_VUS =
  Number(__ENV.SOAK_VUS || 50);

const SOAK_RAMP_UP_DURATION =
  __ENV.SOAK_RAMP_UP_DURATION || '2m';

const SOAK_DURATION =
  __ENV.SOAK_DURATION || '30m';

const SOAK_RAMP_DOWN_DURATION =
  __ENV.SOAK_RAMP_DOWN_DURATION || '2m';

const SOAK_GRACEFUL_RAMP_DOWN =
  __ENV.SOAK_GRACEFUL_RAMP_DOWN || '30s';

const SOAK_POLLING_ENABLED =
  String(
    __ENV.SOAK_POLLING_ENABLED || 'true',
  ).toLowerCase() === 'true';

const SOAK_POLLING_INTERVAL_SECONDS =
  Number(
    __ENV.SOAK_POLLING_INTERVAL_SECONDS || 1,
  );

const SOAK_POLLING_MAX_ATTEMPTS =
  Number(
    __ENV.SOAK_POLLING_MAX_ATTEMPTS || 30,
  );

/**
 * ============================================================
 * Test Options
 * ============================================================
 */

export const options = {
  scenarios: {
    soak_test: {
      executor: 'ramping-vus',

      startVUs: 0,

      stages: [
        {
          duration: SOAK_RAMP_UP_DURATION,
          target: SOAK_VUS,
        },

        {
          duration: SOAK_DURATION,
          target: SOAK_VUS,
        },

        {
          duration: SOAK_RAMP_DOWN_DURATION,
          target: 0,
        },
      ],

      gracefulRampDown:
        SOAK_GRACEFUL_RAMP_DOWN,

      exec: 'soakScenario',
    },
  },

  thresholds: soakThresholds,

  tags: {
    test_type: 'soak',
    test_run_id: TEST_RUN_ID,
  },

  summaryTrendStats: [
    'avg',
    'min',
    'med',
    'max',
    'p(90)',
    'p(95)',
    'p(99)',
  ],
};

/**
 * ============================================================
 * Test Setup
 * ============================================================
 */

export function setup() {
  debug(
    'Starting soak test setup.',
    {
      testRunId: TEST_RUN_ID,
      virtualUsers: SOAK_VUS,
      duration: SOAK_DURATION,
      pollingEnabled:
        SOAK_POLLING_ENABLED,
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
 * Soak Test Scenario
 * ============================================================
 */

export function soakScenario(data) {
  const token = data.token;

  /**
   * ----------------------------------------------------------
   * Create Request
   * ----------------------------------------------------------
   */

  const payload =
    randomPayload({
      payload: {
        scenario: 'soak',
        testRunId:
          data.testRunId,
        virtualUser:
          __VU,
        iteration:
          __ITER,
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
      'soak: request creation accepted':
        (response) =>
          response.status ===
            STATUS.CREATED ||
          response.status ===
            STATUS.ACCEPTED,

      'soak: request id exists':
        () =>
          Boolean(requestId),
    });

  if (
    !requestCreated ||
    !requestId
  ) {
    debug(
      'Soak iteration stopped because request creation failed.',
      {
        status:
          createResponse.status,

        response:
          createResponse.body,

        virtualUser:
          __VU,

        iteration:
          __ITER,
      },
    );

    sleepRandom(
      0.5,
      1.5,
    );

    return;
  }

  /**
   * ----------------------------------------------------------
   * Poll Request Until Completion
   * ----------------------------------------------------------
   */

  if (SOAK_POLLING_ENABLED) {
    const pollingResult =
      pollRequestUntilCompleted(
        token,
        requestId,
        {
          intervalSeconds:
            SOAK_POLLING_INTERVAL_SECONDS,

          maxAttempts:
            SOAK_POLLING_MAX_ATTEMPTS,

          completedStatuses: [
            'COMPLETED',
            'FAILED',
            'CANCELLED',
          ],
        },
      );

    check(pollingResult, {
      'soak: request reached final state':
        (result) =>
          result.completed === true,

      'soak: final status exists':
        (result) =>
          Boolean(result.status),
    });

    debug(
      'Soak request polling completed.',
      {
        requestId,
        completed:
          pollingResult.completed,
        status:
          pollingResult.status,
        attempts:
          pollingResult.attempts,
      },
    );
  }

  /**
   * ----------------------------------------------------------
   * Think Time
   * ----------------------------------------------------------
   *
   * يمنع إرسال الطلبات بصورة غير واقعية دون توقف،
   * ويحاكي انتظار المستخدم الحقيقي بين العمليات.
   */

  sleepRandom(
    0.5,
    2,
  );
}

/**
 * ============================================================
 * Test Teardown
 * ============================================================
 */

export function teardown(data) {
  debug(
    'Soak test completed.',
    {
      testRunId:
        data.testRunId,
      configuredVUs:
        SOAK_VUS,
      configuredDuration:
        SOAK_DURATION,
    },
  );
}