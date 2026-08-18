package com.example.myapplicationkoG

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.myapplicationkoG.ui.theme.Cyan
import com.example.myapplicationkoG.ui.theme.MidnightBlue

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
            .clip(shape = RoundedCornerShape(size = 12.dp))
            .background(color = if (isSelected) Cyan else Color.LightGray)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) MidnightBlue else Color.Gray,
            maxLines = 1
        )
    }
}

@Composable
fun HomeScreen() {
    var selectedFilter by remember { mutableStateOf("For You") }
    val filters = listOf("For You", "Trend", "Challenge", "Vintage", "Streetwear")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White),
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                ) {
                    // Title
                    Text(
                        text = "AltThread",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MidnightBlue,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Notice Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(shape = RoundedCornerShape(size = 12.dp))
                            .background(color = Cyan)
                            .align(Alignment.CenterEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { /* Handle click action here */ },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.notice),
                                contentDescription = "Notice",
                                tint = MidnightBlue,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
                // Filter Row
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0.85f to Color.Transparent,
                                    1.0f to Color.White
                                )
                            )
                        }
                        .horizontalScroll(state = rememberScrollState()),
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
        items(count = 20) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.8f)
                    .clip(shape = RoundedCornerShape(size = 25.dp))
                    .background(Color.LightGray)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HScreenPreview() {
    HomeScreen()
}