package com.example

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatActivity
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SlateDark
import com.example.util.BiometricPromptHelper

class ManageSpaceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SlateDark
                ) {
                    ManageSpaceScreen(
                        onBackPressed = { finish() },
                        onAuthorizedWipe = { performWipe() }
                    )
                }
            }
        }
    }

    private fun performWipe() {
        try {
            Toast.makeText(this, "Security Authentication Succeeded! Restructuring space...", Toast.LENGTH_LONG).show()
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.clearApplicationUserData()
        } catch (e: Exception) {
            Toast.makeText(this, "Error resetting application state: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSpaceScreen(
    onBackPressed: () -> Unit,
    onAuthorizedWipe: () -> Unit
) {
    val context = LocalContext.current
    var passcode by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isWipeDialogVisible by remember { mutableStateOf(false) }

    fun triggerBiometricAuth() {
        BiometricPromptHelper.authenticate(
            context = context,
            title = "Anti-Theft Storage Protection",
            subtitle = "Authorize Cache/Data Clearance",
            description = "Confirm fingerprint or face template to gain database access.",
            onSuccess = {
                errorMsg = null
                isWipeDialogVisible = true
            },
            onError = { err ->
                errorMsg = "Biometric Auth Failed: $err. Please use passcode fallback."
            }
        )
    }

    LaunchedEffect(Unit) {
        if (BiometricPromptHelper.canAuthenticate(context)) {
            triggerBiometricAuth()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "STORAGE LOCKDOWN",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp,
                        color = EmeraldNeon
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EmeraldNeon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateDark)
            )
        },
        containerColor = SlateDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Pulse Warning Icon Shield
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(DangerRed.copy(alpha = 0.1f), CircleShape)
                    .border(2.dp, DangerRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Lock",
                    tint = DangerRed,
                    modifier = Modifier.size(45.dp)
                )
            }

            Text(
                text = "Anti-Theft Clearance Protection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "A critical security system protects SentinelX data. Thieves often attempt to bypass tracking and local device locks by clearing the application storage, cache, or database. To prevent this, data deletion is locked under cryptographic administrator authorization.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

            if (BiometricPromptHelper.canAuthenticate(context)) {
                Button(
                    onClick = { triggerBiometricAuth() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("biometric_space_btn")
                ) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock with Fingerprint / Face", fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "— OR USE PASSCODE —",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Enter Security Master Passcode",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldNeon
                )

                OutlinedTextField(
                    value = passcode,
                    onValueChange = {
                        passcode = it
                        errorMsg = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("space_passcode_input"),
                    placeholder = { Text("Enter Admin Passcode or Recovery Token", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldNeon,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                        focusedLabelColor = EmeraldNeon,
                        cursorColor = EmeraldNeon,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    visualTransformation = PasswordVisualTransformation()
                )

                errorMsg?.let {
                    Text(
                        text = it,
                        color = DangerRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp
                    )
                }
            }

            Button(
                onClick = {
                    if (com.example.util.PolicyEnforcementManager.authorizeLocalUnlock(context, passcode)) {
                        errorMsg = null
                        isWipeDialogVisible = true
                    } else {
                        errorMsg = "INVALID SECURITY PASSCODE! Please enter the correct master PIN."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_passcode_space_btn")
            ) {
                Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verify Passcode & Access", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Trust badge info
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Your safety is fully secure. No background telemetry or custom files are shared. Real-time active tracking and remote locking are user-authorized only.",
                        fontSize = 9.5.sp,
                        lineHeight = 13.sp,
                        color = Color.LightGray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (isWipeDialogVisible) {
        AlertDialog(
            onDismissRequest = { isWipeDialogVisible = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = DangerRed)
                    Text("CONFIRM FACTORY WIPE", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "Are you absolutely sure you want to completely erase the SentinelX anti-theft database and local caches? Doing so will permanently delete local threat history and reset configuration files.",
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isWipeDialogVisible = false
                        onAuthorizedWipe()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_wipe_btn")
                ) {
                    Text("Erase Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isWipeDialogVisible = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = SlateDark,
            tonalElevation = 6.dp
        )
    }
}
