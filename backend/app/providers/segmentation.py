"""Application-level segmentation interface. Concrete providers live in
`providers/`. The Android client must only depend on this interface."""
from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass


@dataclass
class SegmentationResult:
    width: int
    height: int
    mask_png: bytes        # raw PNG bytes, single-channel
    confidence: float | None


class GarmentSegmentationService(ABC):
    @abstractmethod
    def segment(self, image_bytes: bytes, mime: str, side: str) -> SegmentationResult: ...
