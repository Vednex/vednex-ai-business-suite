from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from app.models.crm_lead import LeadSource, LeadStatus


class LeadCreate(BaseModel):
    company_name: str | None = None
    first_name: str | None = None
    last_name: str | None = None
    email: str | None = None
    phone: str | None = None
    job_title: str | None = None
    industry: str | None = None
    city: str | None = None

    requirement: str | None = None
    estimated_budget: float | None = None
    currency: str | None = Field(None, max_length=3)
    timeline: str | None = None

    source: LeadSource = LeadSource.OTHER
    assigned_user_id: UUID | None = None


class LeadUpdate(BaseModel):
    company_name: str | None = None
    first_name: str | None = None
    last_name: str | None = None
    email: str | None = None
    phone: str | None = None
    job_title: str | None = None
    industry: str | None = None
    city: str | None = None

    requirement: str | None = None
    estimated_budget: float | None = None
    currency: str | None = None
    timeline: str | None = None

    source: LeadSource | None = None
    status: LeadStatus | None = None
    assigned_user_id: UUID | None = None

    qualification_score: int | None = Field(None, ge=0, le=100)
    budget_confirmed: bool | None = None
    authority_confirmed: bool | None = None
    need_confirmed: bool | None = None
    timeline_confirmed: bool | None = None
    qualification_notes: str | None = None


class LeadAssignRequest(BaseModel):
    assigned_user_id: UUID


class LeadQualifyRequest(BaseModel):
    budget_confirmed: bool
    authority_confirmed: bool
    need_confirmed: bool
    timeline_confirmed: bool
    qualification_score: int = Field(ge=0, le=100)
    qualification_notes: str | None = None


class LeadDisqualifyRequest(BaseModel):
    qualification_notes: str | None = None


class LeadResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    company_id: UUID
    lead_number: str

    company_name: str | None
    first_name: str | None
    last_name: str | None
    email: str | None
    phone: str | None
    job_title: str | None
    industry: str | None
    city: str | None

    requirement: str | None
    estimated_budget: float | None
    currency: str | None
    timeline: str | None

    source: str
    status: str
    assigned_user_id: UUID | None

    qualification_score: int
    budget_confirmed: bool
    authority_confirmed: bool
    need_confirmed: bool
    timeline_confirmed: bool
    qualification_notes: str | None

    created_by: UUID | None
    updated_by: UUID | None
    created_at: datetime
    updated_at: datetime
    converted_at: datetime | None
    disqualified_at: datetime | None


class LeadListResponse(BaseModel):
    items: list[LeadResponse]
    total: int
    page: int
    page_size: int
