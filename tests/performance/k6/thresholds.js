/**
 * ============================================================
 * Common Thresholds
 * ============================================================
 */

export const commonThresholds = {
  http_req_failed: [
    'rate<0.01',
  ],

  http_req_duration: [
    'p(95)<500',
    'p(99)<1000',
  ],

  checks: [
    'rate>0.99',
  ],

  business_error_rate: [
    'rate<0.01',
  ],
};

/**
 * ============================================================
 * Authentication Thresholds
 * ============================================================
 */

export const authenticationThresholds = {
  'http_req_duration{endpoint:login}': [
    'p(95)<500',
    'p(99)<1000',
  ],

  'http_req_failed{endpoint:login}': [
    'rate<0.01',
  ],

  authentication_failures: [
    'count<1',
  ],
};

/**
 * ============================================================
 * Request Creation Thresholds
 * ============================================================
 */

export const requestCreationThresholds = {
  'http_req_duration{endpoint:create-request}': [
    'p(95)<500',
    'p(99)<1000',
  ],

  'http_req_failed{endpoint:create-request}': [
    'rate<0.01',
  ],

  request_creation_failures: [
    'count<1',
  ],
};

/**
 * ============================================================
 * Request Retrieval Thresholds
 * ============================================================
 */

export const requestRetrievalThresholds = {
  'http_req_duration{endpoint:get-request}': [
    'p(95)<400',
    'p(99)<800',
  ],

  'http_req_failed{endpoint:get-request}': [
    'rate<0.01',
  ],
};

/**
 * ============================================================
 * Request Listing Thresholds
 * ============================================================
 */

export const requestListingThresholds = {
  'http_req_duration{endpoint:list-requests}': [
    'p(95)<600',
    'p(99)<1200',
  ],

  'http_req_failed{endpoint:list-requests}': [
    'rate<0.01',
  ],
};

/**
 * ============================================================
 * Smoke Test Thresholds
 * ============================================================
 */

export const smokeThresholds = {
  http_req_failed: [
    'rate<0.01',
  ],

  http_req_duration: [
    'p(95)<1000',
    'p(99)<1500',
  ],

  checks: [
    'rate>0.99',
  ],

  business_error_rate: [
    'rate<0.01',
  ],

  authentication_failures: [
    'count<1',
  ],

  request_creation_failures: [
    'count<1',
  ],

  unexpected_status_codes: [
    'count<1',
  ],

  ...authenticationThresholds,
  ...requestCreationThresholds,
  ...requestRetrievalThresholds,
  ...requestListingThresholds,
};

/**
 * ============================================================
 * Load Test Thresholds
 * ============================================================
 */

export const loadThresholds = {
  http_req_failed: [
    'rate<0.01',
  ],

  http_req_duration: [
    'p(90)<350',
    'p(95)<500',
    'p(99)<1000',
  ],

  checks: [
    'rate>0.99',
  ],

  business_error_rate: [
    'rate<0.01',
  ],

  'http_req_duration{endpoint:login}': [
    'p(95)<500',
    'p(99)<1000',
  ],

  'http_req_duration{endpoint:create-request}': [
    'p(95)<500',
    'p(99)<1000',
  ],

  'http_req_duration{endpoint:get-request}': [
    'p(95)<400',
    'p(99)<800',
  ],

  'http_req_duration{endpoint:list-requests}': [
    'p(95)<600',
    'p(99)<1200',
  ],
};

/**
 * ============================================================
 * Stress Test Thresholds
 * ============================================================
 */

export const stressThresholds = {
  http_req_failed: [
    'rate<0.05',
  ],

  http_req_duration: [
    'p(90)<1000',
    'p(95)<1500',
    'p(99)<3000',
  ],

  checks: [
    'rate>0.95',
  ],

  business_error_rate: [
    'rate<0.05',
  ],

  'http_req_duration{endpoint:create-request}': [
    'p(95)<2000',
  ],

  'http_req_duration{endpoint:get-request}': [
    'p(95)<1500',
  ],
};

/**
 * ============================================================
 * Spike Test Thresholds
 * ============================================================
 */

export const spikeThresholds = {
  http_req_failed: [
    'rate<0.10',
  ],

  http_req_duration: [
    'p(90)<1500',
    'p(95)<2500',
    'p(99)<5000',
  ],

  checks: [
    'rate>0.90',
  ],

  business_error_rate: [
    'rate<0.10',
  ],

  'http_req_duration{endpoint:create-request}': [
    'p(95)<3000',
  ],

  'http_req_duration{endpoint:get-request}': [
    'p(95)<2500',
  ],
};

/**
 * ============================================================
 * Soak Test Thresholds
 * ============================================================
 */

export const soakThresholds = {
  http_req_failed: [
    'rate<0.01',
  ],

  http_req_duration: [
    'p(90)<400',
    'p(95)<600',
    'p(99)<1200',
  ],

  checks: [
    'rate>0.99',
  ],

  business_error_rate: [
    'rate<0.01',
  ],

  'http_req_duration{endpoint:create-request}': [
    'p(95)<600',
    'p(99)<1200',
  ],

  'http_req_duration{endpoint:get-request}': [
    'p(95)<500',
    'p(99)<1000',
  ],

  request_processing_time: [
    'p(95)<10000',
    'p(99)<20000',
  ],
};

/**
 * ============================================================
 * Recovery Test Thresholds
 * ============================================================
 */

export const recoveryThresholds = {
  http_req_failed: [
    'rate<0.05',
  ],

  http_req_duration: [
    'p(95)<1500',
    'p(99)<3000',
  ],

  checks: [
    'rate>0.95',
  ],

  business_error_rate: [
    'rate<0.05',
  ],

  request_processing_time: [
    'p(95)<15000',
    'p(99)<30000',
  ],

  request_polling_attempts: [
    'p(95)<30',
  ],
};

/**
 * ============================================================
 * Threshold Selection
 * ============================================================
 */

export function thresholdsFor(scenario) {
  const thresholds = {
    smoke: smokeThresholds,
    load: loadThresholds,
    stress: stressThresholds,
    spike: spikeThresholds,
    soak: soakThresholds,
    recovery: recoveryThresholds,
  };

  const selectedThresholds =
    thresholds[
      String(scenario || '')
        .trim()
        .toLowerCase()
    ];

  if (!selectedThresholds) {
    throw new Error(
      `Unknown k6 scenario: ${scenario}`,
    );
  }

  return selectedThresholds;
}