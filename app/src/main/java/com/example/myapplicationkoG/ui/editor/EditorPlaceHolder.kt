package com.example.myapplicationkoG.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.annotation.DrawableRes
import androidx.compose.material3.TopAppBarDefaults
import com.example.myapplicationkoG.R
import com.example.myapplicationkoG.domain.model.GarmentSideId
import com.example.myapplicationkoG.ui.garmentinput.GarmentInputViewModel
import com.example.myapplicationkoG.ui.theme.Cyan
import com.example.myapplicationkoG.ui.theme.MidnightBlue

enum class StitchStyle(val label: String) { RUNNING("平针"), BACK("回针"), ZIGZAG("之字") }

data class StitchStroke(
    val points: List<Offset>,
    val style: StitchStyle,
    val color: Color
)

data class DyeState(val color: Color, val strength: Float)
data class EditorSnapshot(val dye: DyeState?, val strokes: List<StitchStroke>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorPlaceHolder(
    viewModel: GarmentInputViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var currentSide by remember { mutableStateOf(GarmentSideId.FRONT) }
    val currentPath = when (currentSide) {
        GarmentSideId.FRONT -> state.frontCutoutPath
        GarmentSideId.BACK -> state.backCutoutPath
    }

    // Zoom state for garment
    var scale by remember { mutableFloatStateOf(1.35f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.8f, 4f)
        offset += offsetChange
    }
    val context = LocalContext.current
    fun saveCurrent() {
        // UI preview only, no real save
        Toast.makeText(context, "Save UI preview", Toast.LENGTH_SHORT).show()
    }

    // Tool + dye editing state
    var selectedTool by remember { mutableStateOf<Int?>(null) }
    var dyeColor by remember { mutableStateOf(Color(0xFF1A237E)) }
    var dyeStrength by remember { mutableFloatStateOf(0.55f) }
    var dyeCommitted by remember { mutableStateOf<DyeState?>(null) }
    var dyedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    // Stitch state
    var stitchStyle by remember { mutableStateOf(StitchStyle.RUNNING) }
    var threadColor by remember { mutableStateOf(Color(0xFFB71C1C)) }
    var strokes by remember { mutableStateOf(listOf<StitchStroke>()) }
    var livePoints by remember { mutableStateOf(listOf<Offset>()) }
    var garmentBmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    // Undo/redo stacks, each entry is one committed step
    var undoStack by remember { mutableStateOf(listOf<EditorSnapshot>()) }
    var redoStack by remember { mutableStateOf(listOf<EditorSnapshot>()) }
    var prevTool by remember { mutableStateOf<Int?>(null) }
    val dyeColors = listOf(
        Color(0xFF1A237E),
        Color(0xFF000000),
        Color(0xFFB71C1C),
        Color(0xFF00695C),
        Color(0xFF4A148C),
        Color(0xFFE65100),
        Color(0xFFEC407A),
        Color(0xFF33691E)
    )

    fun currentSnapshot() = EditorSnapshot(dyeCommitted, strokes)
    fun applySnapshot(s: EditorSnapshot) {
        dyeCommitted = s.dye
        strokes = s.strokes
        s.dye?.let { dyeColor = it.color; dyeStrength = it.strength }
    }
    fun commit() {
        undoStack = undoStack + currentSnapshot()
        redoStack = emptyList()
    }
    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack = redoStack + currentSnapshot()
        val prev = undoStack.last()
        undoStack = undoStack.dropLast(1)
        applySnapshot(prev)
    }
    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack = undoStack + currentSnapshot()
        val next = redoStack.last()
        redoStack = redoStack.dropLast(1)
        applySnapshot(next)
    }

    // Leaving dye mode commits one step; entering restores committed values
    LaunchedEffect(selectedTool) {
        if (prevTool == 0 && selectedTool != 0) {
            val live = DyeState(dyeColor, dyeStrength)
            if (live != dyeCommitted) {
                dyeCommitted = live
                commit()
            }
        }
        if (selectedTool == 0 && prevTool != 0) {
            dyeCommitted?.let { dyeColor = it.color; dyeStrength = it.strength }
        }
        prevTool = selectedTool
    }

    // Dye preview while editing, committed dye otherwise
    val activeDye: DyeState? = if (selectedTool == 0) DyeState(dyeColor, dyeStrength) else dyeCommitted
    LaunchedEffect(currentPath, activeDye) {
        val path = currentPath
        val dye = activeDye
        if (path == null || dye == null) {
            dyedImage = null
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.Default) {
            runCatching { makeDyedBitmap(path, dye.color, dye.strength) }.getOrNull()
        }
        dyedImage = result?.asImageBitmap()
    }

    // Small garment bitmap to test whether a stitch point lands on fabric
    LaunchedEffect(currentPath) {
        garmentBmp?.let { runCatching { if (!it.isRecycled) it.recycle() } }
        garmentBmp = null
        val path = currentPath ?: return@LaunchedEffect
        garmentBmp = withContext(Dispatchers.Default) {
            runCatching {
                BitmapFactory.decodeFile(path)?.let { src ->
                    val small = Bitmap.createScaledBitmap(src, 512, 512, true)
                    if (small !== src) runCatching { if (!src.isRecycled) src.recycle() }
                    small
                }
            }.getOrNull()
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Design Space", fontWeight = FontWeight.Bold, color = MidnightBlue) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MidnightBlue)
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(24.dp))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val toolIcons = listOf(R.drawable.dye, R.drawable.stitch, R.drawable.patch)
                    val toolNames = listOf("Dye", "Stitch", "Patch")
                    toolIcons.forEachIndexed { idx, iconRes ->
                        val isSelected = selectedTool == idx
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Cyan else Color(0xFFF0F0F0))
                                    .clickable {
                                        selectedTool = if (isSelected) null else idx
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = toolNames[idx],
                                    tint = if (isSelected) MidnightBlue else Color.Gray,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = toolNames[idx],
                                color = if (isSelected) MidnightBlue else Color.Gray,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                    if (currentPath != null) {
                        val drawMode = selectedTool == 1
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (drawMode) Modifier else Modifier.transformable(state = transformState))
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                        ) {
                            if (dyedImage != null) {
                                Image(
                                    bitmap = dyedImage!!,
                                    contentDescription = currentSide.name,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                                )
                            } else {
                                AsyncImage(
                                    model = currentPath,
                                    contentDescription = currentSide.name,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                                )
                            }
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val density = LocalDensity.current
                                val bwPx = with(density) { maxWidth.toPx() }
                                val bhPx = with(density) { maxHeight.toPx() }
                                fun toFrac(o: Offset) = Offset(
                                    (o.x / bwPx).coerceIn(0f, 1f),
                                    (o.y / bhPx).coerceIn(0f, 1f)
                                )
                                // Map an overlay point back to garment bitmap pixels,
                                // undoing zoom/pan and image letterboxing. Null = white space.
                                fun toGarmentPixel(f: Offset): Pair<Int, Int>? {
                                    val bmp = garmentBmp
                                        ?.takeIf { !it.isRecycled } ?: return null
                                    val ox = f.x * bwPx
                                    val oy = f.y * bhPx
                                    val cx = bwPx / 2f
                                    val cy = bhPx / 2f
                                    val lx = (ox - cx - offset.x) / scale + cx
                                    val ly = (oy - cy - offset.y) / scale + cy
                                    val s = minOf(bwPx / bmp.width, bhPx / bmp.height)
                                    val dw = bmp.width * s
                                    val dh = bmp.height * s
                                    val left = (bwPx - dw) / 2f
                                    val top = (bhPx - dh) / 2f
                                    val bx = ((lx - left) / dw * bmp.width).toInt()
                                    val by = ((ly - top) / dh * bmp.height).toInt()
                                    if (bx !in 0 until bmp.width || by !in 0 until bmp.height) return null
                                    return if (((bmp.getPixel(bx, by) ushr 24) and 0xFF) > 100) {
                                        bx to by
                                    } else null
                                }
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(
                                            if (drawMode) Modifier.pointerInput(drawMode, currentPath, stitchStyle, threadColor) {
                                                detectDragGestures(
                                                    onDragStart = { livePoints = listOf(toFrac(it)) },
                                                    onDrag = { change, _ -> livePoints = livePoints + toFrac(change.position) },
                                                    onDragEnd = {
                                                        val kept = livePoints.filter { p -> toGarmentPixel(p) != null }
                                                        if (kept.size >= 2) {
                                                            strokes = strokes + StitchStroke(kept, stitchStyle, threadColor)
                                                            commit()
                                                        }
                                                        livePoints = emptyList()
                                                    },
                                                    onDragCancel = { livePoints = emptyList() }
                                                )
                                            } else Modifier
                                        )
                                ) {
                                    val all = if (livePoints.size >= 2) {
                                        strokes + StitchStroke(livePoints, stitchStyle, threadColor)
                                    } else strokes
                                    all.forEach { drawStitch(it) }
                                }
                            }
                        }
                    } else {
                        Text("No image", color = Color(0xFF999999))
                    }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Cyan)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(currentSide.name, color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 12.dp, top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                .background(Cyan)
                                .clickable { undo() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.undo),
                                    contentDescription = "Undo",
                                    tint = MidnightBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                .background(Cyan)
                                .clickable { redo() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.redo),
                                    contentDescription = "Redo",
                                    tint = MidnightBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 12.dp, top = 12.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Cyan)
                                .clickable { saveCurrent() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.save),
                                contentDescription = "Save",
                                tint = MidnightBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (currentSide == GarmentSideId.FRONT && state.backCutoutPath != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .size(width = 220.dp, height = 44.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = 0.dp,
                                            bottomEnd = 0.dp
                                        )
                                    )
                                    .background(Cyan)
                                .clickable {
                                    scale = 1.35f; offset = Offset.Zero
                                    livePoints = emptyList()
                                    currentSide = GarmentSideId.BACK
                                }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.arrowleft),
                                            contentDescription = "Go to back",
                                            tint = MidnightBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "View Back",
                                        color = MidnightBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                        if (currentSide == GarmentSideId.BACK && state.frontCutoutPath != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .size(width = 220.dp, height = 44.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = 0.dp,
                                            bottomEnd = 0.dp
                                        )
                                    )
                                    .background(Cyan)
                                .clickable {
                                    scale = 1.35f; offset = Offset.Zero
                                    livePoints = emptyList()
                                    currentSide = GarmentSideId.FRONT
                                }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Spacer(modifier = Modifier.size(32.dp))
                                    Text(
                                        text = "View Front",
                                        color = MidnightBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Box(
                                        modifier = Modifier.size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.arrowright),
                                            contentDescription = "Go to front",
                                            tint = MidnightBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedTool == 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Dye whole garment", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            dyeColors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            2.dp,
                                            if (dyeColor == c) MidnightBlue else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { dyeColor = c }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Strength", color = Color.Gray, fontSize = 11.sp)
                        Slider(
                            value = dyeStrength,
                            onValueChange = { dyeStrength = it },
                            valueRange = 0.2f..0.85f,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
                if (selectedTool == 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Stitch", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StitchStyle.entries.forEach { s ->
                                val sel = stitchStyle == s
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) Cyan else Color(0xFFF0F0F0))
                                        .clickable { stitchStyle = s }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        s.label,
                                        color = if (sel) MidnightBlue else Color.Gray,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            dyeColors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            2.dp,
                                            if (threadColor == c) MidnightBlue else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { threadColor = c }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Draw with one finger, pinch with two", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun makeDyedBitmap(
    path: String,
    dye: Color,
    strength: Float
): android.graphics.Bitmap {
    val src = BitmapFactory.decodeFile(path) ?: error("Cannot decode $path")
    val w = src.width
    val h = src.height
    val out = src.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
    val pixels = IntArray(w * h)
    out.getPixels(pixels, 0, w, 0, 0, w, h)
    val dyeR = (dye.red * 255).toInt()
    val dyeG = (dye.green * 255).toInt()
    val dyeB = (dye.blue * 255).toInt()
    // Whole-garment dye, no gradient edge
    val k = (strength * 0.85f).coerceIn(0f, 1f)
    for (i in pixels.indices) {
        val p = pixels[i]
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        // keep pure white canvas background untouched
        if (r > 242 && g > 242 && b > 242) continue
        // preserve shading: scale dye by original luminance so wrinkles stay
        val lum = (r + g + b) / 3f / 255f
        val shade = 0.35f + 0.65f * lum
        val nr = (r * (1f - k) + dyeR * shade * k).toInt().coerceIn(0, 255)
        val ng = (g * (1f - k) + dyeG * shade * k).toInt().coerceIn(0, 255)
        val nb = (b * (1f - k) + dyeB * shade * k).toInt().coerceIn(0, 255)
        pixels[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
    }
    out.setPixels(pixels, 0, w, 0, 0, w, h)
    if (out !== src) runCatching { if (!src.isRecycled) src.recycle() }
    return out
}

private fun dashFor(style: StitchStyle): FloatArray = when (style) {
    StitchStyle.RUNNING -> floatArrayOf(12f, 8f)
    StitchStyle.BACK -> floatArrayOf(8f, 4f)
    StitchStyle.ZIGZAG -> floatArrayOf(0f, 0f)
}

private fun zigzagPoints(pts: List<Offset>, step: Float = 12f, amp: Float = 5f): List<Offset> {
    if (pts.size < 2) return pts
    val cum = mutableListOf(0f)
    for (i in 1 until pts.size) cum.add(cum[i - 1] + (pts[i] - pts[i - 1]).getDistance())
    val total = cum.last()
    if (total <= 0f) return pts
    val out = mutableListOf(pts[0])
    var d = 0f
    var side = 1f
    var seg = 0
    while (d < total) {
        d += step
        val t = d.coerceAtMost(total)
        while (seg < cum.size - 2 && cum[seg + 1] < t) seg++
        val segLen = (cum[seg + 1] - cum[seg]).coerceAtLeast(1e-6f)
        val f = ((t - cum[seg]) / segLen).coerceIn(0f, 1f)
        val base = pts[seg] + (pts[seg + 1] - pts[seg]) * f
        val dir = pts[seg + 1] - pts[seg]
        val len = dir.getDistance().coerceAtLeast(1e-6f)
        val normal = Offset(-dir.y / len, dir.x / len)
        out.add(base + normal * amp * side)
        side = -side
    }
    return out
}

private fun DrawScope.drawStitch(s: StitchStroke) {
    val px = s.points.map { Offset(it.x * size.width, it.y * size.height) }
    if (px.size < 2) return
    val w = 2.5.dp.toPx()
    if (s.style == StitchStyle.ZIGZAG) {
        val zp = zigzagPoints(px)
        val path = Path().apply {
            moveTo(zp[0].x, zp[0].y)
            for (i in 1 until zp.size) lineTo(zp[i].x, zp[i].y)
        }
        drawPath(path, s.color.copy(alpha = 0.5f), style = Stroke(width = w + 1.5f))
        drawPath(path, s.color, style = Stroke(width = w))
    } else {
        val d = dashFor(s.style)
        val path = Path().apply {
            moveTo(px[0].x, px[0].y)
            for (i in 1 until px.size) lineTo(px[i].x, px[i].y)
        }
        drawPath(
            path,
            s.color.copy(alpha = 0.5f),
            style = Stroke(width = w + 1.5f, pathEffect = PathEffect.dashPathEffect(d))
        )
        drawPath(path, s.color, style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(d)))
    }
}
