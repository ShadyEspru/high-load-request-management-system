import http from 'k6/http';
import { check, fail, sleep } from 'k6';

import {
    AUTH_TOKEN,
    LOGIN_URL,
    REQUESTS_URL,
    REQUEST_TIMEOUT,
    TEST_USERS,
    TAGS,
    STATUS,
    DEFAULT_HEADERS,
    authorizationHeaders,
    createRequestPayload,
    correlationId,
    debug,
} from './config.js';

/**
 * ============================================================
 * Spike Test
 * ============================================================
 *
 * يحاكي هذا السيناريو ارتفاعًا مفاجئًا في عدد المستخدمين
 * (Virtual Users) خلال فترة زمنية قصيرة، ثم مراقبة
 * استجابة النظام أثناء الذروة وبعد انخفاض الحمل.
 */

const BASE_VUS =
    Number(__ENV.SPIKE_BASE_VUS || 20);

const PEAK_VUS =
    Number(__ENV.SPIKE_PEAK_VUS || 500);

const WARM_UP_DURATION =
    __ENV.SPIKE_WARM_UP_DURATION || '30s';

const RAMP_UP_DURATION =
    __ENV.SPIKE_RAMP_UP_DURATION || '10s';

const SPIKE_DURATION =
    __ENV.SPIKE_DURATION || '1m';

const RAMP_DOWN_DURATION =
    __ENV.SPIKE_RAMP_DOWN_DURATION || '10s';

const RECOVERY_DURATION =
    __ENV.SPIKE_RECOVERY_DURATION || '1m';

export const options = {

    scenarios: {

        suddenTrafficSpike: {

            executor: 'ramping-vus',

            startVUs: BASE_VUS,

            gracefulRampDown: '30s',

            stages: [

                {
                    duration: WARM_UP_DURATION,
                    target: BASE_VUS,
                },

                {
                    duration: RAMP_UP_DURATION,
                    target: PEAK_VUS,
                },

                {
                    duration: SPIKE_DURATION,
                    target: PEAK_VUS,
                },

                {
                    duration: RAMP_DOWN_DURATION,
                    target: BASE_VUS,
                },

                {
                    duration: RECOVERY_DURATION,
                    target: BASE_VUS,
                },

                {
                    duration: '10s',
                    target: 0,
                },

            ],

            exec: 'spikeScenario',

            tags: {
                scenario: 'spike',
                test_type: 'performance',
            },

        },

    },

    thresholds: {

        http_req_failed: [
            {
                threshold: 'rate<0.10',
                abortOnFail: false,
            },
        ],

        http_req_duration: [
            'p(95)<3000',
            'p(99)<5000',
        ],

        checks: [
            'rate>0.90',
        ],

        'http_req_failed{endpoint:create-request}': [
            'rate<0.10',
        ],

        'http_req_duration{endpoint:create-request}': [
            'p(95)<3000',
            'p(99)<5000',
        ],

    },

    batchPerHost:
        Number(__ENV.SPIKE_BATCH_PER_HOST || 20),

    discardResponseBodies: false,

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

export function setup() {

    if (AUTH_TOKEN) {

        debug(
            'Using AUTH_TOKEN from environment.'
        );

        return {
            token: AUTH_TOKEN,
        };

    }

    const user = TEST_USERS[0];

    if (
        !user ||
        !user.username ||
        !user.password
    ) {

        fail(
            'TEST_USERNAME / TEST_PASSWORD are required.'
        );

    }

    const response = http.post(

        LOGIN_URL,

        JSON.stringify({

            username: user.username,
            password: user.password,

        }),

        {

            headers: DEFAULT_HEADERS,
            timeout: REQUEST_TIMEOUT,
            tags: TAGS.LOGIN,

        }

    );

        const loginSucceeded = check(response, {

        'login status is 200': (res) =>
            res.status === STATUS.OK,

        'login response contains token': (res) => {

            const body = parseJson(res);

            return Boolean(
                body?.accessToken ||
                body?.token ||
                body?.data?.accessToken
            );

        },

    });

    if (!loginSucceeded) {

        fail(
            `Authentication failed. Status: ${response.status}`
        );

    }

    const body = parseJson(response);

    const token =
        body?.accessToken ||
        body?.token ||
        body?.data?.accessToken;

    if (!token) {

        fail(
            'Access Token not found in Login response.'
        );

    }

    return {

        token,

    };

}

/**
 * ============================================================
 * Spike Scenario
 * ============================================================
 */

export function spikeScenario(data) {

    if (!data?.token) {

        fail(
            'Access Token is missing.'
        );

    }

    const headers = {

        ...authorizationHeaders(
            data.token
        ),

        'X-Correlation-ID':
            correlationId(),

    };

    const payload =
        createRequestPayload();

    const response = http.post(

        REQUESTS_URL,

        JSON.stringify(payload),

        {

            headers,

            timeout:
                REQUEST_TIMEOUT,

            tags:
                TAGS.CREATE_REQUEST,

        }

    );

    check(response, {

        'request accepted': (res) =>

            res.status === STATUS.CREATED ||

            res.status === STATUS.ACCEPTED,

        'server error does not exist': (res) =>

            res.status <
            STATUS.INTERNAL_SERVER_ERROR,

        'content type is json': (res) => {

            if (!res.body) {

                return true;

            }

            return res.headers[
                'Content-Type'
            ]
                ?.toLowerCase()
                .includes(
                    'application/json'
                );

        },

    });

    debug(
        `Spike request finished. Status=${response.status}`
    );

    sleep(

        Number(
            __ENV.SPIKE_SLEEP_SECONDS ||
            0.1
        )

    );

}

/**
 * ============================================================
 * Helpers
 * ============================================================
 */

function parseJson(response) {

    if (!response) {
        return null;
    }

    if (!response.body) {
        return null;
    }

    try {

        return response.json();

    } catch (error) {

        debug(
            'Unable to parse JSON response.',
            error
        );

        return null;

    }

}

/**
 * ============================================================
 * Teardown
 * ============================================================
 */

export function teardown(data) {

    if (data?.token) {

        debug(
            'Spike Test finished successfully.'
        );

    }

}

/**
 * ============================================================
 * End of File
 * ============================================================
 */