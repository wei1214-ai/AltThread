package com.example.myapplicationkoG.ui.editor

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.myapplicationkoG.ui.garmentinput.GarmentInputViewModel

/**
 * Temporary preview after [GarmentInputScreen] runs YOLO + SAM 2.1.
 * Shows the two cutouts so we can verify the AI actually worked before the
 * real editor is rebuilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorPlaceHolder(
    viewModel: GarmentInputViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Design Space", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFAFAFA))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "AI cutout result",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1B1B1B),
            )
            Text(
                "FRONT and BACK have been isolated by on-device YOLO + SAM 2.1.",
                fontSize = 13.sp,
                color = Color(0xFF666666),
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CutoutTile("FRONT", state.frontCutoutPath)
                CutoutTile("BACK", state.backCutoutPath)
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1B1B), contentColor = Color.White),
            ) { Text("Back to Design Space", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun CutoutTile(label: String, path: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            if (path != null) {
                AsyncImage(model = path, contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                Text("missing", color = Color(0xFF999999))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1B))
    }
}