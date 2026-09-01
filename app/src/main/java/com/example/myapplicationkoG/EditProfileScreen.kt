package com.example.myapplicationkoG

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplicationkoG.ui.ProfileRepository
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    onBack: () -> Unit
) {
    val repository = remember { ProfileRepository() }
    val scope = rememberCoroutineScope()

    var username by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val profile = repository.getMyProfile()
            username = profile.username.orEmpty()
            bio = profile.bio.orEmpty()
        } catch (e: Exception) {
            errorMessage = e.message ?: "Could not load profile"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Edit profile")

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            enabled = !isSaving,
            onClick = {
                if (username.isBlank()) {
                    errorMessage = "Username cannot be empty"
                    return@Button
                }

                scope.launch {
                    try {
                        isSaving = true
                        repository.updateMyProfile(
                            username = username.trim(),
                            bio = bio.trim()
                        )
                        onBack()
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Could not save profile"
                    } finally {
                        isSaving = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSaving) "Saving..." else "Save profile")
        }

        if (errorMessage.isNotBlank()) {
            Text(errorMessage)
        }
    }
}