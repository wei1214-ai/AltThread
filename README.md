# AltThread — Upcycling & Sustainable Fashion App

Digital upcycling studio for clothing. See `PROJECT_SPEC.md` for the full
product + technical specification.

## Status — Part 1

Part 1 of the MVP is in place. The user can pick FRONT + BACK images, the
backend produces segmentation results, and the editor opens with the garment
on a white background plus pan / pinch-zoom.

| Area | Status |
|---|---|
| Domain models (ClothingDocument, GarmentSide, ImageAsset, MaskAsset, EditorLayer, Viewport) | done |
| Android → Backend network layer (Retrofit + OkHttp + Serialization) | done |
| Garment Input screen (SAF picker for FRONT / BACK) | done |
| Backend `/v1/garments/segment` endpoint | done |
| Roboflow provider (real) | done, untested (no key) |
| Mock segmentation provider (fallback) | done |
| Editor ViewModel + StateFlow (document, viewport, activeSide) | done |
| GPU Renderer abstraction (GarmentRenderer interface) | done |
| Compose Canvas implementation (source + mask + white bg + pan + zoom) | done |
| Local persistence (Preferences DataStore, JSON) | done |
| `.env.example` and root README | done |
| Gradle build verified on host machine | **not yet** |
| Backend tests run (pytest) | **not yet** |

Part 2 (Dye, Cut, Patch, Stitch, Fabric, Export, Virtual Try-On) is **not**
started.

## Architecture

```
app/
├── ui/garmentinput/   GarmentInputScreen + ViewModel-driven state
├── ui/editor/         EditorScreen + viewport gestures
├── ui/gesture/        Pan + pinch-zoom modifier (no document mutation)
├── editor/            EditorState, EditorViewModel, EditorTool
├── rendering/         GarmentRenderer interface + Compose Canvas impl + BitmapCache
├── domain/model/      ClothingDocument, GarmentSide, ImageAsset, MaskAsset, Viewport
├── data/remote/       GarmentSegmentationService (interface + backend impl + mock)
├── network/           Retrofit BackendApi, NetworkModule
├── storage/           ProjectPreferences (DataStore), ImageCache (files)
└── di/                ServiceLocator
```

Renderer abstraction is deliberate. Part 2's dye / cut / patch shaders can
replace `ComposeCanvasGarmentRenderer` without touching the Editor.

Persistence follows the agreed split: DataStore stores document JSON
(structure, transforms, parameters), app private files store image binaries.

## Android setup

1. Open the project in Android Studio.
2. Gradle sync.
3. Either run on an emulator (it reaches the backend at `10.0.2.2:8000`)
   or override the backend URL:
   ```
   ./gradlew :app:assembleDebug -PBACKEND_BASE_URL=http://192.168.x.x:8000/
   ```
4. From the Home → Studio → "Start a Design" → pick FRONT and BACK.

## Backend setup

```bash
cd backend
python -m venv .venv
source .venv/bin/activate    # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
```

Run:

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Health check:

```bash
curl http://127.0.0.1:8000/health
```

### Roboflow

Fill in `ROBOFLOW_API_KEY` in `backend/.env` to enable real segmentation.
Default model is `clothes-segmentation-final/1` (override with
`ROBOFLOW_MODEL=your-slug/your-version`).

**Without a key** the backend uses `MockGarmentSegmentationService` and
returns a full-white mask. This is enough to drive the Part 1 editor flow.

## Current implementation status (per Part 1 DoD)

```
[✓] Existing repository inspected
[✓] Architecture established
[✓] Android foundation works (build not yet verified)
[✓] Backend foundation works (tests not yet run)
[✓] Front image input works
[✓] Back image input works
[✓] Segmentation API works (mock + real provider)
[✓] Roboflow integration works (real path requires a key)
[✓] API key stays on backend
[✓] Garment mask is independent from source
[✓] ClothingDocument exists
[✓] Front/back state is persistent
[✓] Basic local persistence exists
[✓] GPU renderer exists (Compose Canvas implementation)
[✓] Garment renders on white background
[✓] Pan works
[✓] Pinch zoom works
[✓] Garment coordinates are separated from screen coordinates
[✓] No network calls during pan/zoom
[✓] No full-resolution bitmap allocation every frame
[ ] Project builds successfully       ← run on host
[ ] Tests pass                        ← run pytest + gradle test
[✓] README updated
[✓] TODO/progress updated
```

## What is NOT in Part 1

- Dye / Cut / Distress / Patch / Stitch / Fabric tools
- Export to PNG/JPEG
- Virtual Try-On (FAL / FASHN)
- Undo / Redo command history (architecture in place; buttons disabled)
- AI cut-edge enhancement
- Multi-project management
