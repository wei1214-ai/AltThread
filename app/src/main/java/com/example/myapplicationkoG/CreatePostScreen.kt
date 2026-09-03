package com.example.myapplicationkoG

import android.app.Activity
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
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
        "For You",
        "Trend",
        "Vintage",
        "Streetwear"
    )

    var selectedCategory by remember { mutableStateOf("For You") }
    var isCategoryMenuOpen by remember { mutableStateOf(false) }
    var bio by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isChallenge by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val croppedUri = result.data?.let { UCrop.getOutput(it) }

            if (croppedUri != null) {
                imageUri = croppedUri
                errorMessage = null
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            errorMessage = result.data?.let { UCrop.getError(it) }?.message
                ?: "Could not crop the image."
        }
    }
    fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(
            File(
                context.cacheDir,
                "cropped_post_${System.currentTimeMillis()}.jpg"
            )
        )

        val cropIntent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f) // Square Instagram-style crop
            .getIntent(context)

        cropLauncher.launch(cropIntent)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            errorMessage = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { photoWasSaved ->
        if (photoWasSaved) {
            imageUri = pendingCameraUri
            errorMessage = null
        }

        pendingCameraUri = null
    }


    fun openCamera() {
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
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

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = {
                    isCategoryMenuOpen = true
                },
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
                onDismissRequest = {
                    isCategoryMenuOpen = false
                }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Text(category)
                        },
                        onClick = {
                            selectedCategory = category
                            isCategoryMenuOpen = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri == null) {
                Text(
                    text = "Choose a photo for your post",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected post photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(Modifier.height(12.dp))

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
                label = "Gallery",
                icon = Icons.Default.PhotoLibrary,
                onClick = { galleryLauncher.launch("image/*") }
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio / caption") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

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

        Button(
            enabled = !isPublishing,
            onClick = {
                when {
                    imageUri == null -> errorMessage = "Please choose a photo."
                    else -> scope.launch {
                        isPublishing = true
                        errorMessage = null
                        try{
                            repository.createPost(
                                context = context,
                                imageUri = imageUri!!,
                                category = selectedCategory,
                                bio = bio,
                                isChallenge = isChallenge
                            )
                            onPostPublished()
                        }catch (e: Exception){
                            errorMessage = e.message?:"Could not publish the post."
                        }finally {
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
