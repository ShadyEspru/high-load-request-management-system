CREATE TABLE transfer_transactions (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,

    sender_user_id UUID NOT NULL,
    recipient_user_id UUID NOT NULL,

    sender_transfer_id VARCHAR(16) NOT NULL,
    recipient_transfer_id VARCHAR(16) NOT NULL,

    sender_display_name VARCHAR(200) NOT NULL,
    recipient_display_name VARCHAR(200) NOT NULL,

    amount NUMERIC(20, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_transfer_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_transfer_not_self
        CHECK (sender_user_id <> recipient_user_id)
);

CREATE INDEX idx_transfer_transactions_sender
    ON transfer_transactions (
        sender_user_id,
        created_at DESC
    );

CREATE INDEX idx_transfer_transactions_recipient
    ON transfer_transactions (
        recipient_user_id,
        created_at DESC
    );

CREATE INDEX idx_transfer_transactions_created_at
    ON transfer_transactions (
        created_at DESC
    );
