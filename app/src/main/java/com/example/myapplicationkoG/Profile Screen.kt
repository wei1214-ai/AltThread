package com.example.myapplicationkoG

import android.widget.Button
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.example.myapplicationkoG.ui.ProfileRepository
import com.example.myapplicationkoG.ui.UserProfile
import com.example.myapplicationkoG.ui.theme.Cyan
import com.example.myapplicationkoG.ui.theme.MidnightBlue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.rememberCoroutineScope
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun CustomTabBar() {
    var selectedTabIndex by remember { mutableStateOf(0) }

    val tabs = listOf("Posts", "Challenges", "Saved")
    val activeColor = MidnightBlue
    val inactiveColor = Color.Gray


    Column {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = activeColor
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index

                Tab(
                    selected = isSelected,
                    onClick = {
                        selectedTabIndex = index
                    },
                    text = {
                        Text(
                            text = title,
                            color = if (isSelected) activeColor else inactiveColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    onEditProfile: ()-> Unit
) {

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    val repository = remember { ProfileRepository() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { imageUri ->
        if (imageUri != null) {
            scope.launch {
                try {
                    repository.uploadAvatar(context, imageUri)

                    // Reload new avatar URL from Supabase.
                    profile = repository.getMyProfile()
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Could not upload avatar"
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            profile = repository.getMyProfile()
        } catch (e: Exception) {
            errorMessage = e.message ?: "Could not load profile"
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
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
                    Box(
                        modifier = Modifier.size(120.dp)
                    ) {
                        AsyncImage(
                            model = profile?.avatar_url?.takeIf { it.isNotBlank() }
                                ?: R.drawable.avatar,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )

                        IconButton(
                            onClick = {
                                avatarPicker.launch("image/*")
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Change avatar",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

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
                        Text( text = profile?.username ?: "New user",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MidnightBlue)
                        Text(text = "@${profile?.username ?: "newuser"}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MidnightBlue)
                        Text(text = profile?.bio ?: "Add a bio in Edit profile",
                            fontSize = 14.sp,
                            color = MidnightBlue)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onEditProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Edit profile")
                }
                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red
                    )
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
                CustomTabBar()
            }
        }
        items(15) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color.LightGray)
            )
        }
    }
}

