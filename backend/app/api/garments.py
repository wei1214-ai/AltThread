"""HTTP routes for garment operations."""
from __future__ import annotations

import io
import logging
import os

from fastapi import APIRouter, File, Form, HTTPException, Request, UploadFile
from PIL import Image, UnidentifiedImageError

from app.models.schemas import HealthResponse, SegmentResponse
from app.providers.mock_provider import MockGarmentSegmentationService
from app.providers.roboflow_provider import (
    RoboflowGarmentSegmentationService,
    RoboflowSegmentationError,
)
from app.services import storage
from app.config import settings

logger = logging.getLogger(__name__)

router = APIRouter()

ALLOWED_MIME = {"image/jpeg", "image/png", "image/webp"}
ALLOWED_SIDES = {"FRONT", "BACK"}
MAX_UPLOAD_BYTES = int(os.getenv("UPLOAD_MAX_BYTES", str(20 * 1024 * 1024)))


@router.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    from app.config import settings
    return HealthResponse(
        status="ok",
        roboflow_configured=settings.roboflow_configured,
        model=settings.roboflow_model,
    )


@router.post("/v1/garments/segment", response_model=SegmentResponse)
async def segment_garment(
    request: Request,
    image: UploadFile = File(...),
    side: str = Form(...),
) -> SegmentResponse:
    side_norm = side.strip().upper()
    if side_norm not in ALLOWED_SIDES:
        raise HTTPException(status_code=400, detail=f"side must be one of {sorted(ALLOWED_SIDES)}")

    mime = (image.content_type or "").lower()
    if mime not in ALLOWED_MIME:
        raise HTTPException(status_code=415, detail=f"Unsupported image MIME type: {mime}")

    file_bytes = await image.read()
    if not file_bytes:
        raise HTTPException(status_code=400, detail="Empty image upload")
    if len(file_bytes) > MAX_UPLOAD_BYTES:
        raise HTTPException(
            status_code=413,
            detail=f"Image too large; max {MAX_UPLOAD_BYTES // (1024 * 1024)} MB",
        )

    # Validate that the bytes are an actual image, not renamed junk.
    try:
        with Image.open(io.BytesIO(file_bytes)) as probe:
            probe.verify()
    except (UnidentifiedImageError, OSError):
        raise HTTPException(status_code=400, detail="File is not a valid image")

    # Re-open to recover dimensions after verify().
    try:
        with Image.open(io.BytesIO(file_bytes)) as img:
            width, height = img.size
    except (UnidentifiedImageError, OSError) as e:
        raise HTTPException(status_code=400, detail=f"Cannot read image: {e}")

    try:
        provider = RoboflowGarmentSegmentationService()
        result = provider.segment(file_bytes, mime, side_norm)
    except RoboflowSegmentationError as e:
        logger.warning("Segmentation failure: %s", e)
        raise HTTPException(status_code=502, detail=str(e)) from e
    except Exception as e:  # last-resort guard
        logger.exception("Unexpected segmentation error")
        raise HTTPException(status_code=500, detail="Segmentation failed") from e

    suffix = (image.filename or "upload.jpg").rsplit(".", 1)[-1].lower() or "jpg"
    garment_id = storage.new_id("garment")
    storage.save_original(file_bytes, suffix=suffix)
    storage.save_mask(result.mask_png, garment_id=garment_id)

    base = str(request.base_url).rstrip("/")
    image_url = f"{base}/static/originals/{storage.new_id('orig')}.{suffix}"
    mask_url = f"{base}/static/masks/{garment_id}.png"

    return SegmentResponse(
        garmentId=garment_id,
        side=side_norm,
        imageUrl=image_url,
        maskUrl=mask_url,
        width=width,
        height=height,
    )
