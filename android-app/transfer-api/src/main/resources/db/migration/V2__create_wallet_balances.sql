CREATE TABLE wallet_balances (
    id BIGSERIAL PRIMARY KEY,

    user_id UUID NOT NULL,

    currency VARCHAR(3) NOT NULL,

    balance NUMERIC(20, 2) NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_wallet_balances_user
        FOREIGN KEY (user_id)
        REFERENCES transfer_profiles (user_id)
        ON DELETE CASCADE,

    CONSTRAINT uq_wallet_balances_user_currency
        UNIQUE (user_id, currency),

    CONSTRAINT chk_wallet_balance_non_negative
        CHECK (balance >= 0),

    CONSTRAINT chk_wallet_currency
        CHECK (
            currency IN (
                'USD',
                'EUR',
                'TRY',
                'SYP'
            )
        )
);

CREATE INDEX idx_wallet_balances_user_id
    ON wallet_balances (user_id);

CREATE INDEX idx_wallet_balances_currency
    ON wallet_balances (currency);


/*
 * أرصدة Demo أولية للحسابات الموجودة مسبقًا.
 *
 * هذه تُنشأ مرة واحدة فقط.
 * بعد ذلك تصبح القيم محفوظة فعليًا في PostgreSQL
 * ولن يعاد ضبطها عند تسجيل الدخول.
 */
INSERT INTO wallet_balances (
    user_id,
    currency,
    balance,
    version,
    created_at,
    updated_at
)
SELECT
    profile.user_id,
    initial.currency,
    initial.balance,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM transfer_profiles profile
CROSS JOIN (
    VALUES
        ('USD', 14280.00::NUMERIC),
        ('EUR', 8260.50::NUMERIC),
        ('TRY', 186750.00::NUMERIC),
        ('SYP', 250000.00::NUMERIC)
) AS initial(currency, balance)
ON CONFLICT (user_id, currency)
DO NOTHING;