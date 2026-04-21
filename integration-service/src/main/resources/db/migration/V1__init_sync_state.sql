CREATE TABLE sync_state (
    key         VARCHAR(64) PRIMARY KEY,
    value       VARCHAR(512),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
