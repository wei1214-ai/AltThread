package com.example.myapplicationkoG

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Upload : Screen("upload")
    object Home : Screen("home")
    object Search : Screen("search")
    object Studio : Screen("studio")
    object Profile : Screen("profile")

    // Part 1 editor flow
    object GarmentInput : Screen("garment_input")
    object Editor : Screen("editor")
}
