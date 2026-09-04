package com.example.myapplicationkoG

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CreatePostScreen(
    onPostPublished: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PostRepository() }

    val categories = listOf(
        "Trend",
        "Vintage",
        "Streetwear"
    )

    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var isCategoryMenuOpen by remember { mutableStateOf(false) }

    // 1. Added State for Clothing Title (e.g., Long Sleeve, Pants)
    var clothingTitle by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    // Supports up to 9 image URIs
    val imageUris = remember { mutableStateListOf<Uri>() }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Toggle post classification (Post vs Challenge)
    var isChallenge by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // UCrop Launcher for single-image cropping
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val croppedUri = result.data?.let { UCrop.getOutput(it) }
            if (croppedUri != null) {
                if (imageUris.size < 9) {
                    imageUris.add(croppedUri)
                }
                errorMessage = null
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            errorMessage = result.data?.let { UCrop.getError(it) }?.message
                ?: "Could not crop the image."
        }
    }

    // Gallery Picker supporting up to 9 photos
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val availableSpace = 9 - imageUris.size
            val urisToAdd = uris.take(availableSpace)
            imageUris.addAll(urisToAdd)
            errorMessage = null
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { photoWasSaved ->
        if (photoWasSaved && pendingCameraUri != null) {
            if (imageUris.size < 9) {
                imageUris.add(pendingCameraUri!!)
            }
            errorMessage = null
        }
        pendingCameraUri = null
    }

    fun openCamera() {
        if (imageUris.size >= 9) {
            errorMessage = "You can only select up to 9 photos."
            return
        }
        val file = File(context.cacheDir, "post_${System.currentTimeMillis()}.jpg")
        val cameraUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        pendingCameraUri = cameraUri
        cameraLauncher.launch(cameraUri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding() // Keyboard overlap fix
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Bar Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Create post",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(16.dp))

        // Category Selection Dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { isCategoryMenuOpen = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Category: $selectedCategory",
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Choose category"
                )
            }

            DropdownMenu(
                expanded = isCategoryMenuOpen,
                onDismissRequest = { isCategoryMenuOpen = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            selectedCategory = category
                            isCategoryMenuOpen = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Selected Media Gallery Display (1 to 9 photos)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUris.isEmpty()) {
                Text(
                    text = "Select up to 9 photos for your post",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(imageUris) { index, uri ->
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Photo ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Remove photo button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                    .clickable { imageUris.removeAt(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove photo",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Image Selection Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UploadChoiceButton(
                modifier = Modifier.weight(1f),
                label = "Camera",
                icon = Icons.Default.CameraAlt,
                onClick = { openCamera() }
            )
            UploadChoiceButton(
                modifier = Modifier.weight(1f),
                label = "Gallery (${imageUris.size}/9)",
                icon = Icons.Default.PhotoLibrary,
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // 2. Clothing Type / Title Input Field
        OutlinedTextField(
            value = clothingTitle,
            onValueChange = { clothingTitle = it },
            label = { Text("Clothing Type (e.g. Long Sleeve, Pants)") },
            placeholder = { Text("Long Sleeve") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // 3. Caption / Bio Input Field
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio / caption") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Post Classification Toggle
        Text(
            text = "Publish as",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = !isChallenge, onClick = { isChallenge = false })
            Text(
                text = "Post",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable { isChallenge = false }
            )
            Spacer(Modifier.width(24.dp))
            RadioButton(selected = isChallenge, onClick = { isChallenge = true })
            Text(
                text = "Challenge",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable { isChallenge = true }
            )
        }

        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        // Publish Action Button
        Button(
            enabled = !isPublishing,
            onClick = {
                when {
                    imageUris.isEmpty() -> errorMessage = "Please choose at least one photo."
                    clothingTitle.isBlank() -> errorMessage = "Please enter a clothing type (title)."
                    else -> scope.launch {
                        isPublishing = true
                        errorMessage = null
                        try {
                            val finalPostType = if (isChallenge) "Challenge" else "Post"

                            // 4. Pass clothingTitle as 'title' parameter to createPost
                            repository.createPost(
                                context = context,
                                imageUris = imageUris,
                                title = clothingTitle,
                                category = selectedCategory,
                                bio = bio,
                                postType = finalPostType,
                                isChallenge = isChallenge
                            )
                            onPostPublished()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Could not publish the post."
                        } finally {
                            isPublishing = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isPublishing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Post")
            }
        }
    }
}

@Composable
private fun UploadChoiceButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Button(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}