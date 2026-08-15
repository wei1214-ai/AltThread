package com.example.myapplicationkoG

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplicationkoG.ui.theme.Cyan
import com.example.myapplicationkoG.ui.theme.LightGray
import com.example.myapplicationkoG.ui.theme.MidnightBlue
import com.example.myapplicationkoG.ui.theme.NeonGreen

@Composable
fun FilterChip(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))    // 12 = corner radius
            .background(color = Cyan)
            .padding(horizontal = 16.dp),   // Adds space on the left and the right side of the text
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            color = MidnightBlue,
            maxLines = 1
        )
    }
}

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = White)
            .navigationBarsPadding()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
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

        Row(
            modifier = Modifier
                .height(40.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilterChip(label = "For You")
            FilterChip(label = "Trend")
            FilterChip(label = "Challenge")
            FilterChip(label = "Vintage")
            FilterChip(label = "Streetwear")
        }
    }

}

@Preview(showBackground = true)
@Composable
fun HScreenPreview() {
    HomeScreen()
}