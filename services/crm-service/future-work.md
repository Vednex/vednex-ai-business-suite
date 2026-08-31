# CRM Lead Module — Future Work

## Test Scenarios (Pending)

### 1. Create Lead
- POST with valid payload → 201, lead_number auto-generated (L-000001)
- POST without JWT → 401
- POST without CRM_CREATE permission → 403

### 2. Company Isolation
- Company A creates lead → Company B cannot GET/PATCH/DELETE it
- Company A cannot read Company B's lead via list endpoint

### 3. Lead Number Generation
- First lead for company → L-000001
- Second lead for company → L-000002
- Numbers are per-company (Company A L-000001 ≠ Company B L-000001)

### 4. Pagination
- page=1, size=2 returns 2 items
- page=2, size=2 returns next 2 items
- total count reflects full dataset

### 5. Filters
- status=NEW returns only NEW leads
- source=REFERRAL returns only REFERRAL leads
- assignedUserId=X returns only leads assigned to X
- search=john matches first_name, last_name, company_name, email
- createdFrom/createdTo filters by date range
- Combined filters work together

### 6. Assignment
- POST /assign with valid assigned_user_id → 200, assigned_user_id updated
- Reassign to different user → works

### 7. Qualification
- POST /qualify with BANT fields → status changes to QUALIFIED
- qualification_score updated
- crm.lead.qualified.v1 event published

### 8. Disqualification
- POST /disqualify → status changes to DISQUALIFIED
- disqualified_at timestamp set
- crm.lead.disqualified.v1 event published

### 9. Invalid Transitions
- Qualify a DISQUALIFIED lead → 409 Conflict
- Qualify a CONVERTED lead → 409 Conflict
- Disqualify a QUALIFIED lead → 409 Conflict
- Disqualify a CONVERTED lead → 409 Conflict

### 10. Soft Delete
- DELETE → 204, lead deleted_at set
- GET after DELETE → 404
- List excludes soft-deleted leads

### 11. Missing Permission → 403
- User without CRM_VIEW → GET returns 403
- User without CRM_CREATE → POST returns 403
- User without CRM_UPDATE → PATCH returns 403
- User without CRM_DELETE → DELETE returns 403

### 12. Missing JWT → 401
- No Authorization header → 401
- Invalid token → 401
- Expired token → 401

---

## Open Questions

### Qualify/Qualified Endpoint Naming
We implemented `POST /leads/{lead_number}/qualify` which sets status to QUALIFIED.
The spec says "Qualify request" → status becomes QUALIFIED.

Need to think about:
- Should the endpoint be `/qualify` or `/qualified`?
- Is the current flow correct: qualify = BANT check → set QUALIFIED?
- Or should qualify be a separate action from setting status to QUALIFIED?
- Consider alignment with Developer 2's Opportunity service — they will need lead qualification data to create Opportunities.

---

## Developer Notes

### Architecture
- FastAPI + SQLAlchemy (sync) + PostgreSQL
- JWT validation via python-jose with RSA public key from identity-service
- Tenant isolation: every query filtered by `company_id` from JWT
- Soft delete: `deleted_at` column, never hard delete
- Lead number: `L-000001` format, auto-incremented per company

### Event Flow
```
leads.py → publisher.py (write to outbox_events in same DB tx)
                ↓
          outbox.py (background poller, reads PENDING, publishes to RabbitMQ)
                ↓
          RabbitMQ vednex.events exchange (routing key = event type)
```

### Key Design Decisions
1. **lead_number as external ID** — UUIDs never exposed in URLs, `lead_number` is the public identifier
2. **Soft delete** — `deleted_at` set on DELETE, all queries filter `deleted_at IS NULL`
3. **Permission model** — OWNER: full access + DELETE; SALES: read + create + update (no DELETE); others: none
4. **Qualification** — `/qualify` endpoint performs BANT check, sets status to QUALIFIED, emits `crm.lead.qualified.v1`
5. **Invalid transitions** — cannot qualify/disqualify a CONVERTED or DISQUALIFIED lead (returns 409)

### Files
| File | Purpose |
|------|---------|
| `app/models/crm_lead.py` | SQLAlchemy model |
| `app/schemas/crm_lead.py` | Pydantic schemas |
| `app/api/deps.py` | JWT auth + permission helper |
| `app/api/leads.py` | All 8 CRUD endpoints |
| `app/events/envelope.py` | EventEnvelope model |
| `app/events/types.py` | Event type constants |
| `app/events/publisher.py` | Write events to outbox |
| `app/events/outbox.py` | Background poller → RabbitMQ |
| `app/core/config.py` | Settings (DB, JWT, RabbitMQ) |
| `alembic/versions/002_add_fields_and_outbox.py` | Migration |

### Running Locally
```powershell
cd services/crm-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -e ".[dev]"
uvicorn app.main:app --host 0.0.0.0 --port 8083 --reload
```

### Docker Rebuild
```powershell
docker build -t vednex-crm-service -f Dockerfile .
docker compose up -d crm-service
```

### Database Migration
```powershell
# Via Alembic (if DB accessible)
alembic upgrade head

# Via SQL file (if Alembic can't connect)
docker exec -i vednex-postgres psql -U vednex -d crm_db < migration.sql
```
- [ ] Rebuild Docker container
- [ ] Run migration 002 against crm_db
- [ ] Test all 8 endpoints end-to-end

---

## Notes
- Migration naming: spec says `001_crm_foundation` + `002_leads`, we have `001_crm_leads` + `002_add_fields_and_outbox` — content covers spec, just naming differs
- `{lead_id}` vs `{lead_number}`: spec says lead_id, we use lead_number intentionally (UUIDs never in URLs)
- Outbox: spec says keep abstraction ready without unsafe direct RabbitMQ publishing — we implemented outbox pattern matching identity-service
