# AltThread Backend

Python FastAPI service that the Android app talks to.

```
Android  ──>  Your Backend  ──>  Roboflow (hosted inference)
                 │
                 └─>  (Part 2) FASHN / fal virtual try-on
```

API keys live ONLY in `backend/.env`. They never appear in the Android app.

## Endpoints (Part 1)

- `POST /v1/garments/segment` — multipart `image` + `side`; returns segmentation result.

## Setup

```bash
cd backend
python -m venv .venv
source .venv/bin/activate     # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env          # then fill in ROBOFLOW_API_KEY
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

The Android emulator reaches this server at `http://10.0.2.2:8000/`.

## Roboflow model

Default model id: `clothing-dcqa4/2` (instance segmentation, "outline" endpoint).
Override with `ROBOFLOW_MODEL=your-slug/your-version` in `.env`.

## Tests

```bash
pytest -q
```
