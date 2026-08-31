"""FastAPI application entry point."""
from __future__ import annotations

import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.api.garments import router as garments_router
from app.services.storage import MASKS_DIR, ORIGINALS_DIR

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s — %(message)s",
)

app = FastAPI(
    title="AltThread Backend",
    version="0.1.0",
    description="Garment segmentation (and later, virtual try-on) service.",
)

# Open CORS for development. Tighten in production.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Serve original + mask images so the Android client can preview them.
app.mount("/static/originals", StaticFiles(directory=str(ORIGINALS_DIR)), name="originals")
app.mount("/static/masks", StaticFiles(directory=str(MASKS_DIR)), name="masks")

app.include_router(garments_router)
