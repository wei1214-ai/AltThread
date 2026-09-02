package com.example.myapplicationkoG

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.example.myapplicationkoG.ui.editor.EditorPlaceHolder
import com.example.myapplicationkoG.ui.garmentinput.GarmentInputScreen
import com.example.myapplicationkoG.ui.garmentinput.GarmentInputViewModel
import com.example.myapplicationkoG.ui.theme.AltThreadTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {

    // Shared by GarmentInputScreen and EditorPlaceHolder so the cutout
    // paths persist across navigation.
    private val garmentInputViewModel: GarmentInputViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supabase.handleDeeplinks(intent)
        enableEdgeToEdge()
        setContent {
            AltThreadTheme {
                AltThreadApp(garmentInputViewModel)
            }
        }
    }
}

@Composable
fun AltThreadApp(garmentInputVm: GarmentInputViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val startDestination =
        if (supabase.auth.currentSessionOrNull() != null) {
            Screen.Home.route
        } else {
            Screen.Auth.route
        }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute in bottomBarRoutes && currentRoute != Screen.Upload.route) {
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
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.Studio.route) {
                StudioScreen(
                    onStartDesign = { navController.navigate(Screen.GarmentInput.route) }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onEditProfile = { navController.navigate(Screen.EditProfile.route) }
                )
            }
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // ---- Start Your Own Design ----
            composable(Screen.GarmentInput.route) {
                GarmentInputScreen(
                    viewModel = garmentInputVm,
                    onOpenEditor = { navController.navigate(Screen.Editor.route) },
                    onBack = { navController.popBackStack() },
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