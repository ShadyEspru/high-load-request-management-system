CREATE TABLE processed_events
(
    event_id UUID PRIMARY KEY,

    request_id UUID NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    event_version INTEGER NOT NULL,

    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,

    processed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_processed_events_request
        FOREIGN KEY (request_id)
        REFERENCES requests (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_processed_events_event_version
        CHECK (event_version > 0)
);

CREATE INDEX idx_processed_events_request_id
    ON processed_events (request_id);

CREATE INDEX idx_processed_events_processed_at
    ON processed_events (processed_at);