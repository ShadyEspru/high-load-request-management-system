CREATE TABLE outbox_events
(
    id UUID PRIMARY KEY,

    aggregate_id UUID NOT NULL,

    aggregate_type VARCHAR(100) NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    payload JSONB NOT NULL,

    status VARCHAR(30) NOT NULL,

    retry_count INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    published_at TIMESTAMP WITH TIME ZONE,

    last_error TEXT,

    CONSTRAINT chk_outbox_events_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'PUBLISHED',
                'FAILED'
            )
        ),

    CONSTRAINT chk_outbox_events_retry_count
        CHECK (retry_count >= 0)
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (status, created_at);

CREATE INDEX idx_outbox_events_aggregate_id
    ON outbox_events (aggregate_id);

CREATE INDEX idx_outbox_events_event_type
    ON outbox_events (event_type);
