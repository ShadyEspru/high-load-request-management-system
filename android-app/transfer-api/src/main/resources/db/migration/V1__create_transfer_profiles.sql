CREATE TABLE transfer_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    transfer_id VARCHAR(16) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_transfer_profiles_transfer_id
    ON transfer_profiles (transfer_id);

CREATE INDEX idx_transfer_profiles_user_id
    ON transfer_profiles (user_id);