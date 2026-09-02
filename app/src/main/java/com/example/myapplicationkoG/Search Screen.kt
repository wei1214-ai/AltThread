package com.example.myapplicationkoG

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.myapplicationkoG.ui.theme.Cyan
import com.example.myapplicationkoG.ui.theme.MidnightBlue
import kotlinx.coroutines.delay

@Composable
fun SearchScreen() {

    var keyword by remember { mutableStateOf("") }
    // "latest" 或 "highest_likes"
    var selectedSortBy by remember { mutableStateOf("latest") }
    var isFilterMenuExpanded by remember { mutableStateOf(false) }

    var results by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedPostForDetail by remember { mutableStateOf<Post?>(null) }

    val repository = remember { PostRepository() }

    // 监听 关键字(keyword) 和 排序规则(selectedSortBy) 的变化
    LaunchedEffect(keyword, selectedSortBy) {
        isLoading = true

        // 当输入框为空时，直接根据当前排序规则获取所有帖子
        if (keyword.isBlank()) {
            try {
                results = repository.getPosts(category = "All", sortBy = selectedSortBy)
            } catch (e: Exception) {
                results = emptyList()
            } finally {
                isLoading = false
            }
            return@LaunchedEffect
        }

        // 防抖处理（Debounce）
        delay(400)

        try {
            // 搜索时把当前选中的排序规则（selectedSortBy）也传进去
            results = repository.searchPosts(query = keyword.trim(), sortBy = selectedSortBy)
        } catch (e: Exception) {
            results = emptyList()
        } finally {
            isLoading = false
        }
    }

    // 点击图片展示 PostCard 弹窗
    selectedPostForDetail?.let { post ->
        Dialog(onDismissRequest = { selectedPostForDetail = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                PostCard(post = post)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // 搜索栏与 Filter 按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(text = "Search clothes, tags...") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = "Search",
                        tint = Color.Gray
                    )
                },
                trailingIcon = {
                    if (keyword.isNotEmpty()) {
                        IconButton(onClick = { keyword = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Filter 按钮及其下拉菜单（DropdownMenu）
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Cyan)
                        .clickable { isFilterMenuExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.filter),
                        contentDescription = "Filter Options",
                        tint = MidnightBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 弹出排序选择菜单
                DropdownMenu(
                    expanded = isFilterMenuExpanded,
                    onDismissRequest = { isFilterMenuExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Latest Posts") },
                        onClick = {
                            selectedSortBy = "latest"
                            isFilterMenuExpanded = false
                        },
                        trailingIcon = {
                            if (selectedSortBy == "latest") {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MidnightBlue)
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Most Liked ❤️") },
                        onClick = {
                            selectedSortBy = "highest_likes"
                            isFilterMenuExpanded = false
                        },
                        trailingIcon = {
                            if (selectedSortBy == "highest_likes") {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MidnightBlue)
                            }
                        }
                    )
                }
            }
        }

        // 内容展示区域
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MidnightBlue)
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No outfits found",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = results,
                    key = { it.id }
                ) { post ->
                    AsyncImage(
                        model = post.mediaUrl,
                        contentDescription = post.caption,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .clickable {
                                selectedPostForDetail = post
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HScreenPreview() {
    SearchScreen()
}