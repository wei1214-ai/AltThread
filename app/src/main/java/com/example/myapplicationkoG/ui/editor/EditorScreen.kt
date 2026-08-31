package com.example.myapplicationkoG.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplicationkoG.domain.model.GarmentSideId
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
                    IconButton(onClick = { /* undo stack — Part 2 */ }, enabled = false) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { /* redo stack — Part 2 */ }, enabled = false) {
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
            Box(
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
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .viewportGestures(
                            current = { state.viewport },
                            onChange = { viewModel.applyViewport(it) }
                        )
                ) {
                    if (side != null) {
                        val rs = RenderState(
                            side = side,
                            viewport = state.viewport,
                            source = source,
                            mask = mask
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
