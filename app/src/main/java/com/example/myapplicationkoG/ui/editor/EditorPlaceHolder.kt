package com.example.myapplicationkoG.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
    var scale by remember { mutableFloatStateOf(1f) }
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

    // Dip Dye state, only active when tool 1 circle is pressed
    var selectedTool by remember { mutableStateOf<Int?>(null) }
    var dyeColor by remember { mutableStateOf(Color(0xFF1A237E)) }
    var dyeHeight by remember { mutableFloatStateOf(0.45f) }
    var dyeStrength by remember { mutableFloatStateOf(0.55f) }
    var dyedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var isDyeing by remember { mutableStateOf(false) }
    val dyeColors = listOf(
        Color(0xFF1A237E),
        Color(0xFF000000),
        Color(0xFFB71C1C),
        Color(0xFF00695C),
        Color(0xFF4A148C)
    )

    LaunchedEffect(currentPath, selectedTool, dyeColor, dyeHeight, dyeStrength) {
        if (selectedTool != 0 || currentPath == null) {
            dyedImage = null
            isDyeing = false
            return@LaunchedEffect
        }
        isDyeing = true
        val path = currentPath
        val color = dyeColor
        val heightRatio = dyeHeight
        val strength = dyeStrength
        val result = withContext(Dispatchers.Default) {
            runCatching { makeDyedBitmap(path, color, heightRatio, strength) }.getOrNull()
        }
        dyedImage = result?.asImageBitmap()
        isDyeing = false
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
                },
                actions = {
                    TopBarActionButton(iconRes = R.drawable.undo, contentDesc = "Undo", onClick = { /* TODO undo */ })
                    TopBarActionButton(iconRes = R.drawable.redo, contentDesc = "Redo", onClick = { /* TODO redo */ })
                    TopBarActionButton(iconRes = R.drawable.save, contentDesc = "Save", onClick = { saveCurrent() })
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
                    repeat(3) { idx ->
                        val isSelected = selectedTool == idx
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Cyan else Color(0xFFF0F0F0))
                                .border(1.dp, if (isSelected) MidnightBlue else Color(0xFFE0E0E0), CircleShape)
                                .clickable {
                                    selectedTool = if (isSelected) null else idx
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                color = if (isSelected) MidnightBlue else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
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
            Text(
                "Design Space",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MidnightBlue
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Pinch to zoom • Tap 1 for Dip Dye",
                fontSize = 13.sp,
                color = Color(0xFF666666),
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF4F6F6))
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
                            if (dyedImage != null && selectedTool == 0) {
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
                            if (isDyeing && selectedTool == 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xCC000000))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Dyeing...", color = Color.White, fontSize = 12.sp)
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
                    if (currentSide == GarmentSideId.FRONT && state.backCutoutPath != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
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
                                    scale = 1f; offset = Offset.Zero
                                    currentSide = GarmentSideId.BACK
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("View Back", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Icon(
                                    painter = painterResource(id = R.drawable.arrowright),
                                    contentDescription = "Go to back",
                                    tint = MidnightBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    if (currentSide == GarmentSideId.BACK && state.frontCutoutPath != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
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
                                    scale = 1f; offset = Offset.Zero
                                    currentSide = GarmentSideId.FRONT
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.arrowleft),
                                    contentDescription = "Go to front",
                                    tint = MidnightBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("View Front", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                if (selectedTool == 0) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 56.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("Dip Dye", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            dyeColors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Dip height", color = Color.Gray, fontSize = 12.sp)
                        Slider(
                            value = dyeHeight,
                            onValueChange = { dyeHeight = it },
                            valueRange = 0.2f..0.85f
                        )
                        Text("Strength", color = Color.Gray, fontSize = 12.sp)
                        Slider(
                            value = dyeStrength,
                            onValueChange = { dyeStrength = it },
                            valueRange = 0.2f..0.85f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarActionButton(
    @DrawableRes iconRes: Int,
    contentDesc: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Cyan)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDesc,
            tint = MidnightBlue,
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun makeDyedBitmap(
    path: String,
    dye: Color,
    heightRatio: Float,
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
    val start = ((1f - heightRatio).coerceIn(0f, 1f)) * h
    for (y in 0 until h) {
        val t = ((y - start) / (h - start).coerceAtLeast(1f)).coerceIn(0f, 1f)
        // smoothstep for soft dip edge, avoids harsh line
        val feather = t * t * (3f - 2f * t)
        if (feather <= 0f) continue
        val k = (feather * strength * 0.85f).coerceIn(0f, 1f)
        for (x in 0 until w) {
            val i = y * w + x
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
    }
    out.setPixels(pixels, 0, w, 0, 0, w, h)
    if (out !== src) runCatching { if (!src.isRecycled) src.recycle() }
    return out
}
