package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeviceEntity
import com.example.MainActivity
import com.example.ui.components.MatrixRainEffect
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldNeon

import com.example.util.BiometricPromptHelper

@Composable
fun PersistentDeviceLockScreen(
    localDevice: DeviceEntity,
    onUnlockAttempt: (pin: String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableIntStateOf(0) }

    // Sync lock state with MainActivity for anti-bypass home swipe protection
    DisposableEffect(Unit) {
        MainActivity.isLockActive = true
        onDispose {
            MainActivity.isLockActive = false
        }
    }

    // Intercept back press to prevent back gesture bypass
    BackHandler(enabled = true) {
        Toast.makeText(context, "🚨 DEVICE LOCKED BY ADMIN POLICY. Enter synced Master PIN or issue Online Unlock command.", Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        MatrixRainEffect(modifier = Modifier.fillMaxSize())

        // STICKY ADMIN KIOSK BANNER
        Surface(
            color = DangerRed.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🚨 STRICT KIOSK POLICY ACTIVE • PERSISTENT LOCKOUT ENFORCED BY ADMIN",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .padding(top = 56.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Warning Shield Icon
                Box(
                    modifier = Modifier
                        .background(DangerRed.copy(alpha = 0.2f), CircleShape)
                        .border(2.dp, DangerRed, CircleShape)
                        .padding(20.dp)
                ) {
                    Icon(
                        imageVector = if (localDevice.isLostMode) Icons.Filled.GppMaybe else Icons.Filled.Lock,
                        contentDescription = "Device Lock Active",
                        tint = DangerRed,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (localDevice.isLostMode) "STOLEN / LOST DEVICE LOCKOUT" else "SENTINEL-X HARDWARE LOCK",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = DangerRed,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = if (localDevice.isOnline) EmeraldNeon.copy(alpha = 0.15f) else AlertOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = if (localDevice.isOnline) "ONLINE • LISTENING FOR REMOTE UNLOCK COMMANDS" else "OFFLINE LOCK ACTIVE • ENTER OWNER PIN OR SCAN FINGERPRINT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (localDevice.isOnline) EmeraldNeon else AlertOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }

                // If Lost Mode Payload is present
                if (localDevice.isLostMode) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DangerRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "OWNER RECOVERY MESSAGE:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = localDevice.customLostMessage.ifEmpty { "This device is lost. Please contact owner immediately." },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            if (localDevice.customLostContact.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${localDevice.customLostContact}"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Contact: ${localDevice.customLostContact}", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = "Call Owner", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call Owner (${localDevice.customLostContact})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, AlertOrange.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.PushPin, contentDescription = null, tint = AlertOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PINNED ADMINISTRATOR KIOSK POLICY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertOrange,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (localDevice.customLostMessage.isNotEmpty()) localDevice.customLostMessage else "ADMIN MSG: Strict kiosk lockout policy is enforced on this secure workstation terminal. Tamper detection is armed. Intercepted signals are uploaded to central logs.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (showError) "INVALID PIN ($failedAttempts FAILED) • UNLOCK DENIED" else "ENTER 4-DIGIT ADMIN PIN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (showError) DangerRed else EmeraldNeon,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                // PIN display dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    repeat(4) { idx ->
                        val active = inputPin.length > idx
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    if (active) DangerRed else Color.Gray.copy(alpha = 0.3f),
                                    CircleShape
                                )
                                .border(1.dp, if (active) DangerRed else Color.Transparent, CircleShape)
                        )
                    }
                }

                // Grid 3x4 Keypad
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Clear", "0", "Unlock")
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(260.dp)
                ) {
                    keys.chunked(3).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
                                        .border(1.dp, DangerRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            when (key) {
                                                "Clear" -> {
                                                    inputPin = ""
                                                    showError = false
                                                }
                                                "Unlock" -> {
                                                    if (inputPin.isNotEmpty()) {
                                                        val success = onUnlockAttempt(inputPin)
                                                        if (!success) {
                                                            failedAttempts++
                                                            inputPin = ""
                                                            showError = true
                                                            Toast.makeText(context, "❌ Incorrect PIN! Failed unlock attempt #$failedAttempts logged.", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            inputPin = ""
                                                            showError = false
                                                            failedAttempts = 0
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    showError = false
                                                    if (inputPin.length < 4) {
                                                        inputPin += key
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("lock_screen_key_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Inspired by Branton",
                        color = EmeraldNeon.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Biometric Authentication Unlock Button
                    OutlinedButton(
                        onClick = {
                            BiometricPromptHelper.authenticate(
                                context = context,
                                title = "Biometric Lock Screen Unlock",
                                subtitle = "Authenticate to unlock device",
                                description = "Scan fingerprint or face to bypass lockout",
                                onSuccess = {
                                    onUnlockAttempt("2026")
                                    Toast.makeText(context, "Biometric authentication verified! Device unlocked.", Toast.LENGTH_SHORT).show()
                                },
                                onError = { errString ->
                                    Toast.makeText(context, "Biometric error: $errString", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = EmeraldNeon.copy(alpha = 0.15f),
                            contentColor = EmeraldNeon
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldNeon),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("biometric_lockscreen_unlock_btn")
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = "Biometric Unlock", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SCAN FINGERPRINT TO UNLOCK", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
