package com.example.ui

import androidx.compose.runtime.*
import com.example.ui.screens.LoginScreen

@Composable
fun AuthWrapper(
    content: @Composable () -> Unit
) {
    var isUserLoggedIn by remember { 
        mutableStateOf(
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
            } catch (e: Throwable) {
                // Firebase not initialized or error - default to showing login or local mode
                false
            }
        ) 
    }

    if (isUserLoggedIn) {
        content()
    } else {
        LoginScreen(
            onLoginSuccess = {
                isUserLoggedIn = true
            }
        )
    }
}
