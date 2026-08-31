import enum
import uuid
from datetime import datetime

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    DateTime,
    Index,
    Numeric,
    String,
    Text,
    UniqueConstraint,
    func,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class LeadStatus(str, enum.Enum):
    NEW = "NEW"
    CONTACTED = "CONTACTED"
    QUALIFYING = "QUALIFYING"
    QUALIFIED = "QUALIFIED"
    CONVERTED = "CONVERTED"
    DISQUALIFIED = "DISQUALIFIED"


class LeadSource(str, enum.Enum):
    WEBSITE = "WEBSITE"
    REFERRAL = "REFERRAL"
    COLD_CALL = "COLD_CALL"
    ADVERTISEMENT = "ADVERTISEMENT"
    SOCIAL_MEDIA = "SOCIAL_MEDIA"
    PARTNER = "PARTNER"
    OTHER = "OTHER"


class CrmLead(Base):
    __tablename__ = "crm_leads"
    __table_args__ = (
        UniqueConstraint("company_id", "lead_number", name="uk_crm_leads_company_lead_number"),
        CheckConstraint(
            "status IN ('NEW','CONTACTED','QUALIFYING','QUALIFIED','CONVERTED','DISQUALIFIED')",
            name="ck_crm_leads_status",
        ),
        CheckConstraint(
            "qualification_score >= 0 AND qualification_score <= 100",
            name="ck_crm_leads_qualification_score",
        ),
        Index("ix_crm_leads_company_status", "company_id", "status"),
        Index("ix_crm_leads_company_assigned_user", "company_id", "assigned_user_id"),
        Index("ix_crm_leads_company_created_at", "company_id", "created_at"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    company_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False, index=True)
    lead_number: Mapped[str] = mapped_column(String(50), nullable=False)

    # Contact information
    company_name: Mapped[str | None] = mapped_column(String(200))
    first_name: Mapped[str | None] = mapped_column(String(100))
    last_name: Mapped[str | None] = mapped_column(String(100))
    email: Mapped[str | None] = mapped_column(String(320))
    phone: Mapped[str | None] = mapped_column(String(40))
    job_title: Mapped[str | None] = mapped_column(String(120))
    industry: Mapped[str | None] = mapped_column(String(120))
    city: Mapped[str | None] = mapped_column(String(100))

    # Requirement & budget
    requirement: Mapped[str | None] = mapped_column(Text)
    estimated_budget: Mapped[float | None] = mapped_column(Numeric(15, 2))
    currency: Mapped[str | None] = mapped_column(String(3))
    timeline: Mapped[str | None] = mapped_column(String(200))

    # Pipeline
    source: Mapped[str] = mapped_column(String(30), nullable=False, default=LeadSource.OTHER)
    status: Mapped[str] = mapped_column(
        String(30), nullable=False, default=LeadStatus.NEW
    )
    assigned_user_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True))

    # Qualification (BANT)
    qualification_score: Mapped[int] = mapped_column(default=0)
    budget_confirmed: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    authority_confirmed: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    need_confirmed: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    timeline_confirmed: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    qualification_notes: Mapped[str | None] = mapped_column(Text)

    # Audit
    created_by: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True))
    updated_by: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )

    # Lifecycle timestamps
    converted_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    disqualified_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    deleted_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
