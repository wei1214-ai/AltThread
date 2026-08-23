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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplicationkoG.ui.theme.Cyan
import com.example.myapplicationkoG.ui.theme.LightGray
import com.example.myapplicationkoG.ui.theme.MidnightBlue
import androidx.compose.runtime.rememberCoroutineScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch


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
    value: String,
    onValueChange: (String)-> Unit,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {Text(placeholder)},
        singleLine = true,
        visualTransformation =
            if (isPassword) PasswordVisualTransformation()
            else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password
            else KeyboardType.Email
        ),
        modifier = modifier.fillMaxWidth()
    )
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
fun AuthScreen(
    onLoginSuccess: () -> Unit = {}
) {
    var isLogin by remember { mutableStateOf( true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope ()


    fun validateLogin(): Boolean{
        errorMessage = when{
            email.isBlank()-> "Email is required"
            password.isBlank()->"Password is required"
            else -> ""}

        return errorMessage.isEmpty()
    }

    fun validateRegister(): Boolean{
        errorMessage = when{
            email.isBlank()->"Email is required."
            !email.contains("@")-> "Enter valid email address."
            password.isBlank()-> "Password is required."
            password.length<6 -> "Password must be at least 6 character."
            confirmPassword.isBlank()->"Please confirm you password."
            password != confirmPassword->"Password does not match."
            else ->""
        }
        return errorMessage.isEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    AuthInputField(
                        placeholder = "Email Address",
                        value = email,
                        onValueChange = {email = it}
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    AuthInputField(placeholder = "Password",
                        value = password,
                        onValueChange = {password=it}
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    AuthPrimaryButton(text = "Log In",
                        onClick = {
                            if(validateLogin()) {
                                val enteredEmail = email
                                val enteredPassword = password
                                scope.launch {
                                    try {
                                        supabase.auth.signInWith(Email){
                                            email = enteredEmail
                                            password= enteredPassword
                                        }
                                        onLoginSuccess()
                                    }catch (exception: Exception){
                                        errorMessage = "Incorrect email or password."
                                    }
                                }
                            }
                        }
                    )
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
                    AuthInputField(placeholder = "Email address",
                        value = email,
                        onValueChange = {email = it})
                    Spacer(modifier = Modifier.height(16.dp))

                    AuthInputField(placeholder = "Password",
                        value = password,
                        onValueChange = {password = it},
                        isPassword = true)
                    Spacer(modifier = Modifier.height(16.dp))

                    AuthInputField(placeholder = "Confirm Password",
                        value = confirmPassword,
                        onValueChange = {confirmPassword = it},
                        isPassword = true)
                    Spacer(modifier = Modifier.height(16.dp))

                    AuthPrimaryButton(text = "Create Account",
                        onClick = {
                            if (validateRegister()){
                                val enteredEmail = email
                                val enteredPassword = password
                                scope.launch {
                                    try {
                                        supabase.auth.signUpWith(Email){
                                            email = enteredEmail
                                            password = enteredPassword
                                        }
                                        errorMessage = " Account created. Check your email to confirm it."
                                    } catch (exception: Exception){
                                        errorMessage= exception.message ?:"Could not create account."
                                    }
                                }
                            }
                        }
                    )
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