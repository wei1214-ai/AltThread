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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplicationkoG.domain.model.GarmentSideId
import com.example.myapplicationkoG.editor.EditorViewModel

/**
 * Part 1 entry point for garment capture.
 * User picks FRONT then BACK, each gets uploaded to the backend, and on
 * success the editor opens.
 */
@Composable
fun GarmentInputScreen(
    onOpenEditor: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickFront = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onPickedImage(GarmentSideId.FRONT, it) }
    }
    val pickBack = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onPickedImage(GarmentSideId.BACK, it) }
    }

    val frontHasImage = state.document?.front?.sourceImage?.uri?.isNotBlank() == true
    val backHasImage = state.document?.back?.sourceImage?.uri?.isNotBlank() == true
    val canOpenEditor = frontHasImage && backHasImage && !state.isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        Text(
            text = "New Garment",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Take a clear photo of the front and back of the garment.",
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        SidePicker(
            label = "FRONT",
            hasImage = frontHasImage,
            enabled = !state.isLoading,
            onPick = { pickFront.launch("image/*") }
        )
        Spacer(Modifier.height(16.dp))
        SidePicker(
            label = "BACK",
            hasImage = backHasImage,
            enabled = !state.isLoading,
            onPick = { pickBack.launch("image/*") }
        )

        Spacer(Modifier.height(24.dp))

        if (state.isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(12.dp))
                Text("Segmenting…")
            }
        }

        state.errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(
                onClick = onOpenEditor,
                enabled = canOpenEditor,
                modifier = Modifier.weight(1f)
            ) { Text("Open Editor") }
        }
    }
}

@Composable
private fun SidePicker(
    label: String,
    hasImage: Boolean,
    enabled: Boolean,
    onPick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (hasImage) Color(0xFFE0F7FA) else Color(0xFFF5F5F5))
            .border(
                width = 1.dp,
                color = if (hasImage) Color(0xFF00ACC1) else Color(0xFFBDBDBD),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onPick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                tint = Color(0xFF455A64)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (hasImage) "$label ✓" else "Select $label Image",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
