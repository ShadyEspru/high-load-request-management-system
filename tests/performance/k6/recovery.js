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
  pollRequestUntilCompleted,
  randomPayload,
  sleepRandom,
} from './helpers.js';

import {
  recoveryThresholds,
} from './thresholds.js';

/**
 * ============================================================
 * Recovery Test
 * ============================================================
 *
 * يتحقق هذا الاختبار من قدرة النظام على العودة إلى حالته
 * الطبيعية بعد تعرضه إلى حمل مرتفع وذروة كبيرة.
 *
 * يتكون السيناريو من المراحل التالية:
 *
 * 1. Normal Load
 * 2. High Load
 * 3. Peak Load
 * 4. Load Reduction
 * 5. Recovery Observation
 *
 * تتم مراقبة:
 *
 * - Response Time
 * - Error Rate
 * - Request Processing Time
 * - Request Polling Attempts
 * - قدرة Workers على تصريف الطلبات المتراكمة
 * - عودة النظام إلى حالة مستقرة
 */

/**
 * ============================================================
 * Environment Configuration
 * ============================================================
 */

const RECOVERY_NORMAL_VUS =
  Number(
    __ENV.RECOVERY_NORMAL_VUS || 20,
  );

const RECOVERY_HIGH_VUS =
  Number(
    __ENV.RECOVERY_HIGH_VUS || 100,
  );

const RECOVERY_PEAK_VUS =
  Number(
    __ENV.RECOVERY_PEAK_VUS || 250,
  );

const RECOVERY_NORMAL_DURATION =
  __ENV.RECOVERY_NORMAL_DURATION || '1m';

const RECOVERY_HIGH_RAMP_DURATION =
  __ENV.RECOVERY_HIGH_RAMP_DURATION || '1m';

const RECOVERY_HIGH_DURATION =
  __ENV.RECOVERY_HIGH_DURATION || '2m';

const RECOVERY_PEAK_RAMP_DURATION =
  __ENV.RECOVERY_PEAK_RAMP_DURATION || '30s';

const RECOVERY_PEAK_DURATION =
  __ENV.RECOVERY_PEAK_DURATION || '1m';

const RECOVERY_REDUCTION_DURATION =
  __ENV.RECOVERY_REDUCTION_DURATION || '1m';

const RECOVERY_OBSERVATION_DURATION =
  __ENV.RECOVERY_OBSERVATION_DURATION || '3m';

const RECOVERY_FINAL_RAMP_DOWN_DURATION =
  __ENV.RECOVERY_FINAL_RAMP_DOWN_DURATION || '30s';

const RECOVERY_GRACEFUL_RAMP_DOWN =
  __ENV.RECOVERY_GRACEFUL_RAMP_DOWN || '30s';

const RECOVERY_POLLING_INTERVAL_SECONDS =
  Number(
    __ENV.RECOVERY_POLLING_INTERVAL_SECONDS || 1,
  );

const RECOVERY_POLLING_MAX_ATTEMPTS =
  Number(
    __ENV.RECOVERY_POLLING_MAX_ATTEMPTS || 30,
  );

const RECOVERY_THINK_TIME_MIN_SECONDS =
  Number(
    __ENV.RECOVERY_THINK_TIME_MIN_SECONDS || 0.2,
  );

const RECOVERY_THINK_TIME_MAX_SECONDS =
  Number(
    __ENV.RECOVERY_THINK_TIME_MAX_SECONDS || 1,
  );

/**
 * ============================================================
 * Configuration Validation
 * ============================================================
 */

