import uuid
from datetime import UTC, datetime

import sqlalchemy as sa
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.events.envelope import EventEnvelope

_OUTBOX_INSERT = sa.text(
    "INSERT INTO outbox_events "
    "(id, aggregate_type, aggregate_id, event_type, event_version, "
    "payload, company_id, user_id, correlation_id, status, created_at, next_retry_at) "
    "VALUES (:id, :aggregate_type, :aggregate_id, :event_type, :event_version, "
    "::payload_json, :company_id, :user_id, :correlation_id, 'PENDING', now(), now())"
)


def publish_event(
    db: Session,
    *,
    event_type: str,
    aggregate_type: str,
    aggregate_id: uuid.UUID,
    company_id: uuid.UUID,
    user_id: uuid.UUID,
    correlation_id: uuid.UUID | None = None,
    data: dict | None = None,
) -> None:
    settings = get_settings()
    envelope = EventEnvelope(
        eventId=uuid.uuid4(),
        eventType=event_type,
        eventVersion=1,
        producer=settings.app_name,
        companyId=company_id,
        userId=user_id,
        correlationId=correlation_id or uuid.uuid4(),
        occurredAt=datetime.now(UTC),
        data=data or {},
    )

    db.execute(
        sa.text(
            "INSERT INTO outbox_events "
            "(id, aggregate_type, aggregate_id, event_type, event_version, "
            "payload, company_id, user_id, correlation_id, status, created_at, next_retry_at) "
            "VALUES "
            "(:id, :agg_type, :agg_id, :evt_type, :evt_ver, "
            "cast(:payload as jsonb), :company_id, :user_id, :corr_id, 'PENDING', now(), now())"
        ),
        {
            "id": uuid.uuid4(),
            "agg_type": aggregate_type,
            "agg_id": aggregate_id,
            "evt_type": event_type,
            "evt_ver": 1,
            "payload": envelope.model_dump_json(),
            "company_id": company_id,
            "user_id": user_id,
            "corr_id": envelope.correlationId,
        },
    )
