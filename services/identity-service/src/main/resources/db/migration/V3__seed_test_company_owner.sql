WITH seeded_company AS (
    INSERT INTO companies (
        id,
        name,
        legal_name,
        slug,
        email,
        phone,
        country,
        timezone,
        currency,
        status,
        trial_ends_at,
        created_at,
        updated_at,
        version
    )
    VALUES (
        '11111111-1111-4111-8111-111111111111',
        'Vednex Test Company',
        'Vednex Test Company',
        'vednex-test-company',
        'test-company@vednex.local',
        NULL,
        'India',
        'Asia/Kolkata',
        'INR',
        'TRIAL',
        CURRENT_TIMESTAMP + INTERVAL '14 days',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    )
    ON CONFLICT (slug) DO UPDATE SET
        name = EXCLUDED.name,
        legal_name = EXCLUDED.legal_name,
        country = EXCLUDED.country,
        timezone = EXCLUDED.timezone,
        currency = EXCLUDED.currency,
        status = EXCLUDED.status,
        trial_ends_at = EXCLUDED.trial_ends_at,
        updated_at = CURRENT_TIMESTAMP
    RETURNING id
),
seeded_user AS (
    INSERT INTO users (
        id,
        first_name,
        last_name,
        email,
        password_hash,
        email_verified,
        account_locked,
        failed_login_attempts,
        status,
        created_at,
        updated_at,
        version
    )
    VALUES (
        '22222222-2222-4222-8222-222222222222',
        'Test',
        'Owner',
        'test@gmail.com',
        '$2a$10$x9BmF8zASpYn.yd/ODgb.uJ2sPmzZ.BFGfcQTBAJzp15F1kcE/XbW',
        TRUE,
        FALSE,
        0,
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    )
    ON CONFLICT (LOWER(email)) DO UPDATE SET
        first_name = EXCLUDED.first_name,
        last_name = EXCLUDED.last_name,
        password_hash = EXCLUDED.password_hash,
        email_verified = TRUE,
        account_locked = FALSE,
        failed_login_attempts = 0,
        status = 'ACTIVE',
        updated_at = CURRENT_TIMESTAMP
    RETURNING id
),
seeded_settings AS (
    INSERT INTO company_settings (
        company_id,
        country,
        currency,
        timezone,
        language,
        date_format,
        created_at,
        updated_at,
        created_by,
        updated_by,
        version,
        active
    )
    SELECT
        c.id,
        'India',
        'INR',
        'Asia/Kolkata',
        'en',
        'yyyy-MM-dd',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        u.id,
        u.id,
        0,
        TRUE
    FROM seeded_company c
    CROSS JOIN seeded_user u
    ON CONFLICT (company_id) DO UPDATE SET
        country = EXCLUDED.country,
        currency = EXCLUDED.currency,
        timezone = EXCLUDED.timezone,
        language = EXCLUDED.language,
        date_format = EXCLUDED.date_format,
        updated_at = CURRENT_TIMESTAMP,
        updated_by = EXCLUDED.updated_by,
        active = TRUE
    RETURNING company_id
),
seeded_member AS (
    INSERT INTO company_members (
        id,
        company_id,
        user_id,
        job_title,
        department_name,
        membership_status,
        joined_at,
        created_at,
        updated_at,
        created_by,
        updated_by,
        version,
        active
    )
    SELECT
        '33333333-3333-4333-8333-333333333333',
        c.id,
        u.id,
        'Owner',
        'Administration',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        u.id,
        u.id,
        0,
        TRUE
    FROM seeded_company c
    CROSS JOIN seeded_user u
    ON CONFLICT (company_id, user_id) DO UPDATE SET
        job_title = EXCLUDED.job_title,
        department_name = EXCLUDED.department_name,
        membership_status = 'ACTIVE',
        joined_at = COALESCE(company_members.joined_at, CURRENT_TIMESTAMP),
        updated_at = CURRENT_TIMESTAMP,
        updated_by = EXCLUDED.updated_by,
        active = TRUE
    RETURNING id, company_id, user_id
),
owner_role AS (
    SELECT id
    FROM roles
    WHERE company_id IS NULL
      AND name = 'OWNER'
    LIMIT 1
),
seeded_user_role AS (
    INSERT INTO user_roles (
        id,
        company_id,
        membership_id,
        user_id,
        role_id,
        created_at,
        created_by,
        active
    )
    SELECT
        '66666666-6666-4666-8666-666666666666',
        m.company_id,
        m.id,
        m.user_id,
        r.id,
        CURRENT_TIMESTAMP,
        m.user_id,
        TRUE
    FROM seeded_member m
    CROSS JOIN owner_role r
    ON CONFLICT (membership_id, role_id) DO UPDATE SET
        active = TRUE
    RETURNING id
),
trial_plan AS (
    SELECT id
    FROM plans
    WHERE code = 'TRIAL'
    LIMIT 1
),
seeded_subscription AS (
    INSERT INTO subscriptions (
        id,
        company_id,
        plan_id,
        status,
        starts_at,
        trial_ends_at,
        current_period_ends_at,
        created_at,
        updated_at,
        created_by,
        updated_by,
        version,
        active
    )
    SELECT
        '44444444-4444-4444-8444-444444444444',
        c.id,
        p.id,
        'TRIAL',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP + INTERVAL '14 days',
        CURRENT_TIMESTAMP + INTERVAL '14 days',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        u.id,
        u.id,
        0,
        TRUE
    FROM seeded_company c
    CROSS JOIN seeded_user u
    CROSS JOIN trial_plan p
    ON CONFLICT (id) DO UPDATE SET
        status = EXCLUDED.status,
        trial_ends_at = EXCLUDED.trial_ends_at,
        current_period_ends_at = EXCLUDED.current_period_ends_at,
        updated_at = CURRENT_TIMESTAMP,
        updated_by = EXCLUDED.updated_by,
        active = TRUE
    RETURNING id
)
INSERT INTO audit_logs (
    id,
    company_id,
    user_id,
    action,
    entity_type,
    entity_id,
    metadata,
    ip_address,
    user_agent,
    created_at
)
SELECT
    '55555555-5555-4555-8555-555555555555',
    c.id,
    u.id,
    'COMPANY_REGISTERED',
    'company',
    c.id,
    '{"source":"flyway_seed","loginEmail":"test@gmail.com"}'::jsonb,
    NULL,
    'Flyway V3 seed migration',
    CURRENT_TIMESTAMP
FROM seeded_company c
CROSS JOIN seeded_user u
ON CONFLICT (id) DO NOTHING;
