package com.example.myapplicationkoG

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplicationkoG.ui.theme.Cyan
import com.example.myapplicationkoG.ui.theme.MidnightBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCard(
    post: Post,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PostRepository() }

    // Like State
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(post.likeCount) }

    // Bookmark / Favourite State
    var isBookmarked by remember { mutableStateOf(false) }

    // Show Likes Dialog State (查看点赞用户弹窗)
    var showLikesDialog by remember { mutableStateOf(false) }
    var likedUsersList by remember { mutableStateOf<List<PostLike>>(emptyList()) }
    var isLoadingLikedUsers by remember { mutableStateOf(false) }

    // Fetch saved state from Supabase whenever post.id changes
    LaunchedEffect(post.id) {
        try {
            isBookmarked = repository.isPostBookmarked(post.id)
            isLiked = repository.isPostLiked(post.id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Comment Bottom Sheet Control
    var showCommentSheet by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    val commentsList = remember { mutableStateListOf("Looks amazing!", "Where did you buy this jacket?") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Header & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = post.userProfilePicUrl ?: "https://via.placeholder.com/150",
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = post.username,
                        fontWeight = FontWeight.Bold,
                        color = MidnightBlue,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Cyan.copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = post.clothingCategory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = post.clothingTitle,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MidnightBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Outfit Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
            ) {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = post.clothingTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Interactive Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. LIKE (Persisted to Supabase)
                IconButton(onClick = {
                    val newLikedState = !isLiked
                    isLiked = newLikedState
                    val newCount = if (newLikedState) likeCount + 1 else (likeCount - 1).coerceAtLeast(0)
                    likeCount = newCount

                    scope.launch {
                        try {
                            repository.toggleLike(post.id, newLikedState, newCount)
                        } catch (e: Exception) {
                            // Revert on failure
                            isLiked = !newLikedState
                            likeCount = if (!newLikedState) likeCount + 1 else (likeCount - 1).coerceAtLeast(0)
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else MidnightBlue
                    )
                }

                // 2. COMMENT
                IconButton(onClick = { showCommentSheet = true }) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = MidnightBlue
                    )
                }

                // 3. SHARE
                IconButton(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, post.clothingTitle)
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Check out this outfit on AltThread: ${post.clothingTitle}\n${post.mediaUrl}"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share outfit via"))
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = "Share",
                        tint = MidnightBlue
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 4. BOOKMARK / FAVOURITE
                IconButton(onClick = {
                    val newBookmarkState = !isBookmarked
                    isBookmarked = newBookmarkState
                    scope.launch {
                        try {
                            repository.toggleFavourite(post.id, newBookmarkState)
                            val message = if (newBookmarkState) "Saved to your favourites!" else "Removed from favourites"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            isBookmarked = !newBookmarkState
                            Toast.makeText(context, "Failed to update favourite", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Favourite",
                        tint = MidnightBlue
                    )
                }
            }

            // Counter & Caption
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                // IG Style: 点击点赞数字查看点赞用户列表
                Text(
                    text = "$likeCount likes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MidnightBlue,
                    modifier = Modifier.clickable {
                        showLikesDialog = true
                        scope.launch {
                            isLoadingLikedUsers = true
                            likedUsersList = repository.getUsersWhoLikedPost(post.id)
                            isLoadingLikedUsers = false
                        }
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.caption,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    maxLines = 3
                )
            }
        }
    }

    // 点赞用户列表弹窗 (Likes Dialog)
    if (showLikesDialog) {
        AlertDialog(
            onDismissRequest = { showLikesDialog = false },
            title = {
                Text(
                    text = "Liked by",
                    fontWeight = FontWeight.Bold,
                    color = MidnightBlue,
                    fontSize = 18.sp
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                ) {
                    if (isLoadingLikedUsers) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Cyan
                        )
                    } else if (likedUsersList.isEmpty()) {
                        Text(
                            text = "No likes yet.",
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(likedUsersList) { like ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    AsyncImage(
                                        model = like.userProfilePicUrl ?: "https://via.placeholder.com/150",
                                        contentDescription = "User Avatar",
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.LightGray)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = like.username ?: "AltUser",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MidnightBlue,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLikesDialog = false }) {
                    Text("Close", color = MidnightBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Comment Modal Bottom Sheet Component
    if (showCommentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCommentSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Comments",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MidnightBlue
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    commentsList.forEach { comment ->
                        Text(
                            text = comment,
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                commentsList.add(commentText.trim())
                                commentText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan)
                    ) {
                        Text("Post", color = MidnightBlue)
                    }
                }
            }
        }
    }
}