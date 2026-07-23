import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

import {
  AUTH_TOKEN,
  DEFAULT_HEADERS,
  LOGIN_URL,
  REQUESTS_URL,
  REQUEST_TIMEOUT,
  STATUS,
  TAGS,
  TEST_USERS,
  authorizationHeaders,
  correlationId,
  createRequestPayload,
  debug,
} from './config.js';

/**
 * ============================================================
 * Custom Metrics
 * ============================================================
 */

export const authenticationFailures =
  new Counter('authentication_failures');

export const requestCreationFailures =
  new Counter('request_creation_failures');

export const unexpectedStatusCodes =
  new Counter('unexpected_status_codes');

export const businessErrorRate =
  new Rate('business_error_rate');

export const requestProcessingTime =
  new Trend(
    'request_processing_time',
    true,
  );

export const requestPollingAttempts =
  new Trend(
    'request_polling_attempts',
  );

/**
 * ============================================================
 * Authentication
 * ============================================================
 */

export function login(user = getTestUser()) {
  if (AUTH_TOKEN) {
    debug('Using AUTH_TOKEN from environment variables.');

    return AUTH_TOKEN;
  }

  if (
    !user ||
    !user.username ||
    !user.password
  ) {
    fail(
      'Authentication credentials are missing. ' +
        'Set AUTH_TOKEN or TEST_USERNAME and TEST_PASSWORD.',
    );
  }

  const payload = JSON.stringify({
    username: user.username,
    password: user.password,
  });

  const response = http.post(
    LOGIN_URL,
    payload,
    {
      headers: DEFAULT_HEADERS,
      tags: TAGS.LOGIN,
      timeout: REQUEST_TIMEOUT,
    },
  );

  const responseBody = parseJson(response);
  const token = extractAccessToken(responseBody);

  const successful = check(response, {
    'login status is 200': (res) =>
      res.status === STATUS.OK,

    'login response is JSON': (res) =>
      isJsonResponse(res),

    'login response contains access token': () =>
      Boolean(token),
  });

  businessErrorRate.add(!successful);

  if (!successful) {
    authenticationFailures.add(1);

    fail(
      `Authentication failed. ` +
        `Status: ${response.status}. ` +
        `Response: ${responseSnippet(response)}`,
    );
  }

  debug(
    'Authentication completed successfully.',
  );

  return token;
}

/**
 * ============================================================
 * Request Creation
 * ============================================================
 */

export function createRequest(
  token,
  payload = createRequestPayload(),
) {
  requireValue('token', token);

  const response = http.post(
    REQUESTS_URL,
    JSON.stringify(payload),
    {
      headers: {
        ...authorizationHeaders(token),

        'X-Correlation-ID':
          correlationId(),
      },

      tags: TAGS.CREATE_REQUEST,

      timeout: REQUEST_TIMEOUT,
    },
  );

  const responseBody = parseJson(response);
  const requestId =
    extractRequestId(responseBody);

  const successful = check(response, {
    'create request status is 201 or 202':
      (res) =>
        res.status === STATUS.CREATED ||
        res.status === STATUS.ACCEPTED,

    'create request response is JSON':
      (res) =>
        isJsonResponse(res),

    'create request response contains id':
      () =>
        Boolean(requestId),
  });

  businessErrorRate.add(!successful);

  if (!successful) {
    requestCreationFailures.add(1);

    trackUnexpectedStatus(
      response,
      [
        STATUS.CREATED,
        STATUS.ACCEPTED,
      ],
    );
  }

  return response;
}

/**
 * ============================================================
 * Request Retrieval
 * ============================================================
 */

