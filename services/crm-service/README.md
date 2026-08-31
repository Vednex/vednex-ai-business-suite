# CRM Service

FastAPI service scaffold for the CRM bounded context.

This project is intentionally empty for now. CRM endpoints, models, migrations, and gateway registration will be added in later steps.

## Local Run

```powershell
cd services/crm-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -e ".[dev]"
uvicorn app.main:app --host 0.0.0.0 --port 8083 --reload
```

## Default Settings

- Port: `8083`
- API prefix reserved for future use: `/api/crm`
- Database reserved for future use: `crm_db`

## Remember

### Why config.py exists

`config.py` is needed because multiple files import from it:

| File | Uses |
|------|------|
| `deps.py` | `jwt_public_key_file`, `jwt_issuer`, `jwt_audience` for JWT validation |
| `main.py` | `app_name`, `app_debug`, `cors_allowed_origins`, `api_prefix` for FastAPI setup |
| `outbox.py` | `rabbitmq_url` to check if RabbitMQ is configured |
| `publisher.py` | `app_name` as event producer name |

### Docker vs Local Dev

- In Docker, `JWT_PUBLIC_KEY_FILE` is set to `/run/secrets/vednex/jwt-public.pem` via docker-compose
- In local dev, `.env` sets `JWT_PUBLIC_KEY_FILE=../../secrets/jwt-public.pem` (relative to `services/crm-service/`)
- The `.env` values are only used for local development outside Docker

### Migration

- Migration naming: `001_crm_foundation` + `002_leads` (spec) vs `001_crm_leads` + `002_add_fields_and_outbox` (actual) — content covers spec, naming differs
- If Alembic can't connect, use `migration.sql` directly via `docker exec`
- If table already exists, stamp migration: `python -m alembic stamp 001` then `python -m alembic upgrade head`
