CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(160) NOT NULL,
    legal_name VARCHAR(200),
    slug VARCHAR(120) NOT NULL,
    email VARCHAR(320) NOT NULL,
    phone VARCHAR(40),
    country VARCHAR(80) NOT NULL,
    timezone VARCHAR(80) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(30) NOT NULL,
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_companies_slug UNIQUE (slug),
    CONSTRAINT ck_companies_status CHECK (status IN ('TRIAL', 'ACTIVE', 'SUSPENDED', 'CANCELLED'))
);

CREATE UNIQUE INDEX uk_companies_email_lower ON companies (LOWER(email));

CREATE TABLE company_settings (
    company_id UUID PRIMARY KEY REFERENCES companies(id) ON DELETE CASCADE,
    logo_url VARCHAR(1000),
    address_line_1 VARCHAR(200),
    address_line_2 VARCHAR(200),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(30),
    country VARCHAR(80),
    gst_number VARCHAR(80),
    tax_number VARCHAR(80),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    timezone VARCHAR(80) NOT NULL DEFAULT 'UTC',
    language VARCHAR(20) NOT NULL DEFAULT 'en',
    date_format VARCHAR(40) NOT NULL DEFAULT 'yyyy-MM-dd',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(40),
    profile_image_url VARCHAR(1000),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    last_login_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_users_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'DISABLED'))
);

CREATE UNIQUE INDEX uk_users_email_lower ON users (LOWER(email));
CREATE INDEX ix_users_status ON users (status);

CREATE TABLE company_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_title VARCHAR(120),
    department_name VARCHAR(120),
    membership_status VARCHAR(30) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_company_members_company_user UNIQUE (company_id, user_id),
    CONSTRAINT ck_company_members_status CHECK (membership_status IN ('INVITED', 'ACTIVE', 'SUSPENDED', 'REMOVED'))
);

CREATE INDEX ix_company_members_company_status ON company_members (company_id, membership_status);
CREATE INDEX ix_company_members_user_status ON company_members (user_id, membership_status);

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(80) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_permissions_code UNIQUE (code)
);

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(255),
    protected_system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX uk_roles_system_name ON roles (name) WHERE company_id IS NULL;
CREATE UNIQUE INDEX uk_roles_company_name ON roles (company_id, name) WHERE company_id IS NOT NULL;
CREATE INDEX ix_roles_company ON roles (company_id);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    membership_id UUID NOT NULL REFERENCES company_members(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_user_roles_membership_role UNIQUE (membership_id, role_id)
);

CREATE INDEX ix_user_roles_company_user ON user_roles (company_id, user_id);

CREATE TABLE invitations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    email VARCHAR(320) NOT NULL,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    token_hash VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    invited_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_invitations_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED'))
);

CREATE INDEX ix_invitations_company_status ON invitations (company_id, status);
CREATE INDEX ix_invitations_company_email ON invitations (company_id, LOWER(email));

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    last_sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_email_verification_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_email_verification_user ON email_verification_tokens (user_id, used_at);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_password_reset_user ON password_reset_tokens (user_id, used_at);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    membership_id UUID NOT NULL REFERENCES company_members(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL,
    family_id UUID NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_token_id UUID,
    ip_address VARCHAR(80),
    user_agent VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_tokens_user_company ON refresh_tokens (user_id, company_id);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID REFERENCES companies(id) ON DELETE SET NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80),
    entity_id UUID,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip_address VARCHAR(80),
    user_agent VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_audit_logs_company_created ON audit_logs (company_id, created_at DESC);
CREATE INDEX ix_audit_logs_user_created ON audit_logs (user_id, created_at DESC);
CREATE INDEX ix_audit_logs_action ON audit_logs (action);

CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    price_cents BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    billing_period VARCHAR(30) NOT NULL DEFAULT 'MONTHLY',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_plans_code UNIQUE (code)
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES plans(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    current_period_ends_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_subscriptions_status CHECK (status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'CANCELLED'))
);

CREATE INDEX ix_subscriptions_company_status ON subscriptions (company_id, status);

INSERT INTO permissions (code, description) VALUES
('COMPANY_VIEW', 'View company information'),
('COMPANY_UPDATE', 'Update company information'),
('USER_VIEW', 'View company users'),
('USER_INVITE', 'Invite company users'),
('USER_UPDATE', 'Update company users'),
('USER_DISABLE', 'Disable company users'),
('ROLE_VIEW', 'View roles'),
('ROLE_CREATE', 'Create roles'),
('ROLE_UPDATE', 'Update roles'),
('ROLE_ASSIGN', 'Assign roles'),
('CRM_VIEW', 'View CRM records'),
('CRM_CREATE', 'Create CRM records'),
('CRM_UPDATE', 'Update CRM records'),
('CRM_DELETE', 'Delete CRM records'),
('INVOICE_VIEW', 'View invoices'),
('INVOICE_CREATE', 'Create invoices'),
('INVOICE_UPDATE', 'Update invoices'),
('INVOICE_DELETE', 'Delete invoices'),
('EMPLOYEE_VIEW', 'View employees'),
('EMPLOYEE_CREATE', 'Create employees'),
('EMPLOYEE_UPDATE', 'Update employees'),
('PROJECT_VIEW', 'View projects'),
('PROJECT_CREATE', 'Create projects'),
('PROJECT_UPDATE', 'Update projects'),
('SUPPORT_VIEW', 'View support'),
('SUPPORT_REPLY', 'Reply to support'),
('AI_USE', 'Use AI features'),
('ANALYTICS_VIEW', 'View analytics'),
('AUTOMATION_MANAGE', 'Manage automation')
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles (name, description, protected_system_role) VALUES
('OWNER', 'Company owner with all permissions', TRUE),
('ADMIN', 'Company administrator', TRUE),
('HR', 'Human resources user', TRUE),
('SALES', 'Sales user', TRUE),
('PROJECT_MANAGER', 'Project manager', TRUE),
('SUPPORT', 'Support user', TRUE),
('EMPLOYEE', 'Employee user', TRUE),
('CUSTOMER', 'Customer user', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'OWNER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'COMPANY_VIEW', 'COMPANY_UPDATE', 'USER_VIEW', 'USER_INVITE', 'USER_UPDATE',
    'USER_DISABLE', 'ROLE_VIEW', 'ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_ASSIGN',
    'ANALYTICS_VIEW'
)
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('COMPANY_VIEW', 'USER_VIEW', 'USER_INVITE', 'EMPLOYEE_VIEW', 'EMPLOYEE_CREATE', 'EMPLOYEE_UPDATE')
WHERE r.name = 'HR'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('COMPANY_VIEW', 'CRM_VIEW', 'CRM_CREATE', 'CRM_UPDATE')
WHERE r.name = 'SALES'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('COMPANY_VIEW', 'PROJECT_VIEW', 'PROJECT_CREATE', 'PROJECT_UPDATE')
WHERE r.name = 'PROJECT_MANAGER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('COMPANY_VIEW', 'SUPPORT_VIEW', 'SUPPORT_REPLY')
WHERE r.name = 'SUPPORT'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('COMPANY_VIEW')
WHERE r.name IN ('EMPLOYEE', 'CUSTOMER')
ON CONFLICT DO NOTHING;

INSERT INTO plans (code, name, description, price_cents, currency, billing_period)
VALUES ('TRIAL', 'Trial', 'Default trial plan for new companies', 0, 'USD', 'MONTHLY')
ON CONFLICT (code) DO NOTHING;