export function getRequest(
  token,
  requestId,
) {
  requireValue('token', token);
  requireValue('requestId', requestId);

  const response = http.get(
    `${REQUESTS_URL}/${encodeURIComponent(requestId)}`,
    {
      headers: {
        ...authorizationHeaders(token),

        'X-Correlation-ID':
          correlationId(),
      },

      tags: TAGS.GET_REQUEST,

      timeout: REQUEST_TIMEOUT,
    },
  );

  const successful = check(response, {
    'get request status is 200':
      (res) =>
        res.status === STATUS.OK,

    'get request response is JSON':
      (res) =>
        isJsonResponse(res),
  });

  businessErrorRate.add(!successful);

  if (!successful) {
    trackUnexpectedStatus(
      response,
      [STATUS.OK],
    );
  }

  return response;
}

/**
 * ============================================================
 * Request Listing
 * ============================================================
 */

export function listRequests(
  token,
  queryParameters = {},
) {
  requireValue('token', token);

  const queryString =
    buildQueryString(queryParameters);

  const url = queryString
    ? `${REQUESTS_URL}?${queryString}`
    : REQUESTS_URL;

  const response = http.get(
    url,
    {
      headers: {
        ...authorizationHeaders(token),

        'X-Correlation-ID':
          correlationId(),
      },

      tags: TAGS.LIST_REQUESTS,

      timeout: REQUEST_TIMEOUT,
    },
  );

  const successful = check(response, {
    'list requests status is 200':
      (res) =>
        res.status === STATUS.OK,

    'list requests response is JSON':
      (res) =>
        isJsonResponse(res),
  });

  businessErrorRate.add(!successful);

  if (!successful) {
    trackUnexpectedStatus(
      response,
      [STATUS.OK],
    );
  }

  return response;
}

/**
 * ============================================================
 * Request Polling
 * ============================================================
 */

export function pollRequestUntilCompleted(
  token,
  requestId,
  options = {},
) {
  const intervalSeconds =
    options.intervalSeconds || 1;

  const maxAttempts =
    options.maxAttempts || 30;

  const completedStatuses =
    options.completedStatuses || [
      'COMPLETED',
      'FAILED',
      'CANCELLED',
    ];

  const startedAt = Date.now();

  for (
    let attempt = 1;
    attempt <= maxAttempts;
    attempt += 1
  ) {
    const response =
      getRequest(token, requestId);

    if (response.status === STATUS.OK) {
      const body = parseJson(response);

      const currentStatus =
        extractRequestStatus(body);

      debug(
        'Polling request status.',
        {
          requestId,
          attempt,
          currentStatus,
        },
      );

      if (
        currentStatus &&
        completedStatuses.includes(
          currentStatus,
        )
      ) {
        requestPollingAttempts.add(
          attempt,
        );

        requestProcessingTime.add(
          Date.now() - startedAt,
        );

        return {
          completed: true,
          attempts: attempt,
          status: currentStatus,
          response,
          body,
        };
      }
    }

    if (attempt < maxAttempts) {
      sleep(intervalSeconds);
    }
  }

  requestPollingAttempts.add(
    maxAttempts,
  );

  requestProcessingTime.add(
    Date.now() - startedAt,
  );

  businessErrorRate.add(true);

  return {
    completed: false,
    attempts: maxAttempts,
    status: null,
    response: null,
    body: null,
  };
}

/**
 * ============================================================
 * Response Parsing
 * ============================================================
 */

export function parseJson(response) {
  if (
    !response ||
    !response.body
  ) {
    return null;
  }

  try {
    return response.json();
  } catch (error) {
    debug(
      'Failed to parse JSON response.',
      error.message,
    );

    return null;
  }
}

export function extractAccessToken(body) {
  if (!body) {
    return null;
  }

  return (
    body.accessToken ||
    body.access_token ||
    body.token ||
    body.data?.accessToken ||
    body.data?.access_token ||
    body.data?.token ||
    null
  );
}

export function extractRequestId(body) {
  if (!body) {
    return null;
  }

  return (
    body.id ||
    body.requestId ||
    body.request_id ||
    body.data?.id ||
    body.data?.requestId ||
    body.data?.request_id ||
    null
  );
}

export function extractRequestStatus(body) {
  if (!body) {
    return null;
  }

  const status =
    body.status ||
    body.requestStatus ||
    body.request_status ||
    body.data?.status ||
    body.data?.requestStatus ||
    body.data?.request_status ||
    null;

  if (!status) {
    return null;
  }

  return String(status).toUpperCase();
}

