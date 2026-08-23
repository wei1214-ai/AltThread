package com.example.myapplicationkoG

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplicationkoG.ui.theme.MidnightBlue

@Composable
fun PostCard(
    post: Post
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {

        // ==========================================
        // USER INFORMATION
        // ==========================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 4.dp,
                    vertical = 8.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            // Temporary profile picture
            Spacer(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Text(
                text = post.username,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MidnightBlue
            )
        }


        // ==========================================
        // POST IMAGE
        // ==========================================

        AsyncImage(

            model = post.image_url,

            contentDescription =
                post.caption,

            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .clip(
                    RoundedCornerShape(25.dp)
                ),

            contentScale =
                ContentScale.Crop
        )


        // ==========================================
        // CAPTION
        // ==========================================

        Text(
            text = post.caption,

            modifier = Modifier
                .padding(
                    top = 10.dp,
                    start = 8.dp,
                    end = 8.dp
                )
        )


        // ==========================================
        // CATEGORY
        // ==========================================

        Text(
            text = "#${post.category}",

            modifier = Modifier
                .padding(
                    start = 8.dp,
                    top = 4.dp,
                    bottom = 10.dp
                ),

            color = Color.Gray
        )
    }
}