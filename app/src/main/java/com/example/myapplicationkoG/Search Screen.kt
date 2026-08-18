package com.example.myapplicationkoG

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplicationkoG.ui.theme.Cyan
import com.example.myapplicationkoG.ui.theme.LightGray
import com.example.myapplicationkoG.ui.theme.MidnightBlue
import com.example.myapplicationkoG.ui.theme.NeonGreen

@Composable
fun SearchScreen(){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
    ) {
        //First Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //Search Bar
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(shape = RoundedCornerShape(size = 12.dp))
                    .background(color = LightGray),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.search),
                    contentDescription = "Icon Description",
                    tint = Gray,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(30.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search",
                    fontSize = 26.sp,
                    color = Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            //Filter Button
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(40.dp)
                    .clip(shape = RoundedCornerShape(size = 12.dp))
                    .background(color = Cyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.filter),
                    contentDescription = "Icon Description",
                    tint = MidnightBlue,
                    modifier = Modifier
                        .size(30.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SeScreenPreview(){
    SearchScreen()
}