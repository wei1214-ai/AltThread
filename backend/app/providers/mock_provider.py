"""Local fallback used when no Roboflow key is configured.

Returns the original image's dimensions and a full-white mask, so the Android
editor can render something sensible without any network call. The point of
Part 1 is to prove the pipeline; the real provider is the one in
`roboflow_provider.py`."""
from __future__ import annotations

import io

from PIL import Image

from app.providers.segmentation import GarmentSegmentationService, SegmentationResult


class MockGarmentSegmentationService(GarmentSegmentationService):
    def segment(self, image_bytes: bytes, mime: str, side: str) -> SegmentationResult:
        with Image.open(io.BytesIO(image_bytes)) as img:
            width, height = img.size
        # White mask = "the whole image is the garment". Not realistic, but it
        # proves the renderer pipeline end-to-end.
        mask = Image.new("L", (width, height), 255)
        buf = io.BytesIO()
        mask.save(buf, format="PNG", optimize=True)
        return SegmentationResult(
            width=width,
            height=height,
            mask_png=buf.getvalue(),
            confidence=1.0,
        )
