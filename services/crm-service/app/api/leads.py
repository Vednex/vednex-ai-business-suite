from datetime import UTC, datetime
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.api.deps import CurrentUser, get_current_user, require_permission
from app.db.base import get_db
from app.events.publisher import publish_event
from app.events.types import (
    CRM_LEAD_CREATED_V1,
    CRM_LEAD_DISQUALIFIED_V1,
    CRM_LEAD_QUALIFIED_V1,
)
from app.models.crm_lead import CrmLead, LeadSource, LeadStatus
from app.schemas.crm_lead import (
    LeadAssignRequest,
    LeadCreate,
    LeadDisqualifyRequest,
    LeadListResponse,
    LeadQualifyRequest,
    LeadResponse,
    LeadUpdate,
)

router = APIRouter(prefix="/leads", tags=["leads"])

_SOFT_DELETE_FILTER = CrmLead.deleted_at.is_(None)


def _next_lead_number(db: Session, company_id: UUID) -> str:
    last = (
        db.query(CrmLead.lead_number)
        .filter(CrmLead.company_id == company_id, _SOFT_DELETE_FILTER)
        .order_by(CrmLead.created_at.desc())
        .first()
    )
    if last and last[0].startswith("L-"):
        try:
            num = int(last[0].split("-")[1]) + 1
            return f"L-{num:06d}"
        except (IndexError, ValueError):
            pass
    return "L-000001"


def _get_lead_or_404(db: Session, lead_number: str, company_id: UUID) -> CrmLead:
    lead = (
        db.query(CrmLead)
        .filter(
            CrmLead.lead_number == lead_number,
            CrmLead.company_id == company_id,
            _SOFT_DELETE_FILTER,
        )
        .first()
    )
    if not lead:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Lead not found")
    return lead


# ── CREATE ────────────────────────────────────────────────────────────────────


@router.post("", response_model=LeadResponse, status_code=status.HTTP_201_CREATED)
def create_lead(
    payload: LeadCreate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
) -> CrmLead:
    require_permission(current_user, "CRM_CREATE")

    lead = CrmLead(
        company_id=current_user.company_id,
        lead_number=_next_lead_number(db, current_user.company_id),
        company_name=payload.company_name,
        first_name=payload.first_name,
        last_name=payload.last_name,
        email=payload.email,
        phone=payload.phone,
        job_title=payload.job_title,
        industry=payload.industry,
        city=payload.city,
        requirement=payload.requirement,
        estimated_budget=payload.estimated_budget,
        currency=payload.currency,
        timeline=payload.timeline,
        source=payload.source,
        status=LeadStatus.NEW,
        assigned_user_id=payload.assigned_user_id,
        created_by=current_user.user_id,
        updated_by=current_user.user_id,
    )
    db.add(lead)
    db.flush()

    publish_event(
        db,
        event_type=CRM_LEAD_CREATED_V1,
        aggregate_type="crm_lead",
        aggregate_id=lead.id,
        company_id=current_user.company_id,
        user_id=current_user.user_id,
        data={
            "leadId": str(lead.id),
            "leadNumber": lead.lead_number,
            "companyName": lead.company_name,
            "source": lead.source,
            "assignedUserId": str(lead.assigned_user_id) if lead.assigned_user_id else None,
        },
    )

    db.commit()
    db.refresh(lead)
    return lead


# ── LIST ──────────────────────────────────────────────────────────────────────


@router.get("", response_model=LeadListResponse)
def list_leads(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    status_filter: LeadStatus | None = Query(None, alias="status"),
    source_filter: LeadSource | None = Query(None, alias="source"),
    assigned_user_id: UUID | None = Query(None, alias="assignedUserId"),
    search: str | None = None,
    created_from: datetime | None = Query(None, alias="createdFrom"),
    created_to: datetime | None = Query(None, alias="createdTo"),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
) -> dict:
    require_permission(current_user, "CRM_VIEW")

    query = db.query(CrmLead).filter(
        CrmLead.company_id == current_user.company_id, _SOFT_DELETE_FILTER
    )

    if status_filter:
        query = query.filter(CrmLead.status == status_filter)
    if source_filter:
        query = query.filter(CrmLead.source == source_filter)
    if assigned_user_id:
        query = query.filter(CrmLead.assigned_user_id == assigned_user_id)
    if search:
        like = f"%{search}%"
        query = query.filter(
            (CrmLead.company_name.ilike(like))
            | (CrmLead.first_name.ilike(like))
            | (CrmLead.last_name.ilike(like))
            | (CrmLead.email.ilike(like))
        )
    if created_from:
        query = query.filter(CrmLead.created_at >= created_from)
    if created_to:
        query = query.filter(CrmLead.created_at <= created_to)

    total = query.count()
    items = (
        query.order_by(CrmLead.created_at.desc())
        .offset((page - 1) * page_size)
        .limit(page_size)
        .all()
    )

    return {"items": items, "total": total, "page": page, "page_size": page_size}


# ── GET ───────────────────────────────────────────────────────────────────────