export function isJsonResponse(response) {
  if (
    !response ||
    !response.headers
  ) {
    return false;
  }

  const contentType =
    response.headers['Content-Type'] ||
    response.headers['content-type'] ||
    '';

  return contentType
    .toLowerCase()
    .includes('application/json');
}

/**
 * ============================================================
 * Test User Utilities
 * ============================================================
 */

export function getTestUser(
  index = __VU - 1,
) {
  if (!TEST_USERS.length) {
    return null;
  }

  return TEST_USERS[
    index % TEST_USERS.length
  ];
}

export function randomUser() {
  if (!TEST_USERS.length) {
    return null;
  }

  const index =
    Math.floor(
      Math.random() *
      TEST_USERS.length,
    );

  return TEST_USERS[index];
}

/**
 * ============================================================
 * Test Payload Utilities
 * ============================================================
 */

export function randomPayload(
  overrides = {},
) {
  const priorities = [
    'LOW',
    'NORMAL',
    'HIGH',
  ];

  const randomPriority =
    priorities[
      Math.floor(
        Math.random() *
        priorities.length,
      )
    ];

  const basePayload =
    createRequestPayload();

  return {
    ...basePayload,
    ...overrides,

    priority:
      overrides.priority ||
      randomPriority,

    payload: {
      ...basePayload.payload,
      generatedAt:
        new Date().toISOString(),

      randomValue:
        Math.random(),

      ...(overrides.payload || {}),
    },
  };
}

/**
 * ============================================================
 * Validation Utilities
 * ============================================================
 */

export function checkStatus(
  response,
  expectedStatuses,
  checkName =
    'response status is expected',
) {
  const statuses =
    Array.isArray(expectedStatuses)
      ? expectedStatuses
      : [expectedStatuses];

  const successful = check(
    response,
    {
      [checkName]: (res) =>
        statuses.includes(
          res.status,
        ),
    },
  );

  if (!successful) {
    trackUnexpectedStatus(
      response,
      statuses,
    );
  }

  return successful;
}

export function failFast(
  condition,
  message,
) {
  if (!condition) {
    fail(message);
  }
}

/**
 * ============================================================
 * Execution Utilities
 * ============================================================
 */

export function sleepRandom(
  minSeconds = 0.5,
  maxSeconds = 2,
) {
  if (
    minSeconds < 0 ||
    maxSeconds < minSeconds
  ) {
    throw new Error(
      'sleepRandom requires ' +
      '0 <= minSeconds <= maxSeconds.',
    );
  }

  const duration =
    minSeconds +
    Math.random() *
    (
      maxSeconds -
      minSeconds
    );

  sleep(duration);
}

/**
 * ============================================================
 * Internal Utilities
 * ============================================================
 */

function buildQueryString(
  parameters,
) {
  return Object.entries(parameters)
    .filter(
      ([, value]) =>
        value !== undefined &&
        value !== null,
    )
    .map(
      ([key, value]) =>
        `${encodeURIComponent(key)}` +
        `=${encodeURIComponent(value)}`,
    )
    .join('&');
}

function trackUnexpectedStatus(
  response,
  expectedStatuses,
) {
  unexpectedStatusCodes.add(1);

  debug(
    'Unexpected HTTP status.',
    {
      actualStatus:
        response?.status,

      expectedStatuses,

      response:
        responseSnippet(response),
    },
  );
}

function responseSnippet(
  response,
  maxLength = 500,
) {
  const body =
    String(
      response?.body || '',
    );

  if (
    body.length <= maxLength
  ) {
    return body;
  }

  return (
    `${body.slice(0, maxLength)}` +
    '...'
  );
}

function requireValue(
  name,
  value,
) {
  if (
    value === undefined ||
    value === null ||
    value === ''
  ) {
    throw new Error(
      `${name} is required.`,
    );
  }
}