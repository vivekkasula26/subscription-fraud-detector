CREATE TABLE fraud_cases (
    id UUID PRIMARY KEY,
    account_id VARCHAR(128) NOT NULL,
    subscription_id VARCHAR(128) NOT NULL,
    risk_score INTEGER NOT NULL CHECK (risk_score BETWEEN 0 AND 100),
    band VARCHAR(16) NOT NULL CHECK (band IN ('LOW', 'MEDIUM', 'HIGH')),
    status VARCHAR(16) NOT NULL CHECK (status IN ('OPEN', 'DISMISSED', 'FROZEN', 'BANNED')),
    reasons JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_fraud_case_account_subscription UNIQUE (account_id, subscription_id)
);

CREATE TABLE case_actions (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES fraud_cases (id),
    action VARCHAR(16) NOT NULL CHECK (action IN ('FREEZE')),
    note VARCHAR(128) NOT NULL,
    actor_id VARCHAR(128) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_case_action_type UNIQUE (case_id, action)
);
