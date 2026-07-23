import { SharedArray } from 'k6/data';

/**
 * ============================================================
 * Environment Variables
 * ============================================================
 */

export const BASE_URL =
  __ENV.BASE_URL || 'http://localhost:8080';

export const API_PREFIX =
  __ENV.API_PREFIX || '/api/v1';

export const LOGIN_ENDPOINT =
  __ENV.LOGIN_ENDPOINT || '/auth/login';

export const REQUEST_ENDPOINT =
  __ENV.REQUEST_ENDPOINT || '/requests';

export const REQUEST_TIMEOUT =
  __ENV.REQUEST_TIMEOUT || '30s';

export const DEBUG =
  (__ENV.DEBUG || 'false').toLowerCase() === 'true';

export const TEST_RUN_ID =
  __ENV.TEST_RUN_ID ||
  `k6-${Date.now()}`;

export const TEST_USERNAME =
  __ENV.TEST_USERNAME || '';

export const TEST_PASSWORD =
  __ENV.TEST_PASSWORD || '';

export const AUTH_TOKEN =
  __ENV.AUTH_TOKEN || '';

/**
 * ============================================================
 * Full API URLs
 * ============================================================
 */

export const LOGIN_URL =
  `${BASE_URL}${API_PREFIX}${LOGIN_ENDPOINT}`;

export const REQUESTS_URL =
  `${BASE_URL}${API_PREFIX}${REQUEST_ENDPOINT}`;

/**
 * ============================================================
 * Default Headers
 * ============================================================
 */

export const DEFAULT_HEADERS = {
  'Content-Type': 'application/json',
  Accept: 'application/json',
};

export function authorizationHeaders(token) {
  return {
    ...DEFAULT_HEADERS,
    Authorization: `Bearer ${token}`,
  };
}

/**
 * ============================================================
 * Request Tags
 * ============================================================
 */

export const TAGS = {
  LOGIN: {
    endpoint: 'login',
  },

  CREATE_REQUEST: {
    endpoint: 'create-request',
  },

  GET_REQUEST: {
    endpoint: 'get-request',
  },

  LIST_REQUESTS: {
    endpoint: 'list-requests',
  },
};

/**
 * ============================================================
 * HTTP Status Codes
 * ============================================================
 */

export const STATUS = {
  OK: 200,

  CREATED: 201,

  ACCEPTED: 202,

  BAD_REQUEST: 400,

  UNAUTHORIZED: 401,

  FORBIDDEN: 403,

  NOT_FOUND: 404,

  CONFLICT: 409,

  TOO_MANY_REQUESTS: 429,

  INTERNAL_SERVER_ERROR: 500,
};

/**
 * ============================================================
 * Default Request Payload
 * ============================================================
 */

export function createRequestPayload() {
  const uniqueId =
    `${__VU}-${__ITER}-${Date.now()}`;

  return {
    externalReference: `k6-${uniqueId}`,

    type: 'STANDARD',

    priority: 'NORMAL',

    payload: {
      source: 'k6',

      operation: 'performance-test',

      testRunId: TEST_RUN_ID,
    },
  };
}

/**
 * ============================================================
 * Test Users
 * ============================================================
 */

export const TEST_USERS =
  new SharedArray(
    'test-users',
    () => [
      {
        username: TEST_USERNAME,
        password: TEST_PASSWORD,
      },
    ],
  );

/**
 * ============================================================
 * Utility Functions
 * ============================================================
 */

export function correlationId() {
  return `k6-${TEST_RUN_ID}-${__VU}-${__ITER}`;
}

export function debug(...args) {
  if (DEBUG) {
    console.log(...args);
  }
}