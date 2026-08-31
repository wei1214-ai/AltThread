package com.example.myapplicationkoG.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplicationkoG.domain.model.GarmentSideId
import com.example.myapplicationkoG.domain.model.Point
import com.example.myapplicationkoG.editor.EditorTool
import com.example.myapplicationkoG.editor.EditorViewModel
import com.example.myapplicationkoG.rendering.BitmapCache
import com.example.myapplicationkoG.rendering.ComposeCanvasGarmentRenderer
import com.example.myapplicationkoG.rendering.GarmentRenderer
import com.example.myapplicationkoG.rendering.RenderState
import com.example.myapplicationkoG.ui.gesture.viewportGestures

/**
 * Top-level editor screen. Pan + zoom happen locally; the GPU renderer
 * is held behind an abstraction so future shaders can drop in.
 *
 * Part 2 P1: stroke tools (Dye + Cut) wire through
 *   beginStroke -> extendStroke -> endStroke
 * with Undo/Redo enabled. Tool options panel exposes colour, brush radius,
 * opacity, intensity and cut width.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cache = remember { BitmapCache() }
    val renderer: GarmentRenderer = remember { ComposeCanvasGarmentRenderer() }

    val side = state.activeSideData

    // Cache holds the decoded bitmaps. Pan/zoom reads from snapshot (no IO, no decode).
    var source by remember { mutableStateOf<ImageBitmap?>(null) }
    var mask by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(side?.sourceImage?.uri, side?.garmentMask?.uri) {
        if (side != null) {
            val (s, m) = cache.loadFor(side)
            source = s
            mask = m
        } else {
            source = null
            mask = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = state.canUndo
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = state.canRedo
                    ) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            SideSwitcher(
                active = state.activeSide,
                onChange = { viewModel.switchSide(it) }
            )

            // Map a screen point back into garment coordinates for stroke input.
            val sourceW = source?.width?.toFloat() ?: 1f
            val sourceH = source?.height?.toFloat() ?: 1f

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }
                val strokeTool = state.selectedTool
                var canvasPx by remember { mutableStateOf(IntSize.Zero) }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { canvasPx = it }
                        .viewportGestures(
                            current = { state.viewport },
                            onChange = { viewModel.applyViewport(it) }
                        )
                        .pointerInput(strokeTool, sourceW, sourceH, state.viewport, canvasPx) {
                            // Only intercept drag for active stroke tools. Pan/zoom is
                            // handled inside viewportGestures; using detectDragGestures
                            // would steal those gestures, so we rely on the touch-slop
                            // ordering: viewportGestures wins on multi-finger, drag wins
                            // on single-finger stroke.
                            if (strokeTool != EditorTool.DYE && strokeTool != EditorTool.CUT) {
                                return@pointerInput
                            }
                            val vp = state.viewport
                            detectDragGestures(
                                onDragStart = { screenOffset ->
                                    val garment = screenToGarment(
                                        screen = screenOffset,
                                        sourceW = sourceW,
                                        sourceH = sourceH,
                                        viewport = vp,
                                        canvasW = canvasPx.width.toFloat(),
                                        canvasH = canvasPx.height.toFloat()
                                    ) ?: return@detectDragGestures
                                    viewModel.beginStroke(garment)
                                },
                                onDrag = { change, _ ->
                                    val garment = screenToGarment(
                                        screen = change.position,
                                        sourceW = sourceW,
                                        sourceH = sourceH,
                                        viewport = vp,
                                        canvasW = canvasPx.width.toFloat(),
                                        canvasH = canvasPx.height.toFloat()
                                    ) ?: return@detectDragGestures
                                    viewModel.extendStroke(garment)
                                },
                                onDragEnd = {
                                    viewModel.endStroke()
                                },
                                onDragCancel = {
                                    viewModel.endStroke()
                                }
                            )
                        }
                ) {
                    if (side != null) {
                        val rs = RenderState(
                            side = side,
                            viewport = state.viewport,
                            source = source,
                            mask = mask,
                            layers = side.layers
                        )
                        renderer.draw(this, rs)
                    }
                }
                state.errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                    )
                }
            }

            ToolOptionsPanel(
                state = state,
                onColor = viewModel::setToolColor,
                onBrushRadius = viewModel::setBrushRadius,
                onBrushOpacity = viewModel::setBrushOpacity,
                onDyeIntensity = viewModel::setDyeIntensity,
                onCutWidth = viewModel::setCutWidth
            )

            ToolBar(
                selected = state.selectedTool,
                onSelect = { viewModel.setTool(it) }
            )
        }
    }
}

@Composable
private fun SideSwitcher(active: GarmentSideId, onChange: (GarmentSideId) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            selected = active == GarmentSideId.FRONT,
            onClick = { onChange(GarmentSideId.FRONT) },
            label = { Text("FRONT") }
        )
        FilterChip(
            selected = active == GarmentSideId.BACK,
            onClick = { onChange(GarmentSideId.BACK) },
            label = { Text("BACK") }
        )
    }
}

@Composable
private fun ToolOptionsPanel(
    state: com.example.myapplicationkoG.editor.EditorState,
    onColor: (Int) -> Unit,
    onBrushRadius: (Float) -> Unit,
    onBrushOpacity: (Float) -> Unit,
    onDyeIntensity: (Float) -> Unit,
    onCutWidth: (Float) -> Unit
) {
    val isDye = state.selectedTool == EditorTool.DYE
    val isCut = state.selectedTool == EditorTool.CUT
    if (!isDye && !isCut) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (isDye) {
            ColorRow(
                selectedArgb = state.toolColorArgb,
                onColor = onColor
            )
            LabeledSlider(
                label = "Brush size",
                value = state.brushRadius,
                valueRange = 8f..160f,
                onChange = onBrushRadius
            )
            LabeledSlider(
                label = "Opacity",
                value = state.brushOpacity,
                valueRange = 0f..1f,
                onChange = onBrushOpacity
            )
            LabeledSlider(
                label = "Intensity",
                value = state.dyeIntensity,
                valueRange = 0f..1f,
                onChange = onDyeIntensity
            )
        } else {
            LabeledSlider(
                label = "Cut width",
                value = state.cutWidth,
                valueRange = 4f..80f,
                onChange = onCutWidth
            )
        }
    }
}

@Composable
private fun ColorRow(selectedArgb: Int, onColor: (Int) -> Unit) {
    val swatches = listOf(
        0xFFD32F2F.toInt(), // red
        0xFF1976D2.toInt(), // blue
        0xFF388E3C.toInt(), // green
        0xFFFBC02D.toInt(), // yellow
        0xFF8E24AA.toInt(), // purple
        0xFF000000.toInt()  // black
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        swatches.forEach { argb ->
            val isSel = argb == selectedArgb
            Box(
                modifier = Modifier
                    .size(if (isSel) 30.dp else 24.dp)
                    .background(Color(argb), CircleShape)
                    .clickable { onColor(argb) }
            )
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ${"%.2f".format(value)}",
            fontSize = 11.sp,
            color = Color.DarkGray
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ToolBar(selected: EditorTool, onSelect: (EditorTool) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            EditorTool.DYE,
            EditorTool.CUT,
            EditorTool.DISTRESS,
            EditorTool.PATCH,
            EditorTool.STITCH,
            EditorTool.FABRIC
        ).forEach { tool ->
            ToolChip(
                label = tool.name,
                selected = selected == tool,
                onClick = { onSelect(tool) }
            )
        }
    }
}

@Composable
private fun ToolChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (selected) Color(0xFF00ACC1) else Color(0xFFE0E0E0),
                        shape = CircleShape
                    )
            )
        }
        Text(label, fontSize = 10.sp, color = Color.DarkGray)
    }
}

/**
 * Inverse of the renderer's fit-contain + viewport transform. Returns the
 * garment-space point that corresponds to [screen], or null if the screen
 * point is outside the garment bounds.
 */
private fun screenToGarment(
    screen: Offset,
    sourceW: Float,
    sourceH: Float,
    viewport: com.example.myapplicationkoG.domain.model.Viewport,
    canvasW: Float,
    canvasH: Float
): Point? {
    if (sourceW <= 0f || sourceH <= 0f) return null
    val fitScale = minOf(canvasW / sourceW, canvasH / sourceH)
    val fitWidth = sourceW * fitScale
    val fitHeight = sourceH * fitScale
    val baseLeft = (canvasW - fitWidth) / 2f
    val baseTop = (canvasH - fitHeight) / 2f
    val left = baseLeft + viewport.translationX
    val top = baseTop + viewport.translationY
    val gx = (screen.x - left) / (fitScale * viewport.scale)
    val gy = (screen.y - top) / (fitScale * viewport.scale)
    if (gx < 0f || gy < 0f || gx > sourceW || gy > sourceH) return null
    return Point(gx, gy)
}