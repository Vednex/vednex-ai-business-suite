"""CRM leads table

Revision ID: 001
Revises:
Create Date: 2026-08-30
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import UUID

revision: str = "001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"")

    op.create_table(
        "crm_leads",
        sa.Column("id", UUID(as_uuid=True), primary_key=True, server_default=sa.text("uuid_generate_v4()")),
        sa.Column("company_id", UUID(as_uuid=True), nullable=False, index=True),
        sa.Column("lead_number", sa.String(50), nullable=False),

        # Contact information
        sa.Column("company_name", sa.String(200)),
        sa.Column("first_name", sa.String(100)),
        sa.Column("last_name", sa.String(100)),
        sa.Column("email", sa.String(320)),
        sa.Column("phone", sa.String(40)),
        sa.Column("job_title", sa.String(120)),

        # Requirement & budget
        sa.Column("requirement", sa.Text),
        sa.Column("estimated_budget", sa.Numeric(15, 2)),
        sa.Column("currency", sa.String(3)),
        sa.Column("timeline", sa.String(200)),

        # Pipeline
        sa.Column("source", sa.String(30), nullable=False, server_default="OTHER"),
        sa.Column("status", sa.String(30), nullable=False, server_default="NEW"),
        sa.Column("assigned_user_id", UUID(as_uuid=True)),

        # Qualification (BANT)
        sa.Column("qualification_score", sa.Integer, nullable=False, server_default="0"),
        sa.Column("budget_confirmed", sa.Boolean, nullable=False, server_default=sa.text("FALSE")),
        sa.Column("authority_confirmed", sa.Boolean, nullable=False, server_default=sa.text("FALSE")),
        sa.Column("need_confirmed", sa.Boolean, nullable=False, server_default=sa.text("FALSE")),
        sa.Column("timeline_confirmed", sa.Boolean, nullable=False, server_default=sa.text("FALSE")),
        sa.Column("qualification_notes", sa.Text),

        # Audit
        sa.Column("created_by", UUID(as_uuid=True)),
        sa.Column("updated_by", UUID(as_uuid=True)),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),

        # Lifecycle timestamps
        sa.Column("converted_at", sa.DateTime(timezone=True)),
        sa.Column("disqualified_at", sa.DateTime(timezone=True)),

        # Constraints
        sa.UniqueConstraint("company_id", "lead_number", name="uk_crm_leads_company_lead_number"),
        sa.CheckConstraint(
            "status IN ('NEW','CONTACTED','QUALIFYING','QUALIFIED','CONVERTED','DISQUALIFIED')",
            name="ck_crm_leads_status",
        ),
        sa.CheckConstraint(
            "qualification_score >= 0 AND qualification_score <= 100",
            name="ck_crm_leads_qualification_score",
        ),
    )

    op.create_index("ix_crm_leads_company_status", "crm_leads", ["company_id", "status"])
    op.create_index("ix_crm_leads_company_assigned_user", "crm_leads", ["company_id", "assigned_user_id"])
    op.create_index("ix_crm_leads_company_created_at", "crm_leads", ["company_id", "created_at"])


def downgrade() -> None:
    op.drop_table("crm_leads")
