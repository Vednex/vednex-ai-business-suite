CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE crm_leads (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL,
    lead_number VARCHAR(50) NOT NULL,

    company_name VARCHAR(200),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(320),
    phone VARCHAR(40),
    job_title VARCHAR(120),
    industry VARCHAR(120),
    city VARCHAR(100),

    requirement TEXT,
    estimated_budget NUMERIC(15,2),
    currency VARCHAR(3),
    timeline VARCHAR(200),

    source VARCHAR(30) NOT NULL DEFAULT 'OTHER',
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    assigned_user_id UUID,

    qualification_score INTEGER NOT NULL DEFAULT 0,
    budget_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    authority_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    need_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    timeline_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    qualification_notes TEXT,

    created_by UUID,
    updated_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    converted_at TIMESTAMP WITH TIME ZONE,
    disqualified_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uk_crm_leads_company_lead_number UNIQUE (company_id, lead_number),
    CONSTRAINT ck_crm_leads_status CHECK (status IN ('NEW','CONTACTED','QUALIFYING','QUALIFIED','CONVERTED','DISQUALIFIED')),
    CONSTRAINT ck_crm_leads_qualification_score CHECK (qualification_score >= 0 AND qualification_score <= 100)
);

CREATE INDEX ix_crm_leads_company_id ON crm_leads (company_id);
CREATE INDEX ix_crm_leads_company_status ON crm_leads (company_id, status);
CREATE INDEX ix_crm_leads_company_assigned_user ON crm_leads (company_id, assigned_user_id);
CREATE INDEX ix_crm_leads_company_created_at ON crm_leads (company_id, created_at);

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
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX ix_outbox_events_status_retry ON outbox_events (status, next_retry_at);
CREATE INDEX ix_outbox_events_company ON outbox_events (company_id);
