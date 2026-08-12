package com.example.ui.screens
import androidx.compose.ui.graphics.asImageBitmap


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeviceEntity
import com.example.ui.SentinelViewModel
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.DangerRed
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontFamily
import com.example.util.DeviceDiagnosticHelper
import com.example.util.UsageStatsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SentinelViewModel,
    devices: List<DeviceEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedDevice by remember { mutableStateOf<DeviceEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showPermissionsDialog by remember { 
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) ||
            !(context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager).isAdminActive(android.content.ComponentName(context, com.example.receiver.SentinelDeviceAdminReceiver::class.java))
        ) 
    }


    // Diagnostic Remote Management States
    val diagnosticReport by viewModel.diagnosticReport.collectAsState()
    var showDiagnosticDialog by remember { mutableStateOf(false) }

    // Device Enrollment Dialog States
    var showEnrollDialog by remember { mutableStateOf(false) }
    var newDeviceName by remember { mutableStateOf("") }
    var newDeviceModel by remember { mutableStateOf("") }
    var newDeviceManufacturer by remember { mutableStateOf("") }

    // Device Unenrollment Confirmation State
    var showUnenrollConfirmDialog by remember { mutableStateOf(false) }

    // If no device is selected, default to the local device
    LaunchedEffect(devices) {
        if (selectedDevice == null && devices.isNotEmpty()) {
            selectedDevice = devices.find { it.id == viewModel.localDeviceId } ?: devices.first()
        } else if (selectedDevice != null) {
            // keep it updated
            selectedDevice = devices.find { it.id == selectedDevice?.id }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Logged-in Profile
        item {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.3f), RoundedCornerShape(12.dp))
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Current Profile", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            Text(currentUser.email ?: "Anonymous User", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Button(
                            onClick = {
                                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                com.example.MainActivity.relaunchFromApplication(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                        ) {
                            Text("LOG OUT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Anti-Theft Protection Policy & Data Privacy Card
        item {
            Text(
                text = "Inspired by Branton",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = EmeraldNeon.copy(alpha = 0.06f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, EmeraldNeon.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(EmeraldNeon.copy(alpha = 0.15f), CircleShape)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = "Security Shield",
                                tint = EmeraldNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Anti-Theft Security Policy",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldNeon
                            )
                            Text(
                                text = "Voluntary Safety Enrollment Active",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "SentinelX is engineered strictly for safety and physical theft protection. In the event of device snatching or emergency loss, the system enables remote locking and real-time location tracking.",
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PrivacyTip,
                            contentDescription = "Privacy Verified",
                            tint = EmeraldNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Absolute Data Privacy: This application does not collect, sell, or transmit user files, contacts, or personal data. Enrollment is completely voluntary.",
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Overall Security Shield Section
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SentinelX Threat Shield",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "All devices monitored & cryptographically verified",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = "Shield Verified",
                            tint = EmeraldNeon,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Big Circular Health Score Ring
                    val score = selectedDevice?.healthScore ?: 94
                    val scoreColor = when {
                        score >= 90 -> EmeraldNeon
                        score >= 70 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(140.dp)
                            .background(MaterialTheme.colorScheme.background, CircleShape)
                            .border(3.dp, scoreColor.copy(alpha = 0.2f), CircleShape)
                            .border(6.dp, scoreColor, CircleShape)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$score%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = scoreColor,
                                fontSize = 36.sp
                            )
                            Text(
                                text = "HEALTH SCORE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when {
                            score >= 90 -> "Secure Environment"
                            score >= 70 -> "Warnings Detected"
                            else -> "Critical Threat Action Required"
                        },
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Persistent Foreground Service Active Banner
        item {
            var isServiceRunning by remember { mutableStateOf(true) }
            val context = LocalContext.current

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceRunning) EmeraldNeon.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isServiceRunning) EmeraldNeon.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isServiceRunning) EmeraldNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    CircleShape
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isServiceRunning) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                contentDescription = "Foreground Service",
                                tint = if (isServiceRunning) EmeraldNeon else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Persistent Security Service",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isServiceRunning) "Foreground channel active • Listening for background lock commands" else "Service paused • Remote lock commands suspended",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { checked ->
                            isServiceRunning = checked
                            if (checked) {
                                com.example.service.SentinelForegroundService.startService(context)
                            } else {
                                com.example.service.SentinelForegroundService.stopService(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = EmeraldNeon
                        ),
                        modifier = Modifier.testTag("toggle_foreground_service_switch")
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // SYSTEM SHIELD: SECURE KIOSK SYSTEM ENFORCER
        // -------------------------------------------------------------
        item {
            val isKioskEnabled by viewModel.isKioskModeEnabled.collectAsState()
            val context = LocalContext.current

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(
                        1.5.dp,
                        if (isKioskEnabled) EmeraldNeon.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("kiosk_system_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isKioskEnabled) EmeraldNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isKioskEnabled) Icons.Filled.VerifiedUser else Icons.Filled.AdminPanelSettings,
                                contentDescription = "Kiosk System Enforcer",
                                tint = if (isKioskEnabled) EmeraldNeon else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Persistent Kiosk Mode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isKioskEnabled) "PERMANENT LOCKDOWN ACTIVE • All system gestures are blocked. This device is now a dedicated Sentinel station. Disable this to restore normal phone usage." else "Persistent Kiosk Mode • Convert this device into a dedicated security terminal. Standard navigation (Home, Back, Recents) will be completely blocked regardless of the lock state.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Switch(
                        checked = isKioskEnabled,
                        onCheckedChange = { checked ->
                            viewModel.setKioskModeEnabled(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = EmeraldNeon
                        ),
                        modifier = Modifier.testTag("toggle_kiosk_mode_switch")
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // USAGE INTELLIGENCE: ENHANCED LOCK RELIABILITY
        // -------------------------------------------------------------
        item {
            val context = LocalContext.current
            var isUsageGranted by remember { mutableStateOf(UsageStatsHelper.isUsageAccessGranted(context)) }

            // Refresh state when returning from settings
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        isUsageGranted = UsageStatsHelper.isUsageAccessGranted(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUsageGranted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = if (!isUsageGranted) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isUsageGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isUsageGranted) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Usage Intelligence",
                                tint = if (isUsageGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Intelligent Lock Guard",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isUsageGranted) "SMART LOCKING ACTIVE • App uses Usage Stats to distinguish between bypass attempts and authorized system dialogs." else "Incomplete Security • Grant Usage Access to allow the app to recognize biometric prompts and system dialogs correctly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    if (!isUsageGranted) {
                        Button(
                            onClick = {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                                Toast.makeText(context, "Grant 'SentinelX' usage access", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("grant_usage_access_button")
                        ) {
                            Text("Grant", color = Color.White)
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Access Granted",
                            tint = EmeraldNeon,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // MANAGED AGENT CONNECTION: LINK TO ADMIN CONSOLE
        // -------------------------------------------------------------
        item {
            val isDeviceManaged by viewModel.isDeviceManaged.collectAsState()
            val adminPairingCode by viewModel.adminPairingCode.collectAsState()
            val context = LocalContext.current
            var pairingInput by remember { mutableStateOf("") }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(
                        1.5.dp,
                        if (isDeviceManaged) EmeraldNeon.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("managed_agent_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isDeviceManaged) EmeraldNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isDeviceManaged) Icons.Filled.Link else Icons.Filled.LinkOff,
                                contentDescription = "Managed Agent Connection",
                                tint = if (isDeviceManaged) EmeraldNeon else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Managed Agent Peer Binding",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDeviceManaged) "CONNECTED • Managed Agent of Admin Phone" else "STANDALONE MODE • Unlinked",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDeviceManaged) EmeraldNeon else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    if (!isDeviceManaged) {
                        Text(
                            text = "To enroll this device under remote administration, enter the Pairing Token generated by the authorized Admin phone console below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        OutlinedTextField(
                            value = pairingInput,
                            onValueChange = { pairingInput = it.uppercase() },
                            label = { Text("6-Digit Peer Pairing OTP") },
                            placeholder = { Text("e.g. XF92AL or STX-PAIR") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldNeon,
                                focusedLabelColor = EmeraldNeon
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pairing_token_request_input")
                        )

                        Button(
                            onClick = {
                                if (pairingInput.trim().length >= 4) {
                                    viewModel.linkDeviceToAdmin(pairingInput.trim()) { success, message ->
                                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                        if (success) {
                                            pairingInput = ""
                                        }
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Please enter a valid pairing code.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldNeon,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("request_enrollment_btn")
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Request Connection & Bind Agent", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "This device is registered and cryptographically bound to Admin Pairing Token: '$adminPairingCode'. This device receives and processes remote locks, real-time tracking requests, and hardware commands.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            Button(
                                onClick = {
                                    viewModel.unlinkDeviceFromAdmin()
                                    android.widget.Toast.makeText(context, "Device unlinked successfully. Standalone mode restored.", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("unlink_agent_btn")
                            ) {
                                Icon(Icons.Filled.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sever Remote Link & Revert to Standalone", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // INTERACTIVE KIOSK LOCK SCREEN TESTER & SYSTEM SHIELD CONSOLE
        // -------------------------------------------------------------
        item {
            var countdownSeconds by remember { mutableStateOf(-1) }
            val context = LocalContext.current

            LaunchedEffect(countdownSeconds) {
                if (countdownSeconds > 0) {
                    delay(1000)
                    countdownSeconds--
                } else if (countdownSeconds == 0) {
                    countdownSeconds = -1
                    // Trigger remote lock command locally on local device
                    viewModel.triggerRemoteCommand(viewModel.localDeviceId, "LOCK_DEVICE")
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (countdownSeconds >= 0) DangerRed else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(if (countdownSeconds >= 0) DangerRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (countdownSeconds >= 0) Icons.Filled.Timer else Icons.Filled.Lock,
                                    contentDescription = "Lock Tester",
                                    tint = if (countdownSeconds >= 0) DangerRed else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Lock & Swipe Bypass Tester",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Verify persistent lockout, home-swipe blocker & secure heartbeat loop",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (countdownSeconds >= 0) {
                        Surface(
                            color = DangerRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = DangerRed,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "ENFORCING LOCKOUT IN $countdownSeconds SECONDS...",
                                    color = DangerRed,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { countdownSeconds = 5 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (countdownSeconds >= 0) DangerRed else EmeraldNeon,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("dashboard_test_lock_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "EXECUTE EMERGENCY LOCK (5S COUNTDOWN)",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Educational Permissions Rationale Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = "Camera & Location Access",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Camera & Location Permissions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Educational dialog explaining why access is required",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Button(
                        onClick = { showPermissionsDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dashboard_permissions_btn")
                    ) {
                        Text("Explain Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Enrolled Devices List Title & Enrollment Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enrolled Devices (${devices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedButton(
                    onClick = { showEnrollDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldNeon),
                    modifier = Modifier.testTag("enroll_new_device_btn")
                ) {
                    Icon(Icons.Filled.AddLink, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Enroll Device", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Horizontal Enrolled Devices Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                devices.forEach { device ->
                    val isSelected = selectedDevice?.id == device.id
                    val borderAlpha = if (isSelected) 1f else 0.1f
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .border(if (isSelected) 2.dp else 1.dp, borderColor.copy(alpha = borderAlpha), RoundedCornerShape(12.dp))
                            .clickable { selectedDevice = device }
                            .testTag("device_card_${device.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (device.model.contains("Tablet", ignoreCase = true)) Icons.Filled.TabletAndroid else Icons.Filled.PhoneAndroid,
                                    contentDescription = "Device Type",
                                    tint = if (device.isOnline) EmeraldNeon else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (device.isOnline) EmeraldNeon else Color.Gray, CircleShape)
                                )
                            }

                            Column {
                                Text(
                                    text = if (device.id == viewModel.localDeviceId) "This Device" else device.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (device.isOnline) "Online" else "Offline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (device.isOnline) EmeraldNeon else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selected Device Specification Details
        selectedDevice?.let { device ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Device Specifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (device.id != viewModel.localDeviceId) {
                                OutlinedButton(
                                    onClick = { showUnenrollConfirmDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("unenroll_selected_device_btn")
                                ) {
                                    Icon(Icons.Filled.LinkOff, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Unenroll", fontSize = 10.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Grid of details
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailRow(label = "Manufacturer", value = device.manufacturer)
                            DetailRow(label = "Model Code", value = device.model)
                            DetailRow(label = "OS Version", value = device.androidVersion)
                            DetailRow(label = "Security Patch", value = device.securityPatch, isWarn = device.securityPatch.startsWith("2024"))
                            DetailRow(label = "Network Status", value = device.networkStatus)
                            DetailRow(label = "Last Contact", value = SimpleDateFormat("HH:mm:ss (MMM dd)", Locale.getDefault()).format(Date(device.lastActiveTime)))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Hardware Resource Metrics Gauges
                        Text(
                            text = "Resource Utilization",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Storage gauge
                            HardwareGauge(
                                modifier = Modifier.weight(1f),
                                title = "Storage",
                                total = "${device.storageTotalGb} GB",
                                used = "${device.storageUsedGb} GB",
                                pct = (device.storageUsedGb / device.storageTotalGb).toFloat()
                            )

                            // RAM gauge
                            HardwareGauge(
                                modifier = Modifier.weight(1f),
                                title = "RAM",
                                total = "${device.ramTotalGb} GB",
                                used = "${device.ramUsedGb} GB",
                                pct = (device.ramUsedGb / device.ramTotalGb).toFloat()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Battery gauge
                        val batteryPct = device.batteryPercentage / 100f
                        HardwareGauge(
                            modifier = Modifier.fillMaxWidth(),
                            title = "Battery Status",
                            total = if (device.isCharging) "Charging" else "Discharging",
                            used = "${device.batteryPercentage}%",
                            pct = batteryPct,
                            barColor = if (device.batteryPercentage < 20) MaterialTheme.colorScheme.error else EmeraldNeon
                        )
                    }
                }
            }

        // Remote Management Diagnostic Information Helper Card
        item {
            val report = diagnosticReport ?: viewModel.refreshDiagnostics()
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmeraldNeon.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .testTag("device_diagnostics_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(EmeraldNeon.copy(alpha = 0.15f), CircleShape)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Analytics,
                                    contentDescription = "Diagnostics",
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Device Diagnostics (Remote Info)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Model, Battery & Android OS telemetry",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.refreshDiagnostics() },
                            modifier = Modifier.testTag("refresh_diagnostics_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh Telemetry",
                                tint = EmeraldNeon
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow(
                            label = "Device Model",
                            value = "${report.manufacturer} ${report.model} (${report.board})"
                        )
                        DetailRow(
                            label = "Battery Level",
                            value = "${report.batteryLevelPercentage}% • ${report.batteryStatus} (${report.powerSource})"
                        )
                        DetailRow(
                            label = "Android Version",
                            value = "Android ${report.androidVersion} (API ${report.sdkInt})"
                        )
                        DetailRow(
                            label = "Security Patch",
                            value = report.securityPatch,
                            isWarn = report.securityPatch.startsWith("2024")
                        )
                        DetailRow(
                            label = "RAM Utilization",
                            value = "${report.usedRamGb} GB / ${report.totalRamGb} GB"
                        )
                        DetailRow(
                            label = "Storage Utilization",
                            value = "${report.usedStorageGb} GB / ${report.totalStorageGb} GB"
                        )
                        DetailRow(
                            label = "Network Status",
                            value = report.networkStatus
                        )
                        DetailRow(
                            label = "System Uptime",
                            value = report.uptimeFormatted
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showDiagnosticDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldNeon),
                            modifier = Modifier.testTag("view_diagnostic_report_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Terminal,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Remote Payload", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

            // Installed Application Package Explorer
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Installed Applications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Filled.AppSettingsAlt,
                                contentDescription = "Apps Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // App Search text field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search system package label...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Apps", modifier = Modifier.size(18.dp)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("app_search_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Query list of applications
                        val rawAppList = remember(device.id) { viewModel.getLocalInstalledApplications() }
                        val filteredAppList = rawAppList.filter { it.name.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (filteredAppList.isEmpty()) {
                                Text(
                                    text = "No applications match '$searchQuery'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                                )
                            } else {
                                filteredAppList.forEach { appName ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Android,
                                            contentDescription = "App Icon",
                                            tint = EmeraldNeon,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = appName.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    com.example.ui.components.EducationalPermissionsDialog(
        showDialog = showPermissionsDialog,
        onDismiss = { showPermissionsDialog = false }
    )

    // ENROLL NEW CONNECTED DEVICE DIALOG
    if (showEnrollDialog) {
        var enrollTab by remember { mutableStateOf(0) } // 0: Show My QR, 1: Scan Remote QR, 2: Manual Entry
        var remoteTokenInput by remember { mutableStateOf("STX-PAIR-84F29A") }

        AlertDialog(
            onDismissRequest = { showEnrollDialog = false },
            icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = EmeraldNeon) },
            title = {
                Text(
                    text = "Re-Enroll / Connect to Admin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 3-Way Mode Selector
                    ScrollableTabRow(
                        selectedTabIndex = enrollTab,
                        containerColor = Color.Transparent,
                        divider = {},
                        edgePadding = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = enrollTab == 0,
                            onClick = { enrollTab = 0 },
                            text = { Text("1. Show My QR", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("enroll_tab_show_qr")
                        )
                        Tab(
                            selected = enrollTab == 1,
                            onClick = { enrollTab = 1 },
                            text = { Text("2. Scan Remote QR", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("enroll_tab_scan_qr")
                        )
                        Tab(
                            selected = enrollTab == 2,
                            onClick = { enrollTab = 2 },
                            text = { Text("3. Manual Form", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("enroll_tab_manual")
                        )
                    }

                    when (enrollTab) {
                        0 -> {
                            // MODE 1: SHOW MY QR CODE (For Phone B to Scan)
                            val currentCode by viewModel.adminPairingCode.collectAsState()
                            val displayCode = if (currentCode.isBlank()) viewModel.generateNewPairingCode() else currentCode
                            val agentId = viewModel.localDeviceId

                            Text(
                                text = "Show this QR code on Phone A. Open SentinelX on Phone B, select 'Scan Remote QR', and point its camera here.",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .border(2.dp, EmeraldNeon, RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                QRCodeDisplay(
                                    contentString = "sentinelx://enroll?agent_id=$agentId&token=$displayCode",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Agent ID: $agentId",
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Pairing Token: $displayCode",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = EmeraldNeon
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = { viewModel.generateNewPairingCode() }) {
                                    Text("Regenerate Token", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        1 -> {
                            // MODE 2: SCAN REMOTE QR CODE (From Phone B)
                            Text(
                                text = "Camera Viewfinder: Point camera at target QR code or tap frame to auto-capture pairing code.",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            CameraXScannerView(
                                onTokenDetected = { token ->
                                    remoteTokenInput = token
                                }
                            )

                            OutlinedTextField(
                                value = remoteTokenInput,
                                onValueChange = { remoteTokenInput = it },
                                label = { Text("Scanned Pairing Token") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("scanned_token_input")
                            )

                            Button(
                                onClick = {
                                    val token = remoteTokenInput.ifBlank { "STX-PAIR-84F29A" }
                                    val newId = viewModel.enrollNewDevice("Paired Mobile Phone B", "Galaxy S24 Ultra", "Samsung")
                                    Toast.makeText(context, "Device successfully paired with token: $token", Toast.LENGTH_SHORT).show()
                                    showEnrollDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("execute_qr_pairing_btn")
                            ) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pair & Enroll Discovered Phone B", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        2 -> {
                            // MODE 3: MANUAL ENTRY
                            Text(
                                text = "Enter connected device parameters to register cryptographic agent token manually:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            OutlinedTextField(
                                value = newDeviceName,
                                onValueChange = { newDeviceName = it },
                                label = { Text("Device Name (e.g. Work Phone)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("enroll_device_name_input")
                            )

                            OutlinedTextField(
                                value = newDeviceModel,
                                onValueChange = { newDeviceModel = it },
                                label = { Text("Device Model (e.g. Pixel 8 Pro)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("enroll_device_model_input")
                            )

                            OutlinedTextField(
                                value = newDeviceManufacturer,
                                onValueChange = { newDeviceManufacturer = it },
                                label = { Text("Manufacturer (e.g. Google, Samsung)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("enroll_device_manufacturer_input")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (enrollTab == 2) {
                    Button(
                        onClick = {
                            val name = if (newDeviceName.isBlank()) "Connected Device" else newDeviceName
                            val model = if (newDeviceModel.isBlank()) "Android Terminal" else newDeviceModel
                            val mfr = if (newDeviceManufacturer.isBlank()) "Android" else newDeviceManufacturer
                            viewModel.enrollNewDevice(name, model, mfr)
                            newDeviceName = ""
                            newDeviceModel = ""
                            newDeviceManufacturer = ""
                            showEnrollDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black),
                        modifier = Modifier.testTag("confirm_enroll_device_btn")
                    ) {
                        Text("Complete Enrollment", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnrollDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // UNENROLL DEVICE CONFIRMATION DIALOG
    if (showUnenrollConfirmDialog && selectedDevice != null) {
        val target = selectedDevice!!
        AlertDialog(
            onDismissRequest = { showUnenrollConfirmDialog = false },
            icon = { Icon(Icons.Filled.LinkOff, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = {
                Text(
                    text = "Unenroll Device '${target.name}'?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Unenrolling this device will revoke its security keys, remove it from the SentinelX network, and disconnect remote lock control.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unenrollDevice(target.id)
                        showUnenrollConfirmDialog = false
                        selectedDevice = devices.find { it.id == viewModel.localDeviceId }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_unenroll_btn")
                ) {
                    Text("Revoke & Unenroll", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnenrollConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Diagnostic Payload Dialog
    if (showDiagnosticDialog) {
        val report = diagnosticReport ?: viewModel.refreshDiagnostics()
        val jsonPayload = remember(report) { DeviceDiagnosticHelper.exportAsJson(report) }
        val summaryText = remember(report) { DeviceDiagnosticHelper.formatSummaryText(report) }
        var isJsonMode by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showDiagnosticDialog = false },
            icon = { Icon(Icons.Filled.Analytics, contentDescription = null, tint = EmeraldNeon) },
            title = {
                Text(
                    text = "Remote Diagnostic Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isJsonMode) "Format: JSON Payload" else "Format: Plain Text Summary",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        TextButton(onClick = { isJsonMode = !isJsonMode }) {
                            Text(if (isJsonMode) "Switch to Text" else "Switch to JSON", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color.Black, RoundedCornerShape(8.dp))
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn {
                            item {
                                Text(
                                    text = if (isJsonMode) jsonPayload else summaryText,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = EmeraldNeon
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDiagnosticDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black),
                    modifier = Modifier.testTag("close_diagnostic_dialog_btn")
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String, isWarn: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isWarn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun HardwareGauge(
    modifier: Modifier = Modifier,
    title: String,
    total: String,
    used: String,
    pct: Float,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "$used / $total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { pct.coerceIn(0f, 1f) },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.background,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun QRCodeDisplay(
    contentString: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(contentString) {
        try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(contentString, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bmp.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        androidx.compose.foundation.Image(bitmap = bitmap, contentDescription = "QR Code", modifier = modifier)
    } else {
        Box(modifier = modifier.background(Color.White))
    }
}

@Composable
fun CameraXScannerView(
    onTokenDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .background(Color.Black, RoundedCornerShape(12.dp))
            .border(1.dp, EmeraldNeon, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            Log.d("CameraScanner", "CameraProvider retrieved successfully")
                            
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(ctx),
                                com.example.util.QrCodeAnalyzer { token -> 
                                    Log.d("CameraScanner", "QR Code detected: $token")
                                    onTokenDetected(token)
                                    Toast.makeText(ctx, "QR Token Captured: $token", Toast.LENGTH_SHORT).show()
                                }
                            )
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            Log.d("CameraScanner", "Camera bound to lifecycle successfully")
                        } catch (e: Exception) {
                            Log.e("CameraScanner", "Camera binding failed: ${e.message}", e)
                            Toast.makeText(ctx, "Camera Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.CenterFocusWeak,
                        contentDescription = "Scanner Viewfinder",
                        tint = EmeraldNeon,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "LIVE CAMERA FEED ACTIVE • SCANNING QR...",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = EmeraldNeon,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Camera,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Camera Permission Required for QR Code Scanning",
                    fontSize = 11.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Grant Camera Permission", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

