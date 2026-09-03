package com.example.myapplicationkoG

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplicationkoG.ui.editor.EditorPlaceHolder
import com.example.myapplicationkoG.ui.garmentinput.GarmentInputScreen
import com.example.myapplicationkoG.ui.garmentinput.GarmentInputViewModel
import com.example.myapplicationkoG.ui.theme.AltThreadTheme
import com.example.myapplicationkoG.ui.theme.MidnightBlue
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private val garmentInputViewModel: GarmentInputViewModel by viewModels()
    private var sharedPostIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supabase.handleDeeplinks(intent)
        extractPostIdFromIntent(intent)

        enableEdgeToEdge()
        setContent {
            AltThreadTheme {
                val sharedPostId = sharedPostIdState.value

                AltThreadApp(
                    garmentInputVm = garmentInputViewModel,
                    sharedPostId = sharedPostId,
                    onDismissSharedPost = { sharedPostIdState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        supabase.handleDeeplinks(intent)
        extractPostIdFromIntent(intent)
    }

    private fun extractPostIdFromIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "altthread" && data.host == "post") {
            val postId = data.lastPathSegment
            if (!postId.isNullOrEmpty()) {
                sharedPostIdState.value = postId
            }
        }
    }
}

@Composable
fun AltThreadApp(
    garmentInputVm: GarmentInputViewModel,
    sharedPostId: String? = null,
    onDismissSharedPost: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var sharedPost by remember { mutableStateOf<Post?>(null) }
    var isLoadingSharedPost by remember { mutableStateOf(false) }
    val postRepository = remember { PostRepository() }

    // 通用跳转他人主页的方法（自动对 URL 进行 Encode 防止路径报错）
    val navigateToOtherUserProfile = { userId:String, username: String, avatarUrl: String ->
        val encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8.toString())
        val encodedAvatarUrl = URLEncoder.encode(
            avatarUrl.ifEmpty { "https://via.placeholder.com/150" },
            StandardCharsets.UTF_8.toString()
        )
        val encodedUserId = URLEncoder.encode(
            userId,
            StandardCharsets.UTF_8.toString()
        )

        navController.navigate("other_user_profile/$encodedUserId/$encodedUsername/$encodedAvatarUrl")
    }

    val navigateToUserProfile = { userId: String, username: String, avatarUrl: String ->
        val currentUserId = supabase.auth.currentUserOrNull()?.id
        if (userId == currentUserId) {
            navController.navigate(Screen.Profile.route) { launchSingleTop = true }
        } else {
            navigateToOtherUserProfile(userId, username, avatarUrl)
        }
    }

    LaunchedEffect(sharedPostId) {
        if (!sharedPostId.isNullOrEmpty()) {
            isLoadingSharedPost = true
            try {
                val allPosts = postRepository.getPosts()
                sharedPost = allPosts.find { it.id == sharedPostId }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingSharedPost = false
            }
        } else {
            sharedPost = null
        }
    }

    if (sharedPostId != null) {
        Dialog(onDismissRequest = onDismissSharedPost) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoadingSharedPost) {
                    CircularProgressIndicator(color = MidnightBlue)
                } else if (sharedPost != null) {
                    PostCard(
                        post = sharedPost!!,
                        onUserClick = {userId, username, avatarUrl ->
                            onDismissSharedPost()
                            navigateToUserProfile(userId, username, avatarUrl)
                        }
                    )
                } else {
                    onDismissSharedPost()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute in bottomBarRoutes && currentRoute != Screen.Upload.route && currentRoute?.startsWith("other_user_profile") != true) {
                BottomNavBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Auth.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Upload.route) {
                UploadScreen(
                    onClose = { navController.popBackStack() }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onUserClick = { userId,username,avatarUrl ->
                        navigateToUserProfile(userId, username, avatarUrl)
                    }
                )
            }
            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.Studio.route) {
                StudioScreen(
                    onStartDesign = {
                        garmentInputVm.clearAll()
                        navController.navigate(Screen.GarmentInput.route)
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onShowFollowers = { userId -> navController.navigate("followers/$userId") },
                    onShowFollowing = { userId -> navController.navigate("following/$userId") }
                )
            }
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // 新增：他人主页路由与参数解析
            composable(
                route = "other_user_profile/{userId}/{username}/{avatarUrl}",
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("username") { type = NavType.StringType },
                    navArgument("avatarUrl") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedUserId = backStackEntry.arguments?.getString("userId") ?: ""
                val encodedUsername = backStackEntry.arguments?.getString("username") ?: ""
                val encodedAvatarUrl = backStackEntry.arguments?.getString("avatarUrl") ?: ""

                val decodedUserId = URLDecoder.decode(encodedUserId, StandardCharsets.UTF_8.toString())
                val decodedUsername = URLDecoder.decode(encodedUsername, StandardCharsets.UTF_8.toString())
                val decodedAvatarUrl = URLDecoder.decode(encodedAvatarUrl, StandardCharsets.UTF_8.toString())

                OtherUserProfileScreen(
                    userId = decodedUserId,
                    username = decodedUsername,
                    avatarUrl = decodedAvatarUrl,
                    onBackClick = { navController.popBackStack() },
                    onShowFollowers = { userId -> navController.navigate("followers/$userId") },
                    onShowFollowing = { userId -> navController.navigate("following/$userId") }
                )
            }

            composable(
                route = "followers/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                FollowListScreen(
                    userId = userId,
                    showFollowers = true,
                    onBackClick = { navController.popBackStack() },
                    onUserClick = { selectedUserId, username, avatarUrl ->
                        navigateToUserProfile(selectedUserId, username, avatarUrl)
                    }
                )
            }

            composable(
                route = "following/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                FollowListScreen(
                    userId = userId,
                    showFollowers = false,
                    onBackClick = { navController.popBackStack() },
                    onUserClick = { selectedUserId, username, avatarUrl ->
                        navigateToUserProfile(selectedUserId, username, avatarUrl)
                    }
                )
            }

            composable(Screen.GarmentInput.route) {
                GarmentInputScreen(
                    viewModel = garmentInputVm,
                    onOpenEditor = { navController.navigate(Screen.Editor.route) },
                    onBack = {
                        garmentInputVm.clearAll()
                        navController.popBackStack()
                    },
                )
            }
            composable(Screen.Editor.route) {
                EditorPlaceHolder(
                    viewModel = garmentInputVm,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
