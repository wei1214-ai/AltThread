package com.example.myapplicationkoG

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplicationkoG.ui.theme.Cyan
import com.example.myapplicationkoG.ui.theme.LightGray
import com.example.myapplicationkoG.ui.theme.MidnightBlue


@Composable
private fun AuthTabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape = RoundedCornerShape(size = 25.dp))
            .background(color = if (isSelected) Cyan else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MidnightBlue else Color.Gray
        )
    }
}

@Composable
private fun AuthInputField(
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(shape = RoundedCornerShape(size = 12.dp))
            .background(color = LightGray)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = placeholder, color = Color.Gray)
    }
}

@Composable
private fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(shape = RoundedCornerShape(size = 25.dp))
            .background(color = Cyan)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, color = MidnightBlue)
    }
}

@Composable
fun AuthScreen() {
    var isLogin by remember { mutableStateOf(value = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        // Upper Border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
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
                    .padding(all = 24.dp)
            ) {
                Text(
                    text = "AltThread",
                    fontSize = 45.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Cyan
                )
                Text(
                    text = "Upcycle. Design. Inspire.",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = White
                )
            }
        }

        // Lower Border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(shape = RoundedCornerShape(size = 25.dp))
                        .background(color = LightGray)
                ) {
                    AuthTabItem(
                        text = "Log In",
                        isSelected = isLogin,
                        onClick = { isLogin = true },
                        modifier = Modifier.weight(1f)
                    )
                    AuthTabItem(
                        text = "Register",
                        isSelected = !isLogin,
                        onClick = { isLogin = false },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLogin) {
                    AuthInputField(placeholder = "Username")
                    Spacer(modifier = Modifier.height(16.dp))

                    AuthInputField(placeholder = "Password")
                    Spacer(modifier = Modifier.height(16.dp))

                    AuthPrimaryButton(text = "Log In", onClick = { /* TODO */ })
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Forgot password?",
                            color = MidnightBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    AuthInputField(placeholder = "Email address")
                    Spacer(modifier = Modifier.height(16.dp))

                    AuthInputField(placeholder = "Password")
                    Spacer(modifier = Modifier.height(16.dp))

                    AuthInputField(placeholder = "Confirm Password")
                    Spacer(modifier = Modifier.height(16.dp))

                    AuthPrimaryButton(text = "Create Account", onClick = { /* TODO */ })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AScreenPreview() {
    AuthScreen()
}