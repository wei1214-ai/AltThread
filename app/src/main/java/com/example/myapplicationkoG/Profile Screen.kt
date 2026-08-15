package com.example.myapplicationkoG

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
                .align(Alignment.End)
                .size(40.dp)
                .clip(shape = RoundedCornerShape(size = 12.dp))
                .background(color = Cyan)
                .clickable {
                    // Handle button click action here
                },
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
}

@Preview(showBackground = true)
@Composable
fun PScreen(){
    ProfileScreen()
}