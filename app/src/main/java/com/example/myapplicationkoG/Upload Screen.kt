package com.example.myapplicationkoG

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.myapplicationkoG.di.ServiceLocator
import com.example.myapplicationkoG.ui.theme.Cyan
import com.example.myapplicationkoG.ui.theme.LightGray
import com.example.myapplicationkoG.ui.theme.MidnightBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
private fun UploadTabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(25.dp))
            .background(if (isSelected) Cyan else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MidnightBlue else Color.Gray,
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PostRepository() }

    var isPostMode by remember { mutableStateOf(true) }

    // Post states
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showPublishDialog by remember { mutableStateOf(false) }
    var clothingTitleInput by remember { mutableStateOf("") }
    var captionInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Streetwear") }
    var isUploading by remember { mutableStateOf(false) }
    val categoriesList = listOf("Trend", "Vintage", "Streetwear")

    // Challenge states - two garment images with processing
    var frontOrigUri by remember { mutableStateOf<Uri?>(null) }
    var backOrigUri by remember { mutableStateOf<Uri?>(null) }
    var frontCutoutPath by remember { mutableStateOf<String?>(null) }
    var backCutoutPath by remember { mutableStateOf<String?>(null) }
    var frontError by remember { mutableStateOf<String?>(null) }
    var backError by remember { mutableStateOf<String?>(null) }
    var isProcessingFront by remember { mutableStateOf(false) }
    var isProcessingBack by remember { mutableStateOf(false) }

    var postCameraUri by remember { mutableStateOf<Uri?>(null) }
    var frontCameraUri by remember { mutableStateOf<Uri?>(null) }
    var backCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraSide by remember { mutableStateOf<String?>(null) }

    fun createCameraUri(): Uri {
        val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        file.parentFile?.mkdirs()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris ->
        if (uris.isNotEmpty()) {
            if (selectedUris.size + uris.size > 9) {
                Toast.makeText(context, "Max 9 images", Toast.LENGTH_SHORT).show()
                selectedUris = (selectedUris + uris).take(9)
            } else {
                selectedUris = selectedUris + uris
            }
        }
    }

    val takePostPicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) postCameraUri?.let {
            if (selectedUris.size < 9) selectedUris = selectedUris + it
            else Toast.makeText(context, "Max 9 images", Toast.LENGTH_SHORT).show()
        }
    }

    val takeFrontPicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) frontCameraUri?.let { uri ->
            frontOrigUri = uri
            frontError = null
            frontCutoutPath = null
            isProcessingFront = true
            scope.launch {
                try {
                    val cutout = processChallengeImage(context, uri)
                    frontCutoutPath = cutout.absolutePath
                    frontError = null
                } catch (e: Exception) {
                    frontError = e.message ?: "Could not detect garment"
                    frontCutoutPath = null
                } finally {
                    isProcessingFront = false
                }
            }
        }
    }

    val takeBackPicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) backCameraUri?.let { uri ->
            backOrigUri = uri
            backError = null
            backCutoutPath = null
            isProcessingBack = true
            scope.launch {
                try {
                    val cutout = processChallengeImage(context, uri)
                    backCutoutPath = cutout.absolutePath
                    backError = null
                } catch (e: Exception) {
                    backError = e.message ?: "Could not detect garment"
                    backCutoutPath = null
                } finally {
                    isProcessingBack = false
                }
            }
        }
    }

    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            when (pendingCameraSide) {
                "post" -> {
                    val uri = createCameraUri()
                    postCameraUri = uri
                    takePostPicture.launch(uri)
                }
                "front" -> {
                    val uri = createCameraUri()
                    frontCameraUri = uri
                    takeFrontPicture.launch(uri)
                }
                "back" -> {
                    val uri = createCameraUri()
                    backCameraUri = uri
                    takeBackPicture.launch(uri)
                }
            }
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
        pendingCameraSide = null
    }

    fun launchCamera(side: String) {
        val perm = Manifest.permission.CAMERA
        val granted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = createCameraUri()
            when (side) {
                "post" -> { postCameraUri = uri; takePostPicture.launch(uri) }
                "front" -> { frontCameraUri = uri; takeFrontPicture.launch(uri) }
                "back" -> { backCameraUri = uri; takeBackPicture.launch(uri) }
            }
        } else {
            pendingCameraSide = side
            requestCameraPermission.launch(perm)
        }
    }

    // Challenge pickers - single image
    val pickFrontLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            frontOrigUri = it
            frontError = null
            frontCutoutPath = null
            isProcessingFront = true
            scope.launch {
                try {
                    val cutout = processChallengeImage(context, it)
                    frontCutoutPath = cutout.absolutePath
                    frontError = null
                } catch (e: Exception) {
                    frontError = e.message ?: "Could not detect garment"
                    frontCutoutPath = null
                } finally {
                    isProcessingFront = false
                }
            }
        }
    }
    val pickBackLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            backOrigUri = it
            backError = null
            backCutoutPath = null
            isProcessingBack = true
            scope.launch {
                try {
                    val cutout = processChallengeImage(context, it)
                    backCutoutPath = cutout.absolutePath
                    backError = null
                } catch (e: Exception) {
                    backError = e.message ?: "Could not detect garment"
                    backCutoutPath = null
                } finally {
                    isProcessingBack = false
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Upload", fontWeight = FontWeight.ExtraBold, color = MidnightBlue, fontSize = 22.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White),
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MidnightBlue)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top radio tabs - Post / Challenge (like AuthScreen Login/Register)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(25.dp))
                    .background(LightGray)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                UploadTabItem(
                    text = "Post",
                    isSelected = isPostMode,
                    onClick = { isPostMode = true },
                    modifier = Modifier.weight(1f)
                )
                UploadTabItem(
                    text = "Challenge",
                    isSelected = !isPostMode,
                    onClick = { isPostMode = false },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isPostMode) {
                // POST MODE ------------------------------------------------
                Text("Create Post", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MidnightBlue)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Share your outfit with the community", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))

                // Category - only available when Post selected
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (isPostMode && !isUploading) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        enabled = isPostMode && !isUploading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MidnightBlue,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categoriesList.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    if (isPostMode) {
                                        selectedCategory = category
                                        expanded = false
                                    }
                                },
                                enabled = isPostMode
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = clothingTitleInput,
                    onValueChange = { clothingTitleInput = it },
                    label = { Text("Clothing Type") },
                    placeholder = { Text("Long Sleeve") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUploading,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightBlue)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = captionInput,
                    onValueChange = { captionInput = it },
                    label = { Text("Description / Bio") },
                    placeholder = { Text("pink color cartoon clothes") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4,
                    enabled = !isUploading,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightBlue)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Image picker area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8F8F8))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    if (selectedUris.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MidnightBlue, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Tap the buttons below to add images (up to 9)", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                "${selectedUris.size}/9 selected",
                                color = MidnightBlue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 44.dp)
                            ) {
                                items(selectedUris) { uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        modifier = Modifier.size(84.dp).clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Cyan).clickable { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(id = R.drawable.addfromalbum), contentDescription = "Gallery", tint = MidnightBlue, modifier = Modifier.size(18.dp))
                        }
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Cyan).clickable { launchCamera("post") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(id = R.drawable.addfromcamera), contentDescription = "Camera", tint = MidnightBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val postReady = selectedUris.isNotEmpty() && clothingTitleInput.isNotBlank()
                Button(
                    onClick = {
                        if (selectedUris.isEmpty()) {
                            Toast.makeText(context, "Please select at least one image", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
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
                                    Toast.makeText(context, "Post created!", Toast.LENGTH_SHORT).show()
                                    onClose()
                                } else {
                                    Toast.makeText(context, "Failed to create post", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = MidnightBlue, disabledContainerColor = Color(0xFFE0E0E0), disabledContentColor = Color.Gray),
                    enabled = postReady && !isUploading
                ) {
                    if (isUploading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MidnightBlue, strokeWidth = 2.dp)
                    else Text("Post", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

            } else {
                // CHALLENGE MODE ------------------------------------------------
                Text("Join Challenge", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MidnightBlue)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Upload front and back garment photos. Each will be processed to isolate the garment.", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))

                // Front challenge box
                ChallengePickerBox(
                    label = "FRONT",
                    origUri = frontOrigUri,
                    cutoutPath = frontCutoutPath,
                    isProcessing = isProcessingFront,
                    error = frontError,
                    onAlbum = { pickFrontLauncher.launch("image/*") },
                    onCamera = { launchCamera("front") }
                )
                if (!frontError.isNullOrBlank() && frontCutoutPath == null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(frontError!!, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                ChallengePickerBox(
                    label = "BACK",
                    origUri = backOrigUri,
                    cutoutPath = backCutoutPath,
                    isProcessing = isProcessingBack,
                    error = backError,
                    onAlbum = { pickBackLauncher.launch("image/*") },
                    onCamera = { launchCamera("back") }
                )
                if (!backError.isNullOrBlank() && backCutoutPath == null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(backError!!, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = clothingTitleInput,
                    onValueChange = { clothingTitleInput = it },
                    label = { Text("Challenge Title") },
                    placeholder = { Text("My upcycle look") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightBlue)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = captionInput,
                    onValueChange = { captionInput = it },
                    label = { Text("Description") },
                    placeholder = { Text("Tell us your story...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightBlue)
                )

                Spacer(modifier = Modifier.height(24.dp))

                val challengeReady = frontCutoutPath != null && backCutoutPath != null && clothingTitleInput.isNotBlank() && !isProcessingFront && !isProcessingBack

                Button(
                    onClick = {
                        if (frontCutoutPath == null || backCutoutPath == null) {
                            Toast.makeText(context, "Please upload and process both front and back images", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (clothingTitleInput.isBlank()) {
                            Toast.makeText(context, "Please enter title", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isUploading = true
                        scope.launch {
                            try {
                                val uris = listOf(Uri.fromFile(File(frontCutoutPath!!)), Uri.fromFile(File(backCutoutPath!!)))
                                val success = repository.createPost(
                                    context = context,
                                    imageUris = uris,
                                    title = clothingTitleInput,
                                    category = "Challenge",
                                    bio = captionInput,
                                    isChallenge = true
                                )
                                if (success) {
                                    Toast.makeText(context, "Challenge posted!", Toast.LENGTH_SHORT).show()
                                    onClose()
                                } else {
                                    Toast.makeText(context, "Failed to post challenge", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = MidnightBlue, disabledContainerColor = Color(0xFFE0E0E0), disabledContentColor = Color.Gray),
                    enabled = !isUploading && challengeReady
                ) {
                    if (isUploading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MidnightBlue, strokeWidth = 2.dp)
                    else Text("Post Challenge", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Publish dialog kept for backward compatibility - not used in new flow
    if (showPublishDialog) {
        Dialog(onDismissRequest = { if (!isUploading) showPublishDialog = false }) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(20.dp)
            ) {
                Text("Legacy dialog", color = Color.Gray)
            }
        }
    }
}

@Composable
private fun ChallengePickerBox(
    label: String,
    origUri: Uri?,
    cutoutPath: String?,
    isProcessing: Boolean,
    error: String?,
    onAlbum: () -> Unit,
    onCamera: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (cutoutPath != null) Cyan.copy(alpha = 0.12f) else Color.White)
            .border(1.5.dp, if (cutoutPath != null) MidnightBlue else Color(0xFFCCCCCC), RoundedCornerShape(16.dp))
            .clickable { if (!isProcessing) onAlbum() }
            .padding(12.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MidnightBlue, modifier = Modifier.align(Alignment.TopStart))
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                isProcessing -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MidnightBlue, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Processing $label image...", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                cutoutPath != null -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Original", fontSize = 10.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            AsyncImage(model = origUri, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFEEEEEE)), contentScale = ContentScale.Crop)
                        }
                        Text("→", color = MidnightBlue, fontWeight = FontWeight.Bold)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Cutout", fontSize = 10.sp, color = MidnightBlue, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            AsyncImage(model = cutoutPath, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).border(1.dp, MidnightBlue, RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                        }
                    }
                }
                origUri != null && error != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tap to retry", color = Color.Gray, fontSize = 11.sp)
                    }
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF1B1B1B), modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Select $label photo", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF1B1B1B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Garment will be isolated", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
        // album + camera buttons bottom-right
        Row(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Cyan).clickable { if (!isProcessing) onAlbum() },
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(id = R.drawable.addfromalbum), contentDescription = "Album", tint = MidnightBlue, modifier = Modifier.size(18.dp))
            }
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Cyan).clickable { if (!isProcessing) onCamera() },
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(id = R.drawable.addfromcamera), contentDescription = "Camera", tint = MidnightBlue, modifier = Modifier.size(18.dp))
            }
        }
    }
    if (error != null && cutoutPath == null && !isProcessing) {
        Spacer(modifier = Modifier.height(4.dp))
        // error already shown inside box, no extra needed
    }
}

private suspend fun processChallengeImage(context: android.content.Context, uri: Uri): File = withContext(Dispatchers.IO) {
    val inference = ServiceLocator.inferencePipeline(context)
    val cacheDir = File(context.filesDir, "garment_assets").apply { mkdirs() }
    val imported = copyToCache(context, uri, cacheDir) ?: error("Could not import image")
    try {
        val out = File(cacheDir, "cutout2_${sha256(imported)}.png")
        if (!(out.exists() && out.length() > 0L)) {
            val result = inference.run(imported)
            try {
                savePng(result.cutout, out)
            } finally {
                runCatching { if (!result.cutout.isRecycled) result.cutout.recycle() }
            }
        }
        if (!out.exists() || out.length() == 0L) error("Failed to process image - no garment detected")
        // Validate that cutout actually contains non-transparent pixels (garment detected)
        // If fails, pipeline would have thrown; but double-check via file size
        out
    } finally {
        runCatching { imported.delete() }
    }
}

private fun copyToCache(context: android.content.Context, uri: Uri, cacheDir: File): File? {
    val target = File(cacheDir, "in_upload_${UUID.randomUUID()}.jpg")
    return try {
        context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
        if (target.length() > 0L) target else null
    } catch (_: Exception) { null }
}

private fun savePng(bitmap: Bitmap, file: File) {
    file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
}

private fun sha256(file: File): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    file.inputStream().use { ins ->
        val buf = ByteArray(8192)
        while (true) {
            val n = ins.read(buf)
            if (n <= 0) break
            digest.update(buf, 0, n)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

@Composable
fun ActionIcon(
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 30.dp,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    androidx.compose.material3.IconButton(onClick = onClick, modifier = modifier) {
        androidx.compose.material3.Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}
