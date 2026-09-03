package com.example.myapplicationkoG

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.myapplicationkoG.ui.theme.MidnightBlue
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Helper function to safely extract avatar string from Post regardless of property naming
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

@Composable
fun PostCard(
    post: Post,
    modifier: Modifier = Modifier,
    onUserClick: ((userId: String, username: String, avatarUrl: String) -> Unit)? = null
) {
    val repository = remember { PostRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val avatarUrl = remember(post) { extractAvatarUrl(post) }

    var isLiked by remember { mutableStateOf(post.isLikedByCurrentUser) }
    var likeCount by remember { mutableIntStateOf(post.likeCount) }
    var isSaved by remember { mutableStateOf(post.isFavoritedByCurrentUser) }

    // Comments dialog states
    var showCommentsDialog by remember { mutableStateOf(false) }
    var commentsList by remember { mutableStateOf<List<PostComment>>(emptyList()) }
    var isLoadingComments by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    var isSendingComment by remember { mutableStateOf(false) }
    var commentError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // 1. User Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
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

                Column {
                    Text(
                        text = post.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MidnightBlue
                    )
                    Text(
                        text = post.clothingCategory,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Post Media
            AsyncImage(
                model = post.mediaUrl,
                contentDescription = post.clothingTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                IconButton(onClick = {
                    val targetIsLiked = !isLiked
                    isLiked = targetIsLiked
                    likeCount = if (targetIsLiked) likeCount + 1 else (likeCount - 1).coerceAtLeast(0)

                    scope.launch {
                        val serverCount = repository.toggleLike(post.id)
                        if (serverCount != -1) {
                            likeCount = serverCount
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else MidnightBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Comment Button
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

                // Favorite Button
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

                // Rich Detail Share Button
                IconButton(onClick = {
                    val formattedShareText = """
                        ✨ Look at this amazing outfit on AltThread!
                        
                        👕 Title: ${post.clothingTitle}
                        🏷️ Category: ${post.clothingCategory}
                        👤 Posted by: @${post.username}
                        
                        🖼️ Image:
                        ${post.mediaUrl}
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
                color = MidnightBlue,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 5. Title & Caption
            Text(
                text = post.clothingTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MidnightBlue,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (post.caption.isNotBlank()) {
                Text(
                    text = post.caption,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
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
                        color = MidnightBlue,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        if (isLoadingComments) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MidnightBlue)
                            }
                        } else if (commentsList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No comments yet. Be the first!", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(commentsList) { comment ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF6F8FA), RoundedCornerShape(10.dp))
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
                                                color = MidnightBlue,
                                                modifier = Modifier.clickable {
                                                    showCommentsDialog = false
                                                    onUserClick?.invoke(comment.userId,comment.username, comment.avatar_url ?: "")
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = comment.content,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.surface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Send comment input field
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
                            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.surface),
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