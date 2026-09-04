package com.example.myapplicationkoG.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.toArgb
import com.example.myapplicationkoG.R
import com.example.myapplicationkoG.DesignRepository
import com.example.myapplicationkoG.SavedButton
import com.example.myapplicationkoG.SavedDye
import com.example.myapplicationkoG.domain.model.GarmentSideId
import com.example.myapplicationkoG.ui.garmentinput.GarmentInputViewModel
import com.example.myapplicationkoG.ui.theme.Cyan
import com.example.myapplicationkoG.ui.theme.MidnightBlue
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class DyeState(val color: Color, val strength: Float)
data class PlacedButton(val pos: Offset, val scale: Float, val color: Color)
data class EditorSnapshot(
    val dye: Map<GarmentSideId, DyeState>,
    val buttons: Map<GarmentSideId, List<PlacedButton>>
)

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
    val scope = rememberCoroutineScope()

    var saveDialogVisible by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var showAiChat by remember { mutableStateOf(false) }

    fun saveCurrent() {
        saveDialogVisible = true
    }

    // Tool + dye editing state
    var selectedTool by remember { mutableStateOf<Int?>(null) }
    var dyeColor by remember { mutableStateOf(Color(0xFF1A237E)) }
    var dyeStrength by remember { mutableFloatStateOf(0.55f) }
    var dyeMap by remember { mutableStateOf(mapOf<GarmentSideId, DyeState>()) }
    var dyedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    // Patch buttons, per side
    var buttonMap by remember { mutableStateOf(mapOf<GarmentSideId, List<PlacedButton>>()) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var buttonImg by remember { mutableStateOf<ImageBitmap?>(null) }
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

    var dyeActive by remember { mutableStateOf(false) }
    var buttonColor by remember { mutableStateOf(Color.White) }
    var buttonScale by remember { mutableFloatStateOf(1f) }

    // Load saved design if opened from Continue screen
    LaunchedEffect(Unit) {
        DesignSession.consume()?.let { loaded ->
            dyeMap = loaded.dye
            buttonMap = loaded.buttons
            loaded.dye[GarmentSideId.FRONT]?.let { dyeActive = true; dyeColor = it.color; dyeStrength = it.strength }
        }
    }

    fun currentSnapshot() = EditorSnapshot(dyeMap, buttonMap)
    fun applySnapshot(s: EditorSnapshot) {
        dyeMap = s.dye
        buttonMap = s.buttons
        if (selectedTool == 0) {
            val d = s.dye[currentSide]
            dyeActive = d != null
            d?.let { dyeColor = it.color; dyeStrength = it.strength }
        }
    }
    fun pushHistory() {
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
            val live = if (dyeActive) DyeState(dyeColor, dyeStrength) else null
            if (live != dyeMap[currentSide]) {
                pushHistory()
                dyeMap = if (live != null) dyeMap + (currentSide to live) else dyeMap - currentSide
            }
        }
        if (selectedTool == 0 && prevTool != 0) {
            val d = dyeMap[currentSide]
            dyeActive = d != null
            d?.let { dyeColor = it.color; dyeStrength = it.strength }
        }
        prevTool = selectedTool
    }

    // Dye preview while editing, committed dye otherwise
    val activeDye: DyeState? = if (selectedTool == 0) {
        if (dyeActive) DyeState(dyeColor, dyeStrength) else null
    } else dyeMap[currentSide]
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

    // Button texture: black background keyed out once
    LaunchedEffect(Unit) {
        buttonImg = withContext(Dispatchers.Default) {
            runCatching {
                BitmapFactory.decodeResource(context.resources, R.drawable.patch)
                    ?.let { keyOutBlack(it).asImageBitmap() }
            }.getOrNull()
        }
    }
    val curButtons = buttonMap[currentSide].orEmpty()

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
                    val toolIcons = listOf(R.drawable.dye, R.drawable.patch)
                    val toolNames = listOf("Dye", "Patch")
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MidnightBlue)
                                .clickable { showAiChat = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✦",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Box(
                            modifier = Modifier
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
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .transformable(state = transformState)
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
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .onSizeChanged { overlaySize = it }
                                ) {
                                    val bwPx = overlaySize.width.toFloat().coerceAtLeast(1f)
                                    val bhPx = overlaySize.height.toFloat().coerceAtLeast(1f)
                                    fun toGarmentPixel(fx: Float, fy: Float): Boolean {
                                        val path = currentPath ?: return false
                                        val bmp = hitBitmap(path) ?: return false
                                        val ox = fx * bwPx
                                        val oy = fy * bhPx
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
                                        if (bx !in 0 until bmp.width || by !in 0 until bmp.height) return false
                                        val p = bmp.getPixel(bx, by)
                                        val r = (p shr 16) and 0xFF
                                        val g = (p shr 8) and 0xFF
                                        val b = p and 0xFF
                                        return r < 250 || g < 250 || b < 250
                                    }
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(
                                                if (selectedTool == 1) Modifier.pointerInput(selectedTool, currentPath, overlaySize) {
                                                    detectTapGestures(
                                                        onTap = { pos ->
                                                            val fx = (pos.x / bwPx).coerceIn(0f, 1f)
                                                            val fy = (pos.y / bhPx).coerceIn(0f, 1f)
                                                            if (!toGarmentPixel(fx, fy)) {
                                                                Toast.makeText(context, "Please place buttons on the garment", Toast.LENGTH_SHORT).show()
                                                                return@detectTapGestures
                                                            }
                                                            if (curButtons.size >= 6) {
                                                                Toast.makeText(context, "Max 6 buttons per side", Toast.LENGTH_SHORT).show()
                                                                return@detectTapGestures
                                                            }
                                                            pushHistory()
                                                            buttonMap = buttonMap + (currentSide to (curButtons + PlacedButton(Offset(fx, fy), buttonScale, buttonColor)))
                                                        }
                                                    )
                                                } else Modifier
                                            )
                                    ) {
                                        val b = buttonImg
                                        if (b != null) {
                                            val base = 0.05f * minOf(size.width, size.height)
                                            val aspect = b.width.toFloat() / b.height.toFloat().coerceAtLeast(1f)
                                            curButtons.forEach { btn ->
                                                // Size bar range is the clamp: 0.5x-2.0x of base
                                                val h = (base * btn.scale).coerceIn(base * 0.5f, base * 2f)
                                                val wdt = h * aspect
                                                val cx = btn.pos.x * size.width
                                                val cy = btn.pos.y * size.height
                                                drawImage(
                                                    b,
                                                    dstOffset = IntOffset((cx - wdt / 2).toInt(), (cy - h / 2).toInt()),
                                                    dstSize = IntSize(wdt.toInt(), h.toInt()),
                                                    colorFilter = if (btn.color == Color.White) {
                                                        null
                                                    } else {
                                                        ColorFilter.tint(btn.color, BlendMode.Multiply)
                                                    }
                                                )
                                            }
                                        }
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
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (currentSide == GarmentSideId.FRONT && state.backCutoutPath != null) {
                        Box(
                            modifier = Modifier
                                .size(width = 220.dp, height = 44.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Cyan)
                                .clickable {
                                    scale = 1.35f; offset = Offset.Zero
                                    currentSide = GarmentSideId.BACK
                                    dyeMap[GarmentSideId.BACK]?.let { dyeColor = it.color; dyeStrength = it.strength }
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
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                    if (currentSide == GarmentSideId.BACK && state.frontCutoutPath != null) {
                        Box(
                            modifier = Modifier
                                .size(width = 220.dp, height = 44.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Cyan)
                                .clickable {
                                    scale = 1.35f; offset = Offset.Zero
                                    currentSide = GarmentSideId.FRONT
                                    dyeMap[GarmentSideId.FRONT]?.let { dyeColor = it.color; dyeStrength = it.strength }
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
                                    textAlign = TextAlign.Center
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
                                            if (dyeActive && dyeColor == c) MidnightBlue else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable {
                                            if (dyeActive && dyeColor == c) {
                                                dyeActive = false
                                            } else {
                                                dyeColor = c
                                                dyeActive = true
                                            }
                                        }
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
                        Text("Patch", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.patch),
                                contentDescription = "Button",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0F0F0))
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                            Text("Button", color = MidnightBlue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val buttonPalette = listOf(Color.White) + dyeColors
                            buttonPalette.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            2.dp,
                                            if (buttonColor == c) MidnightBlue else Color(0xFFE0E0E0),
                                            CircleShape
                                        )
                                        .clickable { buttonColor = c }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Size", color = Color.Gray, fontSize = 11.sp)
                        Slider(
                            value = buttonScale,
                            onValueChange = { buttonScale = it.coerceIn(0.5f, 2f) },
                            valueRange = 0.5f..2f,
                            modifier = Modifier.height(24.dp)
                        )
                        Text("Tap garment to place, max 6 per side", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (saveDialogVisible) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) saveDialogVisible = false },
            title = { Text("Save Design", color = MidnightBlue, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Name your design", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        singleLine = true,
                        placeholder = { Text("My design") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            try {
                                val repo = DesignRepository()
                                val frontPath = state.frontCutoutPath
                                val backPath = state.backCutoutPath
                                if (frontPath == null || backPath == null) {
                                    Toast.makeText(context, "No garment image to save", Toast.LENGTH_SHORT).show()
                                    isSaving = false
                                    saveDialogVisible = false
                                    return@launch
                                }
                                val frontFile = File(frontPath)
                                val backFile = File(backPath)
                                val dyeSaved = dyeMap.mapValues { SavedDye(it.value.color.toArgb(), it.value.strength) }
                                val btnSaved = buttonMap.mapValues { entry -> entry.value.map { SavedButton(it.pos.x, it.pos.y, it.scale, it.color.toArgb()) } }
                                repo.saveDesign(
                                    name = saveName.ifBlank { "Untitled design" },
                                    frontFile = frontFile,
                                    backFile = backFile,
                                    dye = dyeSaved,
                                    buttons = btnSaved
                                )
                                Toast.makeText(context, "Design saved!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSaving = false
                                saveDialogVisible = false
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MidnightBlue, strokeWidth = 2.dp)
                    } else {
                        Text("Save", color = MidnightBlue)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { saveDialogVisible = false }, enabled = !isSaving) {
                    Text("Cancel")
                }
            }
        )
    }

    AiChatDrawer(
        visible = showAiChat,
        onDismiss = { showAiChat = false }
    )
}

private val hitCache = mutableMapOf<String, android.graphics.Bitmap>()

private fun hitBitmap(path: String): android.graphics.Bitmap? = synchronized(hitCache) {
    hitCache[path]?.takeIf { !it.isRecycled }?.let { return it }
    if (hitCache.size > 4) {
        hitCache.values.forEach { runCatching { if (!it.isRecycled) it.recycle() } }
        hitCache.clear()
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
    val bmp = runCatching { BitmapFactory.decodeFile(path, opts) }.getOrNull()
    if (bmp != null) hitCache[path] = bmp
    bmp
}

private fun keyOutBlack(src: android.graphics.Bitmap): android.graphics.Bitmap {
    // Button photo has a black background: make dark pixels transparent.
    val w = src.width
    val h = src.height
    val out = src.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
    val pixels = IntArray(w * h)
    out.getPixels(pixels, 0, w, 0, 0, w, h)
    for (i in pixels.indices) {
        val p = pixels[i]
        val m = maxOf((p shr 16) and 0xFF, (p shr 8) and 0xFF, p and 0xFF)
        val a = ((m - 25f) / (60f - 25f)).coerceIn(0f, 1f)
        pixels[i] = ((a * 255).toInt() shl 24) or (p and 0x00FFFFFF)
    }
    out.setPixels(pixels, 0, w, 0, 0, w, h)
    return out
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
