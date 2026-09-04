package com.example.myapplicationkoG

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplicationkoG.ui.editor.AiChatDrawer
import com.example.myapplicationkoG.ui.theme.Cyan
import com.example.myapplicationkoG.ui.theme.MidnightBlue

// Filter button
@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Cyan else Color.LightGray)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            color = textColorForTheme(if (isSelected) MidnightBlue else Color.Gray),
            maxLines = 1
        )
    }
}

// Home page
@Composable
fun HomeScreen(
    refreshKey: Int = 0,
    initialFilter: String = "For You",
    onUserClick: ((userId: String, username: String, avatarUrl: String) -> Unit)? = null,
    onAcceptChallenge: ((Post) -> Unit)? = null
) {
    // Selected filter
    var selectedFilter by remember { mutableStateOf(initialFilter) }
    androidx.compose.runtime.LaunchedEffect(initialFilter) {
        if (initialFilter != selectedFilter) selectedFilter = initialFilter
    }

    // Posts from Supabase
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }

    // Loading state
    var isLoading by remember { mutableStateOf(true) }

    // Error message
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val repository = remember { PostRepository() }

    val filters = listOf(
        "For You",
        "Trend",
        "Challenge",
        "Vintage",
        "Streetwear"
    )

    var showAiChat by remember { mutableStateOf(false) }
    val backgroundColor = MaterialTheme.colorScheme.background

    // Load posts whenever selectedFilter changes
    LaunchedEffect(selectedFilter, refreshKey) {
        isLoading = true
        errorMessage = null

        try {
            posts = repository.getPosts(category = selectedFilter)
        } catch (e: Exception) {
            errorMessage = e.message ?: "Something went wrong"
        } finally {
            isLoading = false
        }
    }

    val filterScrollState = rememberScrollState()
    // Main content
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header and filters
            item {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        // App title
                        Text(
                            text = "AltThread",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColorForTheme(MidnightBlue),
                            modifier = Modifier.align(Alignment.Center)
                        )

                        // AI button - same style as Profile setting button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Cyan)
                                .align(Alignment.CenterEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { showAiChat = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ai),
                                    contentDescription = "AI",
                                    tint = MidnightBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .drawWithContent {
                                drawContent()
                                if (filterScrollState.canScrollBackward) {
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            0.0f to backgroundColor,
                                            0.15f to Color.Transparent
                                        )
                                    )
                                }
                                if (filterScrollState.canScrollForward) {
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            0.85f to Color.Transparent,
                                            1.0f to backgroundColor
                                        )
                                    )
                                }
                            }
                            .horizontalScroll(filterScrollState),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        filters.forEach { filter ->
                            FilterChip(
                                label = filter,
                                isSelected = selectedFilter == filter,
                                onClick = { selectedFilter = filter }
                            )
                        }
                    }
                }
            }

            // Loading
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Cyan)
                    }
                }
            }
            // Error
            else if (errorMessage != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Failed to load posts\n\n$errorMessage",
                            color = Color.Red
                        )
                    }
                }
            }
            // No posts
            else if (posts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No posts found.",
                            color = textColorForTheme(Color.Gray)
                        )
                    }
                }
            }
            // Display posts
            else {
                items(
                    items = posts,
                    key = { it.id }
                ) { post ->
                    PostCard(
                        post = post,
                        onUserClick = onUserClick,
                        onAcceptChallenge = onAcceptChallenge
                    )
                }
            }
        }
        AiChatDrawer(visible = showAiChat, onDismiss = { showAiChat = false })
    }
}
