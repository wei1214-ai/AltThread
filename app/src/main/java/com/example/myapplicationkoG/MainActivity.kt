package com.example.myapplicationkoG

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplicationkoG.ui.theme.AltThreadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AltThreadTheme {
                AltThreadApp()
            }
        }
    }
}

@Composable
fun AltThreadApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // 只有 Home / Search / Studio / Profile 才显示底部导航栏
            if (currentRoute in bottomBarRoutes) {
                BottomNavBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        // innerPadding 已经把状态栏、导航栏、底部 NavigationBar 占用的空间都算好了
        // 每个 Screen 不需要再自己 statusBarsPadding()/navigationBarsPadding()
        NavHost(
            navController = navController,
            startDestination = Screen.Auth.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            // 把 Auth 从返回栈里移除，登录后按返回键不会又跳回登录页
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Upload.route) {
                UploadScreen(
                    onClose = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.Studio.route) { StudioScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
        }
    }
}