function validateConfiguration() {
  if (
    !Number.isFinite(RECOVERY_NORMAL_VUS) ||
    RECOVERY_NORMAL_VUS < 1
  ) {
    throw new Error(
      'RECOVERY_NORMAL_VUS must be greater than 0.',
    );
  }

  if (
    !Number.isFinite(RECOVERY_HIGH_VUS) ||
    RECOVERY_HIGH_VUS <
      RECOVERY_NORMAL_VUS
  ) {
    throw new Error(
      'RECOVERY_HIGH_VUS must be greater than or equal to ' +
        'RECOVERY_NORMAL_VUS.',
    );
  }

  if (
    !Number.isFinite(RECOVERY_PEAK_VUS) ||
    RECOVERY_PEAK_VUS <
      RECOVERY_HIGH_VUS
  ) {
    throw new Error(
      'RECOVERY_PEAK_VUS must be greater than or equal to ' +
        'RECOVERY_HIGH_VUS.',
    );
  }

  if (
    !Number.isFinite(
      RECOVERY_POLLING_INTERVAL_SECONDS,
    ) ||
    RECOVERY_POLLING_INTERVAL_SECONDS <= 0
  ) {
    throw new Error(
      'RECOVERY_POLLING_INTERVAL_SECONDS must be greater than 0.',
    );
  }

  if (
    !Number.isFinite(
      RECOVERY_POLLING_MAX_ATTEMPTS,
    ) ||
    RECOVERY_POLLING_MAX_ATTEMPTS < 1
  ) {
    throw new Error(
      'RECOVERY_POLLING_MAX_ATTEMPTS must be greater than 0.',
    );
  }

  if (
    RECOVERY_THINK_TIME_MIN_SECONDS < 0 ||
    RECOVERY_THINK_TIME_MAX_SECONDS <
      RECOVERY_THINK_TIME_MIN_SECONDS
  ) {
    throw new Error(
      'Recovery think time requires ' +
        '0 <= minimum <= maximum.',
    );
  }
}

validateConfiguration();

/**
 * ============================================================
 * Test Options
 * ============================================================
 */

export const options = {
  scenarios: {
    recovery_test: {
      executor: 'ramping-vus',

      startVUs: RECOVERY_NORMAL_VUS,

      stages: [
        /**
         * Normal Load
         *
         * تمثل الأداء الطبيعي قبل رفع الحمل.
         */
        {
          duration:
            RECOVERY_NORMAL_DURATION,

          target:
            RECOVERY_NORMAL_VUS,
        },

        /**
         * الانتقال من الحمل الطبيعي إلى الحمل المرتفع.
         */
        {
          duration:
            RECOVERY_HIGH_RAMP_DURATION,

          target:
            RECOVERY_HIGH_VUS,
        },

        /**
         * High Load
         *
         * يبقى النظام تحت حمل مرتفع لفترة كافية
         * لبدء تراكم الطلبات عند وجود Bottleneck.
         */
        {
          duration:
            RECOVERY_HIGH_DURATION,

          target:
            RECOVERY_HIGH_VUS,
        },

        /**
         * الانتقال السريع إلى Peak Load.
         */
        {
          duration:
            RECOVERY_PEAK_RAMP_DURATION,

          target:
            RECOVERY_PEAK_VUS,
        },

        /**
         * Peak Load
         *
         * تمثل هذه المرحلة أعلى ضغط في الاختبار.
         */
        {
          duration:
            RECOVERY_PEAK_DURATION,

          target:
            RECOVERY_PEAK_VUS,
        },

        /**
         * Load Reduction
         *
         * تخفيض الحمل إلى المستوى الطبيعي لبدء
         * مراقبة قدرة النظام على التعافي.
         */
        {
          duration:
            RECOVERY_REDUCTION_DURATION,

          target:
            RECOVERY_NORMAL_VUS,
        },

        /**
         * Recovery Observation
         *
         * يستمر حمل طبيعي منخفض نسبيًا أثناء مراقبة
         * تصريف Queue وتحسن Response Time وError Rate.
         */
        {
          duration:
            RECOVERY_OBSERVATION_DURATION,

          target:
            RECOVERY_NORMAL_VUS,
        },

        /**
         * إنهاء الاختبار تدريجيًا.
         */
        {
          duration:
            RECOVERY_FINAL_RAMP_DOWN_DURATION,

          target: 0,
        },
      ],

      gracefulRampDown:
        RECOVERY_GRACEFUL_RAMP_DOWN,

      exec: 'recoveryScenario',

      tags: {
        scenario: 'recovery',
      },
    },
  },

  thresholds:
    recoveryThresholds,

  tags: {
    test_type: 'recovery',
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
    'Starting recovery test setup.',
    {
      testRunId:
        TEST_RUN_ID,

      normalVUs:
        RECOVERY_NORMAL_VUS,

      highVUs:
        RECOVERY_HIGH_VUS,

      peakVUs:
        RECOVERY_PEAK_VUS,

      recoveryObservationDuration:
        RECOVERY_OBSERVATION_DURATION,
    },
  );

  const token =
    login();

  return {
    token,
    testRunId:
      TEST_RUN_ID,

    startedAt:
      new Date().toISOString(),
  };
}

