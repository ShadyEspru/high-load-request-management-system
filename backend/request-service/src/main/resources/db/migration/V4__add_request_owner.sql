ALTER TABLE requests
    ADD COLUMN user_id UUID;

DO
$$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM requests
        WHERE user_id IS NULL
    ) THEN
        RAISE EXCEPTION
            'Cannot make requests.user_id NOT NULL because existing requests have no owner';
    END IF;
END
$$;

ALTER TABLE requests
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE requests
    DROP CONSTRAINT uk_requests_idempotency_key;

ALTER TABLE requests
    ADD CONSTRAINT uk_requests_user_id_idempotency_key
        UNIQUE (user_id, idempotency_key);

CREATE INDEX idx_requests_user_id
    ON requests (user_id);

CREATE INDEX idx_requests_user_id_created_at
    ON requests (user_id, created_at DESC);

CREATE INDEX idx_requests_user_id_status
    ON requests (user_id, status);