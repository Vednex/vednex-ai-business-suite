"""Add industry, city, deleted_at, and outbox_events table

Revision ID: 002
Revises: 001
Create Date: 2026-08-30
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import UUID, JSONB

revision: str = "002"
down_revision: Union[str, None] = "001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column("crm_leads", sa.Column("industry", sa.String(120)))
    op.add_column("crm_leads", sa.Column("city", sa.String(100)))
    op.add_column(
        "crm_leads",
        sa.Column("deleted_at", sa.DateTime(timezone=True)),
    )

    op.create_table(
        "outbox_events",
        sa.Column("id", UUID(as_uuid=True), primary_key=True, server_default=sa.text("uuid_generate_v4()")),
        sa.Column("aggregate_type", sa.String(120), nullable=False),
        sa.Column("aggregate_id", UUID(as_uuid=True), nullable=False),
        sa.Column("event_type", sa.String(160), nullable=False),
        sa.Column("event_version", sa.Integer, nullable=False),
        sa.Column("payload", JSONB, nullable=False),
        sa.Column("company_id", UUID(as_uuid=True)),
        sa.Column("user_id", UUID(as_uuid=True)),
        sa.Column("correlation_id", UUID(as_uuid=True), nullable=False),
        sa.Column("status", sa.String(30), nullable=False, server_default="PENDING"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column("published_at", sa.DateTime(timezone=True)),
        sa.Column("retry_count", sa.Integer, nullable=False, server_default="0"),
        sa.Column("next_retry_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column("last_error", sa.String(1000)),
        sa.Column("version", sa.BigInteger, nullable=False, server_default="0"),
    )

    op.create_index("ix_outbox_events_status_retry", "outbox_events", ["status", "next_retry_at"])
    op.create_index("ix_outbox_events_company", "outbox_events", ["company_id"])


def downgrade() -> None:
    op.drop_table("outbox_events")
    op.drop_column("crm_leads", "deleted_at")
    op.drop_column("crm_leads", "city")
    op.drop_column("crm_leads", "industry")
