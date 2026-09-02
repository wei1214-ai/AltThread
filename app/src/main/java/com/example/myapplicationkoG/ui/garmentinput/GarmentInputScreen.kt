package com.example.myapplicationkoG.ui.garmentinput

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplicationkoG.domain.model.GarmentSideId

@Composable
fun GarmentInputScreen(
    onOpenEditor: () -> Unit,
    onBack: () -> Unit,
    viewModel: GarmentInputViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val pickFront = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onPickedImage(GarmentSideId.FRONT, it) }
    }
    val pickBack = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onPickedImage(GarmentSideId.BACK, it) }
    }

    val ready = state.frontCutoutPath != null && state.backCutoutPath != null && !state.isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Text("New Design Space", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B1B1B))
        Spacer(Modifier.height(6.dp))
        Text(
            "Upload a clear FRONT photo, then a BACK photo. Lay the garment flat so the AI can isolate it cleanly.",
            fontSize = 14.sp,
            color = Color(0xFF666666),
        )
        Spacer(Modifier.height(24.dp))

        PickerBox("FRONT", state.frontCutoutPath, !state.isLoading) { pickFront.launch("image/*") }
        Spacer(Modifier.height(12.dp))
        PickerBox("BACK", state.backCutoutPath, !state.isLoading) { pickBack.launch("image/*") }

        if (state.isLoading) {
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF1B1B1B))
                Spacer(Modifier.size(12.dp))
                Text("Running on-device AI (YOLO + SAM 2.1)…", fontSize = 13.sp, color = Color(0xFF1B1B1B))
            }
        }

        state.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE), contentColor = Color(0xFF1B1B1B)),
            ) { Text("Back", fontWeight = FontWeight.SemiBold) }

            Button(
                onClick = onOpenEditor,
                enabled = ready,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1B1B), contentColor = Color.White),
            ) { Text("Open Editor", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun PickerBox(label: String, cutoutPath: String?, enabled: Boolean, onPick: () -> Unit) {
    val hasImage = cutoutPath != null
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (hasImage) Color(0xFFE7F4EE) else Color.White)
            .border(
                width = 1.5.dp,
                color = if (hasImage) Color(0xFF2E7D5B) else Color(0xFFCCCCCC),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(enabled = enabled, onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        if (hasImage) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = cutoutPath,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEEEEEE)),
                )
                Spacer(Modifier.height(8.dp))
                Text(label, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D5B))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(28.dp), tint = Color(0xFF1B1B1B))
                Spacer(Modifier.height(6.dp))
                Text("Select $label photo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1B1B1B))
            }
        }
    }
}