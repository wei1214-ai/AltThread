package com.example.myapplicationkoG

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.myapplicationkoG.ui.theme.MidnightBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PostRepository() }

    // Selected images state
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Publish Dialog input states
    var showPublishDialog by remember { mutableStateOf(false) }
    var clothingTitleInput by remember { mutableStateOf("") } // Item Type (e.g. "Long Sleeve")
    var captionInput by remember { mutableStateOf("") }       // Bio / Description
    var selectedCategory by remember { mutableStateOf("Streetwear") }
    var isUploading by remember { mutableStateOf(false) }

    val categoriesList = listOf("Trend", "Vintage", "Streetwear", "Minimalist", "Casual")

    // Gallery Launcher for up to 9 images from camera or gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            showPublishDialog = true // Trigger details dialog once images are selected
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .imePadding() // Avoid keyboard obstruction
    ) {
        // Top Action Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(all = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionIcon(iconResId = R.drawable.close, contentDescription = "Close") { onClose() }
            ActionIcon(iconResId = R.drawable.flash_off, contentDescription = "Flash") { }
            ActionIcon(iconResId = R.drawable.setting, contentDescription = "Settings") { }
        }

        // Camera Preview Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
        ) {
            Text(
                text = "Camera Preview Area",
                color = Color.Black,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Shutter & Controls Dock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(all = 16.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionIcon(iconResId = R.drawable.gallery, contentDescription = "Gallery") {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                ActionIcon(iconResId = R.drawable.wardrobebutton, contentDescription = "Tag Clothes") { }
            }

            // Capture / Action Button
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(width = 4.dp, Color.White, CircleShape)
                    .clickable {
                        // 如果之前选了图但关闭了弹窗，点击快门可重新唤起输入弹窗
                        if (selectedUris.isNotEmpty()) {
                            showPublishDialog = true
                        } else {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            ActionIcon(
                modifier = Modifier.align(Alignment.CenterEnd),
                iconResId = R.drawable.flip_camera,
                contentDescription = "Flip Camera"
            ) { }
        }
    }

    // Publish Details Input Dialog
    if (showPublishDialog) {
        Dialog(onDismissRequest = {
            if (!isUploading) {
                showPublishDialog = false
            }
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "New Post Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightBlue
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ⚡ 图片预览区域 (展现选中的多张图片)
                    if (selectedUris.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .padding(vertical = 4.dp)
                        ) {
                            items(selectedUris) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Selected Image",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 1. Clothing Title Input (Item Type)
                    OutlinedTextField(
                        value = clothingTitleInput,
                        onValueChange = { clothingTitleInput = it },
                        label = { Text("Clothing Type (e.g. Long Sleeve, Pants)") },
                        placeholder = { Text("Long Sleeve") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isUploading
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Category Dropdown Selector
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { if (!isUploading) expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = !isUploading
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categoriesList.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Caption / Bio Input Field
                    OutlinedTextField(
                        value = captionInput,
                        onValueChange = { captionInput = it },
                        label = { Text("Description / Bio") },
                        placeholder = { Text("pink color cartoon clothes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4,
                        enabled = !isUploading
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons (Cancel / Post)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showPublishDialog = false
                                selectedUris = emptyList() // 清理选图状态
                            },
                            enabled = !isUploading
                        ) {
                            Text("Cancel", color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (clothingTitleInput.isBlank()) {
                                    Toast.makeText(context, "Please enter clothing type", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isUploading = true
                                scope.launch {
                                    try {
                                        val success = repository.createPost(
                                            context = context,
                                            imageUris = selectedUris,
                                            title = clothingTitleInput,
                                            category = selectedCategory,
                                            bio = captionInput
                                        )

                                        if (success) {
                                            Toast.makeText(context, "Post created successfully!", Toast.LENGTH_SHORT).show()
                                            showPublishDialog = false
                                            onClose()
                                        } else {
                                            Toast.makeText(context, "Failed to create post.", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isUploading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue),
                            enabled = !isUploading
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Post", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionIcon(
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int,
    contentDescription: String,
    size: Dp = 30.dp,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UScreenPreview() {
    UploadScreen()
}