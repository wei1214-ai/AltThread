"""Application configuration loaded from environment variables."""
from __future__ import annotations

import os
from dataclasses import dataclass
from dotenv import load_dotenv

load_dotenv()


@dataclass(frozen=True)
class Settings:
    roboflow_api_key: str
    roboflow_model: str
    fal_key: str
    upload_max_bytes: int
    request_timeout_seconds: float

    @property
    def roboflow_configured(self) -> bool:
        return bool(self.roboflow_api_key)


def load_settings() -> Settings:
    return Settings(
        roboflow_api_key=os.getenv("ROBOFLOW_API_KEY", "").strip(),
        roboflow_model=os.getenv("ROBOFLOW_MODEL", "clothes-segmentation-final/1").strip(),
        fal_key=os.getenv("FAL_KEY", "").strip(),
        upload_max_bytes=int(os.getenv("UPLOAD_MAX_BYTES", "20971520")),  # 20 MB
        request_timeout_seconds=float(os.getenv("REQUEST_TIMEOUT_SECONDS", "60")),
    )


settings = load_settings()