@router.get("/{lead_number}", response_model=LeadResponse)
def get_lead(
    lead_number: str,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
) -> CrmLead:
    require_permission(current_user, "CRM_VIEW")
    return _get_lead_or_404(db, lead_number, current_user.company_id)


# ── UPDATE ────────────────────────────────────────────────────────────────────


@router.patch("/{lead_number}", response_model=LeadResponse)
def update_lead(
    lead_number: str,
    payload: LeadUpdate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
) -> CrmLead:
    require_permission(current_user, "CRM_UPDATE")

    lead = _get_lead_or_404(db, lead_number, current_user.company_id)
    update_data = payload.model_dump(exclude_unset=True)

    if "status" in update_data:
        new_status = update_data["status"]
        if new_status == LeadStatus.CONVERTED and lead.converted_at is None:
            lead.converted_at = datetime.now(UTC)
        elif new_status == LeadStatus.DISQUALIFIED and lead.disqualified_at is None:
            lead.disqualified_at = datetime.now(UTC)

    for field, value in update_data.items():
        setattr(lead, field, value)

    lead.updated_by = current_user.user_id
    db.commit()
    db.refresh(lead)
    return lead


# ── DELETE (soft) ─────────────────────────────────────────────────────────────


@router.delete("/{lead_number}", status_code=status.HTTP_204_NO_CONTENT)
def delete_lead(
    lead_number: str,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
) -> None:
    require_permission(current_user, "CRM_DELETE")

    lead = _get_lead_or_404(db, lead_number, current_user.company_id)
    lead.deleted_at = datetime.now(UTC)
    lead.updated_by = current_user.user_id
    db.commit()


# ── ASSIGN ────────────────────────────────────────────────────────────────────


@router.post("/{lead_number}/assign", response_model=LeadResponse)
def assign_lead(
    lead_number: str,
    payload: LeadAssignRequest,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
) -> CrmLead:
    require_permission(current_user, "CRM_UPDATE")

    lead = _get_lead_or_404(db, lead_number, current_user.company_id)
    lead.assigned_user_id = payload.assigned_user_id
    lead.updated_by = current_user.user_id
    db.commit()
    db.refresh(lead)
    return lead


# ── QUALIFY ───────────────────────────────────────────────────────────────────


@router.post("/{lead_number}/qualify", response_model=LeadResponse)
def qualify_lead(
    lead_number: str,
    payload: LeadQualifyRequest,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
) -> CrmLead:
    require_permission(current_user, "CRM_UPDATE")

    lead = _get_lead_or_404(db, lead_number, current_user.company_id)

    if lead.status in (LeadStatus.CONVERTED, LeadStatus.DISQUALIFIED):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Cannot qualify a lead with status {lead.status}",
        )

    lead.budget_confirmed = payload.budget_confirmed
    lead.authority_confirmed = payload.authority_confirmed
    lead.need_confirmed = payload.need_confirmed
    lead.timeline_confirmed = payload.timeline_confirmed
    lead.qualification_score = payload.qualification_score
    lead.qualification_notes = payload.qualification_notes
    lead.status = LeadStatus.QUALIFIED
    lead.updated_by = current_user.user_id

    publish_event(
        db,
        event_type=CRM_LEAD_QUALIFIED_V1,
        aggregate_type="crm_lead",
        aggregate_id=lead.id,
        company_id=current_user.company_id,
        user_id=current_user.user_id,
        data={
            "leadId": str(lead.id),
            "leadNumber": lead.lead_number,
            "qualificationScore": lead.qualification_score,
            "budgetConfirmed": lead.budget_confirmed,
            "authorityConfirmed": lead.authority_confirmed,
            "needConfirmed": lead.need_confirmed,
            "timelineConfirmed": lead.timeline_confirmed,
        },
    )

    db.commit()
    db.refresh(lead)
    return lead


# ── DISQUALIFY ────────────────────────────────────────────────────────────────


@router.post("/{lead_number}/disqualify", response_model=LeadResponse)
def disqualify_lead(
    lead_number: str,
    payload: LeadDisqualifyRequest,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
) -> CrmLead:
    require_permission(current_user, "CRM_UPDATE")

    lead = _get_lead_or_404(db, lead_number, current_user.company_id)

    if lead.status in (LeadStatus.CONVERTED, LeadStatus.DISQUALIFIED):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Cannot disqualify a lead with status {lead.status}",
        )

    lead.status = LeadStatus.DISQUALIFIED
    lead.disqualified_at = datetime.now(UTC)
    lead.qualification_notes = payload.qualification_notes
    lead.updated_by = current_user.user_id

    publish_event(
        db,
        event_type=CRM_LEAD_DISQUALIFIED_V1,
        aggregate_type="crm_lead",
        aggregate_id=lead.id,
        company_id=current_user.company_id,
        user_id=current_user.user_id,
        data={
            "leadId": str(lead.id),
            "leadNumber": lead.lead_number,
            "disqualifiedAt": lead.disqualified_at.isoformat(),
        },
    )

    db.commit()
    db.refresh(lead)
    return lead
