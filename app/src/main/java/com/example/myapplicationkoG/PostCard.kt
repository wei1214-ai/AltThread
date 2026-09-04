package com.example.myapplicationkoG

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.myapplicationkoG.ui.ProfileRepository
import com.example.myapplicationkoG.ui.theme.MidnightBlue
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun extractAvatarUrl(post: Post): String {
    return try {
        val field = post::class.java.declaredFields.firstOrNull {
            it.name.contains("avatar", ignoreCase = true) ||
                    it.name.contains("profile", ignoreCase = true) ||
                    it.name.contains("userpic", ignoreCase = true) ||
                    it.name.contains("image", ignoreCase = true)
        }
        field?.isAccessible = true
        (field?.get(post) as? String) ?: ""
    } catch (e: Exception) {
        ""
    }
}

private fun formatPostTime(createdAt: String): String {
    if (createdAt.isBlank()) return ""

    return runCatching {
        OffsetDateTime.parse(createdAt)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a"))
    }.getOrElse {
        createdAt
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: Post,
    modifier: Modifier = Modifier,
    onUserClick: ((userId: String, username: String, avatarUrl: String) -> Unit)? = null,
    onAcceptChallenge: ((Post) -> Unit)? = null,
    onDeletePost: ((Post) -> Unit)? = null
) {
    val repository = remember { PostRepository() }
    val profileRepository = remember { ProfileRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val avatarUrl = remember(post) { extractAvatarUrl(post) }

    var currentUserId by remember { mutableStateOf<String?>(null) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val myProfile = profileRepository.getMyProfile()
            currentUserId = myProfile.id
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val imagesList = remember(post) {
        if (post.mediaUrls.isNotEmpty()) {
            post.mediaUrls
        } else if (post.mediaUrl.isNotBlank()) {
            listOf(post.mediaUrl)
        } else {
            emptyList()
        }
    }

    val pagerState = rememberPagerState(pageCount = { imagesList.size })

    var showFullScreenViewer by remember { mutableStateOf(false) }
    var fullScreenInitialPage by remember { mutableIntStateOf(0) }

    var isLiked by remember { mutableStateOf(post.isLikedByCurrentUser) }
    var likeCount by remember { mutableIntStateOf(post.likeCount) }
    var isSaved by remember { mutableStateOf(post.isFavoritedByCurrentUser) }

    LaunchedEffect(post) {
        isLiked = post.isLikedByCurrentUser
        likeCount = post.likeCount
        isSaved = post.isFavoritedByCurrentUser
    }

    val doubleTapHeartScale = remember { Animatable(0f) }

    var showCommentsDialog by remember { mutableStateOf(false) }
    var commentsList by remember { mutableStateOf<List<PostComment>>(emptyList()) }
    var isLoadingComments by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    var isSendingComment by remember { mutableStateOf(false) }
    var commentError by remember { mutableStateOf<String?>(null) }

    fun handleLikeToggle() {
        val targetIsLiked = !isLiked
        isLiked = targetIsLiked
        likeCount = if (targetIsLiked) likeCount + 1 else (likeCount - 1).coerceAtLeast(0)

        scope.launch {
            val serverCount = repository.toggleLike(post.id)
            if (serverCount != -1) {
                likeCount = serverCount
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // 1. User Header & Fixed 3-Dot Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onUserClick?.invoke(post.userId, post.username, avatarUrl)
                        }
                ) {
                    AsyncImage(
                        model = avatarUrl.ifEmpty { "https://via.placeholder.com/150" },
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColorForTheme(MidnightBlue)
                    )
                    val postedTime = formatPostTime(post.createdAt)

                    if (postedTime.isNotBlank()) {
                        Text(
                            text = "Posted $postedTime",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(com.example.myapplicationkoG.ui.theme.Cyan)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.clothingCategory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightBlue
                    )
                }
            }

                // 3-dot menu button for post owner
                if (currentUserId == post.userId) {
                    Box(
                        modifier = Modifier.padding(start = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { isMenuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = textColorForTheme(MidnightBlue),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Post", color = Color.Red, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1b. Title & Description
            val isValidTitle = remember(post.clothingTitle, post.clothingCategory) {
                post.clothingTitle.isNotBlank() &&
                        !post.clothingTitle.equals("For You", ignoreCase = true) &&
                        !post.clothingTitle.equals(post.clothingCategory, ignoreCase = true)
            }
            if (isValidTitle) {
                Text(
                    text = post.clothingTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = textColorForTheme(MidnightBlue),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            if (post.caption.isNotBlank() && !post.caption.equals(post.clothingTitle, ignoreCase = true)) {
                Text(
                    text = post.caption,
                    fontSize = 13.sp,
                    color = textColorForTheme(Color.DarkGray),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            val isChallengePost = post.postType.equals("Challenge", ignoreCase = true)
            if (isChallengePost && onAcceptChallenge != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(com.example.myapplicationkoG.ui.theme.Cyan)
                        .clickable { onAcceptChallenge(post) }
                        .padding(horizontal = 18.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Accept Challenge", fontWeight = FontWeight.Bold, color = MidnightBlue, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 2. Post Media
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (imagesList.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(isLiked) {
                                    detectTapGestures(
                                        onTap = {
                                            fullScreenInitialPage = page
                                            showFullScreenViewer = true
                                        },
                                        onDoubleTap = {
                                            if (!isLiked) {
                                                handleLikeToggle()
                                            }
                                            scope.launch {
                                                doubleTapHeartScale.snapTo(0f)
                                                doubleTapHeartScale.animateTo(
                                                    targetValue = 1.2f,
                                                    animationSpec = spring(dampingRatio = 0.5f)
                                                )
                                                doubleTapHeartScale.animateTo(0f)
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = imagesList[page],
                                contentDescription = post.clothingTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    if (doubleTapHeartScale.value > 0f) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Double Tap Heart",
                            tint = Color.Red.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(100.dp)
                                .graphicsLayer {
                                    scaleX = doubleTapHeartScale.value
                                    scaleY = doubleTapHeartScale.value
                                }
                        )
                    }

                    if (imagesList.size > 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1}/${imagesList.size}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (imagesList.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(imagesList.size) { iteration ->
                                val isSelected = pagerState.currentPage == iteration
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 7.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MidnightBlue else Color.White.copy(alpha = 0.6f))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { handleLikeToggle() }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else MidnightBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        showCommentsDialog = true
                        scope.launch {
                            isLoadingComments = true
                            commentsList = repository.getComments(post.id)
                            isLoadingComments = false
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ModeComment,
                        contentDescription = "Comment",
                        tint = MidnightBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = {
                    val targetSaved = !isSaved
                    isSaved = targetSaved

                    scope.launch {
                        repository.toggleFavourite(post.id, targetSaved)
                    }
                }) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Favorite",
                        tint = MidnightBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = {
                    val currentDisplayImage = imagesList.getOrNull(pagerState.currentPage) ?: post.mediaUrl
                    val formattedShareText = """
                        ✨ Look at this amazing outfit on AltThread!
                        
                        👕 Clothing Type: ${post.clothingTitle}
                        🏷️ Category: ${post.clothingCategory}
                        👤 Posted by: @${post.username}
                        
                        🖼️ Image:
                        $currentDisplayImage
                    """.trimIndent()

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, formattedShareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share outfit via..."))
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MidnightBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 4. Likes Counter
            Text(
                text = "$likeCount likes",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textColorForTheme(MidnightBlue),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    // Deletion Alert Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("Delete Post", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this post? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {
                        isDeleting = true
                        scope.launch {
                            val success = repository.deletePost(post.id)
                            isDeleting = false
                            showDeleteDialog = false
                            if (success) {
                                onDeletePost?.invoke(post)
                            }
                        }
                    }
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Red)
                    } else {
                        Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Fullscreen Image Viewer Modal
    if (showFullScreenViewer && imagesList.isNotEmpty()) {
        Dialog(
            onDismissRequest = { showFullScreenViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val fullPagerState = rememberPagerState(
                initialPage = fullScreenInitialPage,
                pageCount = { imagesList.size }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                HorizontalPager(
                    state = fullPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }

                    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
                        scale = (scale * zoomChange).coerceIn(1f, 4f)
                        if (scale > 1f) {
                            offset += offsetChange
                        } else {
                            offset = Offset.Zero
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(state = transformableState)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { showFullScreenViewer = false }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = imagesList[page],
                            contentDescription = "Full Screen Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                IconButton(
                    onClick = { showFullScreenViewer = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (imagesList.size > 1) {
                    Text(
                        text = "${fullPagerState.currentPage + 1}/${imagesList.size}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(28.dp)
                    )
                }
            }
        }
    }

    // Comments Dialog
    if (showCommentsDialog) {
        Dialog(onDismissRequest = { showCommentsDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Comments",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textColorForTheme(MidnightBlue),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        if (isLoadingComments) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MidnightBlue)
                            }
                        } else if (commentsList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No comments yet. Be the first!", color = textColorForTheme(Color.Gray))
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(commentsList) { comment ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        AsyncImage(
                                            model = comment.avatar_url ?: "https://via.placeholder.com/150",
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    showCommentsDialog = false
                                                    onUserClick?.invoke(comment.userId, comment.username, comment.avatar_url ?: "")
                                                },
                                            contentScale = ContentScale.Crop
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = comment.username,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = textColorForTheme(MidnightBlue),
                                                modifier = Modifier.clickable {
                                                    showCommentsDialog = false
                                                    onUserClick?.invoke(comment.userId, comment.username, comment.avatar_url ?: "")
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = comment.content,
                                                fontSize = 13.sp,
                                                color = textColorForTheme(Color.Black)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text("Add a comment...", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                color = textColorForTheme(Color.Black)
                            ),
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (newCommentText.isBlank() || isSendingComment) return@IconButton

                                val textToSend = newCommentText.trim()
                                isSendingComment = true
                                commentError = null

                                scope.launch {
                                    val saved = repository.addComment(post.id, textToSend)
                                    isSendingComment = false

                                    if (saved) {
                                        newCommentText = ""
                                        commentsList = repository.getComments(post.id)
                                    } else {
                                        commentError = "Could not send comment. Please try again."
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send Comment",
                                tint = MidnightBlue
                            )
                        }
                    }
                }
            }
        }
    }
}