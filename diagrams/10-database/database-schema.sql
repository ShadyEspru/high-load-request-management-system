-- HLRMS PostgreSQL schema draft
-- Generated for documentation and design review.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE client_system (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    api_key_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE retry_policy (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    max_attempts INTEGER NOT NULL CHECK (max_attempts >= 1),
    initial_delay_ms BIGINT NOT NULL CHECK (initial_delay_ms >= 0),
    multiplier NUMERIC(6,3) NOT NULL DEFAULT 2.0,
    max_delay_ms BIGINT NOT NULL CHECK (max_delay_ms >= initial_delay_ms),
    backoff_strategy VARCHAR(30) NOT NULL,
    retryable_errors JSONB NOT NULL DEFAULT '[]'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_system_id UUID NOT NULL REFERENCES client_system(id),
    retry_policy_id UUID REFERENCES retry_policy(id),
    request_type VARCHAR(80) NOT NULL,
    priority SMALLINT NOT NULL DEFAULT 5,
    status VARCHAR(40) NOT NULL,
    payload JSONB NOT NULL,
    idempotency_key VARCHAR(120),
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    queued_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_request_client_idempotency
        UNIQUE (client_system_id, idempotency_key)
);

CREATE INDEX idx_request_status_created_at ON request(status, created_at);
CREATE INDEX idx_request_client_created_at ON request(client_system_id, created_at);
CREATE INDEX idx_request_type_priority ON request(request_type, priority);

CREATE TABLE request_status_history (
    id BIGSERIAL PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES request(id) ON DELETE CASCADE,
    from_status VARCHAR(40),
    to_status VARCHAR(40) NOT NULL,
    reason_code VARCHAR(80),
    reason_message TEXT,
    changed_by VARCHAR(100),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_status_history_request_time
    ON request_status_history(request_id, changed_at);

CREATE TABLE processing_attempt (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES request(id) ON DELETE CASCADE,
    attempt_number INTEGER NOT NULL CHECK (attempt_number >= 1),
    status VARCHAR(30) NOT NULL,
    worker_id VARCHAR(120),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    failure_type VARCHAR(80),
    failure_message TEXT,
    retryable BOOLEAN,
    next_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_attempt_number UNIQUE (request_id, attempt_number)
);

CREATE INDEX idx_attempt_request ON processing_attempt(request_id);
CREATE INDEX idx_attempt_retry ON processing_attempt(status, next_retry_at);

CREATE TABLE processing_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processing_attempt_id UUID NOT NULL UNIQUE
        REFERENCES processing_attempt(id) ON DELETE CASCADE,
    outcome VARCHAR(30) NOT NULL,
    result_payload JSONB,
    error_code VARCHAR(80),
    processing_time_ms BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE configuration_change (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(150) NOT NULL,
    old_value JSONB,
    new_value JSONB NOT NULL,
    changed_by VARCHAR(120) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE outbox_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID REFERENCES request(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_pending
    ON outbox_event(status, created_at);
