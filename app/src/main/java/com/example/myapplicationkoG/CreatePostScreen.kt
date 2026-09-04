package com.example.myapplicationkoG

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

/**
 * Custom local ExoPlayer Video Player composable dedicated to CreatePostScreen preview.
 * Renamed to CreatePostVideoPlayer to prevent function declaration conflicts with other files.
 */
@OptIn(UnstableApi::class)
@Composable
fun CreatePostVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize ExoPlayer instance for video rendering
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ONE // Loop video endlessly
            prepare()
            playWhenReady = true // Auto-start video playback
        }
    }

    // Release player resources when component leaves composition
    DisposableEffect(videoUrl) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Embed Android Native PlayerView in Jetpack Compose
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false // Hide playback controls for clean preview UI
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Fill container smoothly without black bars
            }
        },
        update = { playerView ->
            // Re-bind player if Compose reuses the view instance
            if (playerView.player != exoPlayer) {
                playerView.player = exoPlayer
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Helper utility to determine whether a given URI represents a video file.
 */
private fun isVideoUri(context: Context, uri: Uri): Boolean {
    val mimeType = context.contentResolver.getType(uri)
    if (mimeType != null && mimeType.startsWith("video", ignoreCase = true)) {
        return true
    }
    val uriString = uri.toString().lowercase()
    return uriString.contains(".mp4") || uriString.contains(".mov") || uriString.contains("video")
}

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

    var clothingTitle by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    // Supports up to 9 image URIs or 1 video URI
    val mediaUris = remember { mutableStateListOf<Uri>() }
    var isVideoSelected by remember { mutableStateOf(false) }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

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
                if (!isVideoSelected && mediaUris.size < 9) {
                    mediaUris.add(croppedUri)
                }
                errorMessage = null
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            errorMessage = result.data?.let { UCrop.getError(it) }?.message
                ?: "Could not crop the image."
        }
    }

    // Gallery Launcher using OpenMultipleDocuments contract to pick images and videos
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val firstUri = uris.first()

            // Request persistent URI permission to allow ExoPlayer reading local media securely
            try {
                context.contentResolver.takePersistableUriPermission(
                    firstUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val containsVideo = isVideoUri(context, firstUri)

            if (containsVideo) {
                // Video mode: Restrict to 1 video only
                mediaUris.clear()
                mediaUris.add(firstUri)
                isVideoSelected = true
                errorMessage = null
            } else {
                // Image mode: Allow up to 9 images
                if (isVideoSelected) {
                    mediaUris.clear()
                    isVideoSelected = false
                }
                val availableSpace = 9 - mediaUris.size
                val urisToAdd = uris.filter { !isVideoUri(context, it) }.take(availableSpace)
                mediaUris.addAll(urisToAdd)
                errorMessage = null
            }
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { photoWasSaved ->
        if (photoWasSaved && pendingCameraUri != null) {
            if (isVideoSelected) {
                mediaUris.clear()
                isVideoSelected = false
            }
            if (mediaUris.size < 9) {
                mediaUris.add(pendingCameraUri!!)
            }
            errorMessage = null
        }
        pendingCameraUri = null
    }

    fun openCamera() {
        if (isVideoSelected) {
            errorMessage = "Please remove the video before adding photos."
            return
        }
        if (mediaUris.size >= 9) {
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
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Navigation Header
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

        // Selected Media Gallery Display (1 to 9 photos or 1 Video)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (mediaUris.isEmpty()) {
                Text(
                    text = "Select up to 9 photos or 1 video for your post",
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
                    itemsIndexed(mediaUris) { index, uri ->
                        val isVideo = isVideoUri(context, uri)

                        Box(
                            modifier = Modifier
                                .width(if (isVideoSelected) 260.dp else 240.dp)
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                        ) {
                            if (isVideo) {
                                CreatePostVideoPlayer(videoUrl = uri.toString())
                            } else {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Photo ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Video Tag Badge
                            if (isVideo) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "VIDEO",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Remove Media Action Button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                    .clickable {
                                        mediaUris.removeAt(index)
                                        if (mediaUris.isEmpty()) {
                                            isVideoSelected = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove media",
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

        // Image & Video Selection Trigger Buttons
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
                label = if (isVideoSelected) "Video Selected" else "Gallery (${mediaUris.size}/9)",
                icon = Icons.Default.PhotoLibrary,
                onClick = {
                    galleryLauncher.launch(arrayOf("image/*", "video/*"))
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Title Input Field
        OutlinedTextField(
            value = clothingTitle,
            onValueChange = { clothingTitle = it },
            label = { Text("Clothing Type (e.g. Long Sleeve, Pants)") },
            placeholder = { Text("Long Sleeve") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Bio / Caption Input Field
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio / caption") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Post Classification Toggle Selection
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

        // Submit Post Action Button
        Button(
            enabled = !isPublishing,
            onClick = {
                when {
                    mediaUris.isEmpty() -> errorMessage = "Please choose at least one photo or video."
                    clothingTitle.isBlank() -> errorMessage = "Please enter a clothing type (title)."
                    else -> scope.launch {
                        isPublishing = true
                        errorMessage = null
                        try {
                            val finalPostType = if (isChallenge) "Challenge" else "Post"

                            repository.createPost(
                                context = context,
                                imageUris = mediaUris,
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
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}