CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE processed_events (
    event_id UUID NOT NULL,
    consumer_name VARCHAR(120) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer_name)
);

CREATE TABLE notification_deliveries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(160) NOT NULL,
    notification_type VARCHAR(80) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    template_key VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    provider_message_id VARCHAR(255) NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000) NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    sent_at TIMESTAMP WITH TIME ZONE NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_notification_deliveries_status CHECK (status IN ('PROCESSING', 'SENT', 'FAILED', 'PERMANENT_FAILED')),
    CONSTRAINT ck_notification_deliveries_channel CHECK (channel IN ('EMAIL')),
    CONSTRAINT ck_notification_deliveries_attempt_count CHECK (attempt_count >= 0)
);

CREATE INDEX ix_notification_deliveries_status_created ON notification_deliveries (status, created_at);
CREATE INDEX ix_notification_deliveries_event_type ON notification_deliveries (event_type);
CREATE INDEX ix_notification_deliveries_recipient ON notification_deliveries (recipient);
