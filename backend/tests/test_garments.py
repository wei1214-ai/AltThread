"""Tests for the garments API. The provider is mocked so no API key is needed."""
from __future__ import annotations

import io

import pytest
from fastapi.testclient import TestClient
from PIL import Image

from app.main import app
from app.providers import roboflow_provider
from app.providers.segmentation import SegmentationResult


@pytest.fixture
def client() -> TestClient:
    return TestClient(app)


@pytest.fixture
def tiny_jpeg() -> tuple[bytes, str, str]:
    img = Image.new("RGB", (32, 32), color=(255, 0, 0))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    return buf.getvalue(), "test.jpg", "image/jpeg"


@pytest.fixture(autouse=True)
def patch_provider(monkeypatch):
    """Skip the live Roboflow call. The service returns a fixed mask."""
    def fake_segment(self, image_bytes, mime, side):
        return SegmentationResult(
            width=32, height=32,
            mask_png=b"\x89PNG\r\n\x1a\n" + b"fake",
            confidence=0.9,
        )
    monkeypatch.setattr(
        roboflow_provider.RoboflowGarmentSegmentationService,
        "segment",
        fake_segment,
    )
    yield


def test_health(client: TestClient) -> None:
    r = client.get("/health")
    assert r.status_code == 200
    body = r.json()
    assert body["status"] == "ok"
    assert "model" in body


def test_segment_success(client: TestClient, tiny_jpeg) -> None:
    data, name, mime = tiny_jpeg
    r = client.post(
        "/v1/garments/segment",
        files={"image": (name, data, mime)},
        data={"side": "FRONT"},
    )
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["side"] == "FRONT"
    assert body["width"] == 32
    assert body["height"] == 32
    assert body["garmentId"].startswith("garment_")
    assert body["maskUrl"].endswith(".png")


def test_segment_rejects_invalid_side(client: TestClient, tiny_jpeg) -> None:
    data, name, mime = tiny_jpeg
    r = client.post(
        "/v1/garments/segment",
        files={"image": (name, data, mime)},
        data={"side": "top"},
    )
    assert r.status_code == 400


def test_segment_rejects_bad_mime(client: TestClient, tiny_jpeg) -> None:
    data, name, _ = tiny_jpeg
    r = client.post(
        "/v1/garments/segment",
        files={"image": (name, data, "application/pdf")},
        data={"side": "FRONT"},
    )
    assert r.status_code == 415


def test_segment_rejects_corrupt_image(client: TestClient) -> None:
    r = client.post(
        "/v1/garments/segment",
        files={"image": ("bad.jpg", b"not an image", "image/jpeg")},
        data={"side": "BACK"},
    )
    assert r.status_code == 400
