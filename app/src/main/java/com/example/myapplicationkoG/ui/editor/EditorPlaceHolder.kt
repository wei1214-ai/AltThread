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
import android.widget.Toast
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
        // 仅 UI 样子，不做真实保存
        Toast.makeText(context, "Save UI preview", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Design Space", fontWeight = FontWeight.Bold, color = MidnightBlue) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MidnightBlue)
                    }
                },
                actions = {
                    TextButton(onClick = { /* TODO undo */ }) { Text("Undo", color = MidnightBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                    TextButton(onClick = { /* TODO redo */ }) { Text("Redo", color = MidnightBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                    TextButton(onClick = { saveCurrent() }) { Text("Save", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                }
            )
        },
        bottomBar = {
            // Bottom bar with 3 circles - placeholder for future tools
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
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (idx == 1) Cyan else Color(0xFFF0F0F0))
                                .border(1.dp, if (idx == 1) MidnightBlue else Color(0xFFE0E0E0), CircleShape)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                color = if (idx == 1) MidnightBlue else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFAFAFA))
                .padding(16.dp)
        ) {
            // Large centered garment image - enlarged to fit with pinch zoom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
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
                        AsyncImage(
                            model = currentPath,
                            contentDescription = currentSide.name,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                        )
                    }
                } else {
                    Text("No image", color = Color(0xFF999999))
                }
                // Side label chip on top
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightBlue)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(currentSide.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Bar tightly attached to edge, centerVertically - wider with stacked text to avoid squeeze
            if (currentSide == GarmentSideId.FRONT && state.backCutoutPath != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(width = 48.dp, height = 110.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                        .background(Cyan)
                        .border(1.dp, MidnightBlue, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                        .clickable {
                            scale = 1f; offset = Offset.Zero
                            currentSide = GarmentSideId.BACK
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("Back", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(">", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            if (currentSide == GarmentSideId.BACK && state.frontCutoutPath != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(width = 48.dp, height = 110.dp)
                        .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                        .background(Cyan)
                        .border(1.dp, MidnightBlue, RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                        .clickable {
                            scale = 1f; offset = Offset.Zero
                            currentSide = GarmentSideId.FRONT
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("<", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Front", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