/**
 * ============================================================
 * Recovery Test Scenario
 * ============================================================
 */

export function recoveryScenario(data) {
  const token = data.token;

  /**
   * ----------------------------------------------------------
   * Create Request
   * ----------------------------------------------------------
   */

  const payload = randomPayload({
    payload: {
      scenario: 'recovery',
      testRunId: data.testRunId,
      virtualUser: __VU,
      iteration: __ITER,
      timestamp: Date.now(),
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
      'recovery: request accepted':
        (response) =>
          response.status === STATUS.CREATED ||
          response.status === STATUS.ACCEPTED,

      'recovery: request id exists':
        () => Boolean(requestId),
    });

  /**
   * ----------------------------------------------------------
   * عند فشل إنشاء الطلب
   * ----------------------------------------------------------
   */

  if (
    !requestCreated ||
    !requestId
  ) {
    debug(
      'Recovery request creation failed.',
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
      RECOVERY_THINK_TIME_MIN_SECONDS,
      RECOVERY_THINK_TIME_MAX_SECONDS,
    );

    return;
  }

  /**
   * ----------------------------------------------------------
   * Read Request
   * ----------------------------------------------------------
   */

  const getResponse =
    getRequest(
      token,
      requestId,
    );

  const getBody =
    parseJson(getResponse);

  check(getResponse, {
    'recovery: request retrieved':
      (response) =>
        response.status === STATUS.OK,

    'recovery: retrieved request matches id':
      () =>
        extractRequestId(getBody) ===
        requestId,
  });

  /**
   * ----------------------------------------------------------
   * Poll Until Completed
   * ----------------------------------------------------------
   */

  const pollingResult =
    pollRequestUntilCompleted(
      token,
      requestId,
      {
        intervalSeconds:
          RECOVERY_POLLING_INTERVAL_SECONDS,

        maxAttempts:
          RECOVERY_POLLING_MAX_ATTEMPTS,

        completedStatuses: [
          'COMPLETED',
          'FAILED',
          'CANCELLED',
        ],
      },
    );

  check(pollingResult, {
    'recovery: request reached terminal state':
      (result) =>
        result.completed === true,

    'recovery: terminal status exists':
      (result) =>
        Boolean(result.status),
  });

  /**
   * ----------------------------------------------------------
   * Recovery Observation
   * ----------------------------------------------------------
   *
   * بعد انتهاء الذروة يفترض أن:
   *
   * - تقل محاولات Polling تدريجياً.
   * - تنخفض مدة معالجة الطلب.
   * - يقل معدل الأخطاء.
   * - تبدأ Queue بالتفريغ.
   */

  debug(
    'Recovery observation.',
    {
      requestId,

      completed:
        pollingResult.completed,

      finalStatus:
        pollingResult.status,

      attempts:
        pollingResult.attempts,

      virtualUser:
        __VU,

      iteration:
        __ITER,
    },
  );

  /**
   * ----------------------------------------------------------
   * Think Time
   * ----------------------------------------------------------
   */

  sleepRandom(
    RECOVERY_THINK_TIME_MIN_SECONDS,
    RECOVERY_THINK_TIME_MAX_SECONDS,
  );
}

/**
 * ============================================================
 * Test Teardown
 * ============================================================
 */

export function teardown(data) {
  const completedAt =
    new Date().toISOString();

  debug(
    'Recovery test completed.',
    {
      testRunId:
        data.testRunId,

      startedAt:
        data.startedAt,

      completedAt,

      configuredNormalVUs:
        RECOVERY_NORMAL_VUS,

      configuredHighVUs:
        RECOVERY_HIGH_VUS,

      configuredPeakVUs:
        RECOVERY_PEAK_VUS,

      configuredDurations: {
        normal:
          RECOVERY_NORMAL_DURATION,

        highRamp:
          RECOVERY_HIGH_RAMP_DURATION,

        high:
          RECOVERY_HIGH_DURATION,

        peakRamp:
          RECOVERY_PEAK_RAMP_DURATION,

        peak:
          RECOVERY_PEAK_DURATION,

        reduction:
          RECOVERY_REDUCTION_DURATION,

        observation:
          RECOVERY_OBSERVATION_DURATION,

        finalRampDown:
          RECOVERY_FINAL_RAMP_DOWN_DURATION,
      },
    },
  );
}