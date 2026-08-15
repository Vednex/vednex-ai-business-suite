CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_type VARCHAR(120) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(160) NOT NULL,
    event_version INTEGER NOT NULL,
    payload JSONB NOT NULL,
    company_id UUID,
    user_id UUID,
    correlation_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_outbox_events_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_outbox_events_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX ix_outbox_events_status_created ON outbox_events (status, created_at);
CREATE INDEX ix_outbox_events_status_next_retry ON outbox_events (status, next_retry_at);
CREATE INDEX ix_outbox_events_event_type ON outbox_events (event_type);
CREATE INDEX ix_outbox_events_correlation ON outbox_events (correlation_id);
