package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            
            Text(
                text = "SENTINEL-X MDM",
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Account Authorization",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Text(
                text = "Log in to link devices to your account",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Account Email") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (min 6 chars)") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || password.length < 6) {
                        Toast.makeText(context, "Valid email and 6-char password required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    try {
                        val auth = FirebaseAuth.getInstance()
                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "System Authorized!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            }
                            .addOnFailureListener { e ->
                                val msg = e.message ?: ""
                                if (msg.contains("CONFIGURATION_NOT_FOUND")) {
                                    isLoading = false
                                    Toast.makeText(context, "ERROR: Please enable 'Email/Password' Sign-In in Firebase", Toast.LENGTH_LONG).show()
                                } else {
                                    // Try registering automatically if login failed (e.g. user not found)
                                    auth.createUserWithEmailAndPassword(email.trim(), password)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            Toast.makeText(context, "Account Created & Linked!", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        }
                                        .addOnFailureListener { re ->
                                            isLoading = false
                                            Toast.makeText(context, "Auth failed. If testing on emulator, use Bypass.", Toast.LENGTH_LONG).show()
                                        }
                                }
                            }
                    } catch (t: Throwable) {
                        isLoading = false
                        Toast.makeText(context, "Error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                } else {
                    Text("LOGIN / REGISTER", fontWeight = FontWeight.Bold)
                }
            }

            TextButton(
                onClick = { onLoginSuccess() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Bypass Login (Dev Mode)", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
