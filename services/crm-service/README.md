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
