"""Roboflow-backed segmentation. Hides provider-specific JSON shapes from the
rest of the backend.

The model id should be a Roboflow instance segmentation project, e.g.
`clothing-dcqa4/2`. The response `predictions[i].points` is converted into a
single-channel PNG mask using Pillow's polygon drawing."""
from __future__ import annotations

import base64
import io
import json
import logging
from typing import Any

import httpx
from PIL import Image, ImageDraw

from app.config import settings
from app.providers.segmentation import GarmentSegmentationService, SegmentationResult

logger = logging.getLogger(__name__)

ROBOFLOW_INSTANCE_SEG_URL = "https://outline.roboflow.com/{model}"


class RoboflowSegmentationError(RuntimeError):
    pass


class RoboflowGarmentSegmentationService(GarmentSegmentationService):
    def __init__(self, api_key: str | None = None, model: str | None = None,
                 timeout: float | None = None) -> None:
        self.api_key = api_key or settings.roboflow_api_key
        self.model = model or settings.roboflow_model
        self.timeout = timeout or settings.request_timeout_seconds

    def segment(self, image_bytes: bytes, mime: str, side: str) -> SegmentationResult:
        if not self.api_key:
            raise RoboflowSegmentationError("ROBOFLOW_API_KEY is not configured")

        url = ROBOFLOW_INSTANCE_SEG_URL.format(model=self.model)
        params = {"api_key": self.api_key}
        encoded = base64.b64encode(image_bytes).decode("ascii")
        headers = {"Content-Type": "application/x-www-form-urlencoded"}

        try:
            with httpx.Client(timeout=self.timeout) as client:
                response = client.post(url, params=params, content=encoded, headers=headers)
        except httpx.HTTPError as e:
            raise RoboflowSegmentationError(f"Network failure: {e}") from e

        if response.status_code == 401 or response.status_code == 403:
            raise RoboflowSegmentationError("Roboflow rejected the API key")
        if response.status_code == 404:
            raise RoboflowSegmentationError(
                f"Model not found: {self.model}. Check ROBOFLOW_MODEL."
            )
        if response.status_code == 429:
            raise RoboflowSegmentationError("Roboflow rate limit hit. Try again shortly.")
        if response.status_code >= 500:
            raise RoboflowSegmentationError(f"Roboflow server error: HTTP {response.status_code}")
        if response.status_code >= 400:
            raise RoboflowSegmentationError(
                f"Roboflow returned HTTP {response.status_code}: {response.text[:300]}"
            )

        try:
            payload: dict[str, Any] = response.json()
        except json.JSONDecodeError as e:
            raise RoboflowSegmentationError("Roboflow returned non-JSON body") from e

        return self._payload_to_mask(payload)

    @staticmethod
    def _payload_to_mask(payload: dict[str, Any]) -> SegmentationResult:
        # Roboflow response shape:
        # {
        #   "image": {"width": w, "height": h},
        #   "predictions": [
        #     {"x": ..., "y": ..., "width": ..., "height": ...,
        #      "points": [{"x": x, "y": y}, ...], "confidence": 0.9, "class": "..."}
        #   ]
        # }
        image_meta = payload.get("image") or {}
        try:
            width = int(image_meta["width"])
            height = int(image_meta["height"])
        except (KeyError, TypeError, ValueError) as e:
            raise RoboflowSegmentationError("Roboflow response missing image dimensions") from e

        predictions = payload.get("predictions") or []
        if not predictions:
            logger.warning("Roboflow returned no predictions; producing empty mask")

        # Multi-class models can return multiple classes; we union all polygons
        # into one binary mask. Per-class masks are Part 2.
        mask_img = Image.new("L", (width, height), 0)
        draw = ImageDraw.Draw(mask_img)

        confidences: list[float] = []
        for pred in predictions:
            pts = pred.get("points") or []
            if not pts:
                continue
            polygon = [(float(p["x"]), float(p["y"])) for p in pts if "x" in p and "y" in p]
            if len(polygon) >= 3:
                draw.polygon(polygon, fill=255)
            conf = pred.get("confidence")
            if isinstance(conf, (int, float)):
                confidences.append(float(conf))

        buf = io.BytesIO()
        mask_img.save(buf, format="PNG", optimize=True)
        avg_conf = sum(confidences) / len(confidences) if confidences else None
        return SegmentationResult(
            width=width,
            height=height,
            mask_png=buf.getvalue(),
            confidence=avg_conf,
        )
