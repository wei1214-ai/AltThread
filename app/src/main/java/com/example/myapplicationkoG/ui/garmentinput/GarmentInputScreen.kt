package com.example.myapplicationkoG.ui.garmentinput

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import coil.compose.AsyncImage
import com.example.myapplicationkoG.domain.model.GarmentSideId
import com.example.myapplicationkoG.editor.EditorViewModel

/**
 * Part 1 entry point for garment capture.
 *
 * User picks FRONT then BACK. Each photo is run on-device through
 * YOLOv8 → SAM 2.1 → OpenCV (rotate + center on 1080x1080) to land
 * the garment in the Design Space. When both sides are ready, the
 * user can open the editor.
 */
@Composable
fun GarmentInputScreen(
    onOpenEditor: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            .background(Color(0xFFFAFAFA))
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // ---------- Title block ----------
        Text(
            text = "New Design Space",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1B1B1B),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Take a clear photo of the front, then the back.",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1B1B1B),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Lay the garment flat, smooth out wrinkles, and shoot from above in good lighting so the AI can isolate the clothing cleanly.",
            fontSize = 13.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // ---------- Tip card ----------
        TipCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        )

        // ---------- Front / Back pickers ----------
        SidePicker(
            label = "FRONT",
            previewUri = state.document?.front?.sourceImage?.uri,
            enabled = !state.isLoading,
            onPick = { pickFront.launch("image/*") }
        )
        Spacer(Modifier.height(14.dp))
        SidePicker(
            label = "BACK",
            previewUri = state.document?.back?.sourceImage?.uri,
            enabled = !state.isLoading,
            onPick = { pickBack.launch("image/*") }
        )

        // ---------- Loading row ----------
        AnimatedVisibility(visible = state.isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF1B1B1B)
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    "Running on-device AI (YOLO + SAM 2.1)…",
                    fontSize = 13.sp,
                    color = Color(0xFF1B1B1B)
                )
            }
        }

        state.errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        // ---------- Bottom buttons ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Back", fontWeight = FontWeight.SemiBold) }

            Button(
                onClick = onOpenEditor,
                enabled = canOpenEditor,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B1B1B),
                    contentColor = Color.White
                )
            ) { Text("Open Editor", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun TipCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF6E5))
            .border(1.dp, Color(0xFFFFD27A), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                "Tips for a clean cutout",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B4A00),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Bullet("Plain background — floor, table, or sheet")
            Bullet("Even, bright lighting (avoid harsh shadows)")
            Bullet("Capture the full garment, edges visible")
            Bullet("Hold the phone straight above, parallel to the garment")
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            "•",
            color = Color(0xFF6B4A00),
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 6.dp, top = 1.dp)
        )
        Text(
            text,
            color = Color(0xFF6B4A00),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SidePicker(
    label: String,
    previewUri: String?,
    enabled: Boolean,
    onPick: () -> Unit
) {
    val hasImage = previewUri != null
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (hasImage) Color(0xFFE7F4EE) else Color.White)
            .border(
                width = 1.5.dp,
                color = if (hasImage) Color(0xFF2E7D5B) else Color(0xFFCCCCCC),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onPick),
        contentAlignment = Alignment.Center
    ) {
        if (hasImage) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                AsyncImage(
                    model = previewUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEEEEEE))
                )
                Spacer(Modifier.size(14.dp))
                Column {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2E7D5B)
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D5B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            "Captured",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D5B)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap to retake",
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = Color(0xFF1B1B1B),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Select $label Image",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF1B1B1B)
                )
            }
        }
    }
}
