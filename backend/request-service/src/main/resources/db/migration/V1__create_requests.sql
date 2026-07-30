CREATE TABLE requests
(
    id UUID PRIMARY KEY,

    request_type VARCHAR(100) NOT NULL,

    payload TEXT NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    result TEXT,

    error_message TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    completed_at TIMESTAMP WITH TIME ZONE,

    version BIGINT NOT NULL DEFAULT 0,

    idempotency_key VARCHAR(100) NOT NULL,

    idempotency_fingerprint VARCHAR(64) NOT NULL,

    CONSTRAINT uk_requests_idempotency_key
        UNIQUE (idempotency_key),

    CONSTRAINT chk_requests_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'COMPLETED',
                'FAILED'
            )
        ),

    CONSTRAINT chk_requests_version
        CHECK (version >= 0)
);

CREATE INDEX idx_requests_status
    ON requests (status);

CREATE INDEX idx_requests_created_at
    ON requests (created_at);

CREATE INDEX idx_requests_request_type
    ON requests (request_type);