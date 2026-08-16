package com.example.myapplicationkoG

import android.R.attr.shape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CheckboxDefaults.colors
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Gray
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
import com.example.myapplicationkoG.ui.theme.LightCyan
import com.example.myapplicationkoG.ui.theme.MidnightBlue

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .navigationBarsPadding()
            .statusBarsPadding()
            .padding(all = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(shape = RoundedCornerShape(size = 12.dp))
                .background(color = Cyan)
                .align(Alignment.End),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.setting),
                    contentDescription = "Setting",
                    tint = MidnightBlue,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Row() {
            Image(
                painter = painterResource(id = R.drawable.avatar),
                contentDescription = "Avatar",
                modifier = Modifier
                    .clip(shape = CircleShape)
                    .size(120.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(
                    space = 5.dp,
                    alignment = Alignment.CenterVertically
                )
            ) {
                Text("HelloWorld",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MidnightBlue)
                Text("@hellooowrld",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MidnightBlue)
                Text("Chasing trends, and wearing whatever makes a statement.",
                    fontSize = 14.sp,
                    color = MidnightBlue)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row() {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("36",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MidnightBlue)
                Text("Posts",
                    fontSize = 14.sp,
                    color = MidnightBlue)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("446",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MidnightBlue)
                Text("Followers",
                    fontSize = 14.sp,
                    color = MidnightBlue)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("344",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MidnightBlue)
                Text("Following",
                    fontSize = 14.sp,
                    color = MidnightBlue)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(25.dp))
        ){
            Image(
                painter = painterResource(id = R.drawable.wardrobe),
                contentDescription = "Wardrobe",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .alpha(0.7f)
                    .clickable {  }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                MidnightBlue.copy(alpha = 1.0f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(all = 16.dp)
            ) {
                Text(
                    text = "Digital Wardrobe",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Cyan
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.rightarrow),
                contentDescription = null,
                tint = White,
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PScreen(){
    ProfileScreen()
}