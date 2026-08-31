from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, Field


class EventEnvelope(BaseModel):
    eventId: UUID
    eventType: str
    eventVersion: int = Field(ge=1)
    producer: str
    companyId: UUID
    userId: UUID
    correlationId: UUID
    occurredAt: datetime
    data: dict = Field(default_factory=dict)
