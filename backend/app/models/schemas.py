"""Wire-format DTOs. Android domain layer must never see provider-specific shapes."""
from __future__ import annotations

from pydantic import BaseModel


class SegmentResponse(BaseModel):
    garmentId: str
    side: str          # "FRONT" or "BACK"
    imageUrl: str      # preserved original image URL or echo
    maskUrl: str       # PNG mask, transparent outside the garment
    width: int
    height: int


class HealthResponse(BaseModel):
    status: str
    roboflow_configured: bool
    model: str
