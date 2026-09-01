package com.example.myapplicationkoG

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplicationkoG.ui.theme.Cyan
import com.example.myapplicationkoG.ui.theme.MidnightBlue

@Composable
fun PostCard(
    post: Post,
    modifier: Modifier = Modifier
) {
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(post.initialLikeCount) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // 1. User Header & Category Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Avatar
                    AsyncImage(
                        model = post.userProfilePicUrl ?: "https://via.placeholder.com/150",
                        contentDescription = "Profile Picture",
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

                // Category Tag (e.g. Streetwear)
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

            // 2. Clothing Title
            Text(
                text = post.clothingTitle,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MidnightBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Clothing Outfit Picture / Video
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
            ) {
                AsyncImage(
                    model = post.mediaUrl, // Supabase storage URL
                    contentDescription = "Outfit Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 4. Social Action Buttons (Like, Comment, Share, Save)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like
                IconButton(onClick = {
                    isLiked = !isLiked
                    if (isLiked) likeCount++ else likeCount--
                }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else MidnightBlue
                    )
                }

                // Comment
                IconButton(onClick = { /* TODO: Open Comments */ }) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = MidnightBlue
                    )
                }

                // Share
                IconButton(onClick = { /* TODO: Trigger Share Intent */ }) {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = "Share",
                        tint = MidnightBlue
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bookmark / Save Outfit
                IconButton(onClick = { /* TODO: Save Outfit */ }) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save Outfit",
                        tint = MidnightBlue
                    )
                }
            }

            // 5. Likes Count & Caption
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = "$likeCount likes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MidnightBlue
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
}