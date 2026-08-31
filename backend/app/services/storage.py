"""Local file storage for assets (originals, masks). Not the source of truth
in production — Part 2 will swap this for object storage."""
from __future__ import annotations

import os
import uuid
from pathlib import Path
from typing import Final

DATA_DIR: Final[Path] = Path(os.getenv("DATA_DIR", "./data")).resolve()
ORIGINALS_DIR: Final[Path] = DATA_DIR / "originals"
MASKS_DIR: Final[Path] = DATA_DIR / "masks"

for d in (ORIGINALS_DIR, MASKS_DIR):
    d.mkdir(parents=True, exist_ok=True)


def new_id(prefix: str) -> str:
    return f"{prefix}_{uuid.uuid4().hex[:12]}"


def save_original(file_bytes: bytes, suffix: str) -> str:
    fname = f"{new_id('garment')}.{suffix.lstrip('.') or 'jpg'}"
    path = ORIGINALS_DIR / fname
    path.write_bytes(file_bytes)
    return fname


def save_mask(mask_bytes: bytes, garment_id: str) -> str:
    fname = f"{garment_id}.png"
    path = MASKS_DIR / fname
    path.write_bytes(mask_bytes)
    return fname


def build_public_url(base_url: str, kind: str, filename: str) -> str:
    return f"{base_url.rstrip('/')}/static/{kind}/{filename}"
