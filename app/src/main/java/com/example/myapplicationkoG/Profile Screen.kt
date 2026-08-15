package com.example.myapplicationkoG

import android.R.attr.shape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplicationkoG.ui.theme.Cyan
import com.example.myapplicationkoG.ui.theme.MidnightBlue

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
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
                text = "Profile",
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
                Icon(
                    painter = painterResource(id = R.drawable.notice),
                    contentDescription = "Notice",
                    tint = MidnightBlue,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Card(
            border = BorderStroke(5.dp, Cyan),
            shape = RoundedCornerShape(size = 25.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            modifier = Modifier.fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier.align(Alignment.Start)
                    .padding(16.dp)
            ){
                Image(
                    painter = painterResource(id = R.drawable.avatar),
                    contentDescription = "Avatar",
                    modifier = Modifier.clip(shape = CircleShape)
                        .size(120.dp)
                )
                Column(

                ){
                    Text(
                        "BlankPage",
                        color = MidnightBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    )
                    Text(
                        "@blankpagess",
                        color = MidnightBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text("Upcycling enthusiast| Turning trash into treasure | DM for collabs",
                        color = MidnightBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PScreen(){
    ProfileScreen()
}