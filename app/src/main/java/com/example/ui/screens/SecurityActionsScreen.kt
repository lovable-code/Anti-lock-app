package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeviceEntity
import com.example.ui.SentinelViewModel
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldNeon
import com.example.util.DeviceAdminHelper
import com.example.util.BiometricPromptHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityActionsScreen(
    viewModel: SentinelViewModel,
    devices: List<DeviceEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAdminActive by remember { mutableStateOf(viewModel.isDeviceAdminActive()) }
    var hasOverlayPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }

    // Filter selection for enrolled devices: 0 = All, 1 = Online, 2 = Offline
    var deviceFilterTab by remember { mutableStateOf(0) }

    // Dialog controllers
    var selectedDeviceForLock by remember { mutableStateOf<DeviceEntity?>(null) }
    var showLockDialog by remember { mutableStateOf(false) }
    var showLostModeDialog by remember { mutableStateOf(false) }
    var showWipeDialog by remember { mutableStateOf(false) }
    var showEmergencyLockAllDialog by remember { mutableStateOf(false) }
    var showOfflineQueueNoticeDialog by remember { mutableStateOf<DeviceEntity?>(null) }

    // Form inputs
    var ownerPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var lostMessage by remember { mutableStateOf("This device is lost. Please contact the owner immediately.") }
    var lostContact by remember { mutableStateOf("+1-555-0199") }

    var wipeConfirmCheck1 by remember { mutableStateOf(false) }
    var wipeConfirmCheck2 by remember { mutableStateOf(false) }
    var wipePhraseInput by remember { mutableStateOf("") }

    // Filtered devices list
    val filteredDevices = when (deviceFilterTab) {
        1 -> devices.filter { it.isOnline }
        2 -> devices.filter { !it.isOnline }
        else -> devices
    }

    val onlineCount = devices.count { it.isOnline }
    val offlineCount = devices.count { !it.isOnline }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =========================================================================
        // SECTION 1: DEVICE ADMIN PRIVILEGES ACTIVATION & SECURITY EXPLANATION
        // =========================================================================
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isAdminActive) EmeraldNeon.copy(alpha = 0.08f) else AlertOrange.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (isAdminActive) EmeraldNeon.copy(alpha = 0.5f) else AlertOrange,
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("device_admin_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header row with status badge & refresh
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isAdminActive) EmeraldNeon.copy(alpha = 0.2f) else AlertOrange.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = "Device Admin Policy",
                                    tint = if (isAdminActive) EmeraldNeon else AlertOrange,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "DeviceAdminReceiver Policy",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = if (isAdminActive) EmeraldNeon.copy(alpha = 0.2f) else AlertOrange.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = if (isAdminActive) "● PRIVILEGE ACTIVE" else "▲ PRIVILEGE REQUIRED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isAdminActive) EmeraldNeon else AlertOrange,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                isAdminActive = viewModel.isDeviceAdminActive()
                                Toast.makeText(context, "Device Admin status updated", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("check_admin_status_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh Status",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Explanation Box: Why Device Admin is necessary for security
                    Text(
                        text = "Why Device Admin privileges are required for security:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeviceAdminRationaleItem(
                            icon = Icons.Filled.Lock,
                            title = "Instant Remote Screen Lock (force-lock)",
                            description = "Enables direct call to DevicePolicyManager.lockNow() to lock the screen immediately upon theft signal."
                        )
                        DeviceAdminRationaleItem(
                            icon = Icons.Filled.Shield,
                            title = "Anti-Tamper & Anti-Uninstall Protection",
                            description = "Prevents unauthorized users, thieves, or malicious malware from force-stopping or uninstalling Sentinel-X."
                        )
                        DeviceAdminRationaleItem(
                            icon = Icons.Filled.Password,
                            title = "Passcode & Lockout Enforcement",
                            description = "Enforces lock screen passcode requirements and security lockout policies after repeated failed attempts."
                        )
                        DeviceAdminRationaleItem(
                            icon = Icons.Filled.DeleteForever,
                            title = "Remote Cryptographic Hardware Wipe",
                            description = "Allows zeroization of sensitive files and storage memory if the device is lost or compromised."
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!isAdminActive) {
                            Button(
                                onClick = {
                                    context.startActivity(DeviceAdminHelper.getRequestAdminIntent(context))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AlertOrange,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("activate_device_admin_btn")
                            ) {
                                Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Activate Device Admin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val success = viewModel.deactivateDeviceAdmin()
                                    isAdminActive = viewModel.isDeviceAdminActive()
                                    if (success) {
                                        Toast.makeText(context, "Device Admin privilege deactivated successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to deactivate Device Admin", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("deactivate_device_admin_btn")
                            ) {
                                Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Deactivate Admin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                val success = viewModel.triggerProgrammaticScreenLock()
                                isAdminActive = viewModel.isDeviceAdminActive()
                                if (!success) {
                                    Toast.makeText(context, "Please enable Device Admin privilege first", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAdminActive) EmeraldNeon else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("trigger_screen_lock_btn")
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock Local Screen Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 1B: BIOMETRIC AUTHENTICATION HARDWARE ENGINE STATUS
        // =========================================================================
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = EmeraldNeon.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, EmeraldNeon.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .testTag("biometric_status_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(EmeraldNeon.copy(alpha = 0.2f), CircleShape)
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Fingerprint,
                                    contentDescription = "Biometric Security",
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "BiometricPrompt API Engine",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = BiometricPromptHelper.getBiometricStatus(context),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldNeon
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                BiometricPromptHelper.authenticate(
                                    context = context,
                                    title = "Test Biometric Prompt",
                                    subtitle = "Hardware Verification Test",
                                    description = "Scan fingerprint or face to test BiometricPrompt sensor integration",
                                    onSuccess = {
                                        Toast.makeText(context, "Biometric Verification Succeeded! Sensor active.", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "Biometric Test: $err", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("test_biometric_scan_btn")
                        ) {
                            Text("Test Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 1C: ADVANCED HARDWARE CONTAINMENT & TAMPER PROTECTION
        // =========================================================================
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = EmeraldNeon.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, EmeraldNeon.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .testTag("hardware_containment_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(EmeraldNeon.copy(alpha = 0.2f), CircleShape)
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PowerOff,
                                    contentDescription = "Hardware Tamper Protection",
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Advanced Hardware Containment",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "● HARDWARE TAMPER MONITOR ACTIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldNeon
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Active anti-theft containment policies currently enforced:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeviceAdminRationaleItem(
                            icon = Icons.Filled.Usb,
                            title = "Power Unplug Containment Guard",
                            description = "Plays maximum volume CDMA emergency siren instantly if charger/USB is disconnected in Lost/Lock mode."
                        )
                        DeviceAdminRationaleItem(
                            icon = Icons.Filled.AirplanemodeActive,
                            title = "Airplane Mode Anti-Isolation Guard",
                            description = "Sounds emergency alert sirens if network isolation (Airplane Mode) is toggled in Lost/Lock mode."
                        )
                        DeviceAdminRationaleItem(
                            icon = Icons.Filled.CancelPresentation,
                            title = "Device Admin Revocation Lockout",
                            description = "If Device Administrator permissions are deactivated, the app auto-triggers Lost Mode and locks the screen."
                        )
                    }
                }
            }
        }

        // Overlay Permission Banner if missing
        if (!hasOverlayPermission) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AlertOrange.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AlertOrange, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = "Overlay Permission Warning",
                            tint = AlertOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OVERLAY PERMISSION REQUIRED",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlertOrange
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Grant 'Display over other apps' permission so Lost Mode can draw over other applications.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                        Button(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertOrange, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("request_overlay_permission_btn")
                        ) {
                            Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 2: LOCK COMMAND UI FOR ONLINE & OFFLINE DEVICES ENROLLED TO ADMIN
        // =========================================================================
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Enrolled Device Admin Console",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Manage remote screen lock commands for online and offline enrolled devices",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    Button(
                        onClick = { showEmergencyLockAllDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("emergency_lock_all_btn")
                    ) {
                        Icon(Icons.Filled.GppBad, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LOCK ALL", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Segmented Filter Tabs (All, Online, Offline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = deviceFilterTab == 0,
                        onClick = { deviceFilterTab = 0 },
                        label = { Text("All Devices (${devices.size})") },
                        leadingIcon = {
                            Icon(Icons.Filled.Devices, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("filter_all_devices_chip")
                    )

                    FilterChip(
                        selected = deviceFilterTab == 1,
                        onClick = { deviceFilterTab = 1 },
                        label = { Text("Online ($onlineCount)") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(EmeraldNeon, CircleShape)
                            )
                        },
                        modifier = Modifier.testTag("filter_online_devices_chip")
                    )

                    FilterChip(
                        selected = deviceFilterTab == 2,
                        onClick = { deviceFilterTab = 2 },
                        label = { Text("Offline ($offlineCount)") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(AlertOrange, CircleShape)
                            )
                        },
                        modifier = Modifier.testTag("filter_offline_devices_chip")
                    )
                }
            }
        }

        // Render Cards for each matching enrolled device
        items(filteredDevices, key = { it.id }) { device ->
            EnrolledDeviceLockCard(
                localDeviceId = viewModel.localDeviceId,
                device = device,
                onLockClick = {
                    if (device.isOnline) {
                        viewModel.triggerRemoteCommand(device.id, "LOCK_DEVICE")
                        Toast.makeText(context, "Remote lock command sent to ${device.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        // Offline device: queue command & show notice
                        viewModel.triggerRemoteCommand(device.id, "LOCK_DEVICE")
                        showOfflineQueueNoticeDialog = device
                    }
                },
                onUnlockClick = {
                    selectedDeviceForLock = device
                    ownerPinInput = ""
                    pinError = false
                    showLockDialog = true
                },
                onLostModeClick = {
                    selectedDeviceForLock = device
                    if (device.isLostMode) {
                        viewModel.triggerRemoteCommand(device.id, "STOP_LOST_MODE")
                    } else {
                        showLostModeDialog = true
                    }
                },
                onAlarmClick = {
                    if (device.isAlarmActive) {
                        viewModel.triggerRemoteCommand(device.id, "STOP_ALARM")
                    } else {
                        viewModel.triggerRemoteCommand(device.id, "PLAY_ALARM")
                    }
                },
                onWipeClick = {
                    selectedDeviceForLock = device
                    wipeConfirmCheck1 = false
                    wipeConfirmCheck2 = false
                    showWipeDialog = true
                }
            )
        }
    }

    // =========================================================================
    // MODALS & DIALOGS
    // =========================================================================

    // Modal 1: OFFLINE COMMAND QUEUED DIALOG NOTICE
    showOfflineQueueNoticeDialog?.let { offlineDev ->
        AlertDialog(
            onDismissRequest = { showOfflineQueueNoticeDialog = null },
            icon = { Icon(Icons.Filled.CloudOff, contentDescription = null, tint = AlertOrange) },
            title = { Text("Offline Device Lock Command Queued", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Device '${offlineDev.name}' is currently OFFLINE.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "The remote lock payload has been cryptographically signed with RSA-2048 and stored in the background dispatch queue.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    Surface(
                        color = AlertOrange.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "● Automatic Delivery Methods Active:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlertOrange
                            )
                            Text(
                                text = "1. Immediate auto-lock upon network reconnect\n2. Encrypted SMS Gateway Payload fallback trigger\n3. Local Device Policy Manager background agent daemon",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showOfflineQueueNoticeDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertOrange, contentColor = Color.Black)
                ) {
                    Text("Acknowledge Queue")
                }
            }
        )
    }

    // Modal 2: EMERGENCY SYSTEM-WIDE LOCKDOWN CONFIRMATION DIALOG
    if (showEmergencyLockAllDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyLockAllDialog = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = DangerRed) },
            title = { Text("Execute Emergency Lockdown", fontWeight = FontWeight.Bold, color = DangerRed) },
            text = {
                Text(
                    text = "This action will dispatch lock commands to ALL ${devices.size} enrolled devices (both online and offline). Offline devices will queue the lock payload for immediate execution upon reconnect.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyLockAllDialog = false
                        viewModel.lockAllDevices()
                        Toast.makeText(context, "System-wide lock commands dispatched to all enrolled devices", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("LOCK ALL DEVICES NOW")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyLockAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal 3: UNLOCK DEVICE OWNER PIN DIALOG
    if (showLockDialog) {
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            title = {
                Text(
                    text = "Unlock Device Authorization",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "To authorize unlocking '${selectedDeviceForLock?.name}', please enter your Owner PIN Code:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = ownerPinInput,
                        onValueChange = {
                            ownerPinInput = it
                            pinError = false
                        },
                        label = { Text("Enter 4-Digit Owner PIN") },
                        singleLine = true,
                        isError = pinError,
                        supportingText = {
                            if (pinError) {
                                Text("Invalid Secure Token Code. Hint: Use 1234 or 2026")
                            } else {
                                Text("Hint: Use 1234 or 2026")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_pin_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            BiometricPromptHelper.authenticate(
                                context = context,
                                title = "Authorize Device Unlock",
                                subtitle = "Biometric Security Scan",
                                description = "Scan fingerprint or face to authorize unlocking '${selectedDeviceForLock?.name}'",
                                onSuccess = {
                                    showLockDialog = false
                                    selectedDeviceForLock?.let { dev ->
                                        viewModel.triggerRemoteCommand(dev.id, "UNLOCK_DEVICE")
                                        Toast.makeText(context, "Biometric verified! Unlock command dispatched to ${dev.name}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Biometric error: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Biometric", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            if (viewModel.authenticateOwner(ownerPinInput)) {
                                showLockDialog = false
                                selectedDeviceForLock?.let { dev ->
                                    viewModel.triggerRemoteCommand(dev.id, "UNLOCK_DEVICE")
                                    Toast.makeText(context, "Unlock command dispatched to ${dev.name}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                pinError = true
                            }
                        },
                        modifier = Modifier.testTag("confirm_lock_btn")
                    ) {
                        Text("Authorize Unlock")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal 4: LOST MODE DIALOG
    if (showLostModeDialog) {
        AlertDialog(
            onDismissRequest = { showLostModeDialog = false },
            title = { Text("Configure Lost Mode Recovery Payload", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Customize the message and contact details displayed on '${selectedDeviceForLock?.name}':",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = lostMessage,
                        onValueChange = { lostMessage = it },
                        label = { Text("Screen Display Message") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lost_msg_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = lostContact,
                        onValueChange = { lostContact = it },
                        label = { Text("Emergency Return Phone Number") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lost_contact_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLostModeDialog = false
                        selectedDeviceForLock?.let { dev ->
                            viewModel.triggerRemoteCommand(
                                dev.id,
                                "START_LOST_MODE",
                                mapOf("message" to lostMessage, "contact" to lostContact)
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertOrange),
                    modifier = Modifier.testTag("confirm_lost_btn")
                ) {
                    Text("Deploy Payload")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLostModeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal 5: CRYPTOGRAPHIC FACTORY WIPE DIALOG
    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = "Critical Warning", tint = DangerRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cryptographic Wipe Authorization", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "You are initiating a Remote Factory Wipe on '${selectedDeviceForLock?.name}'. This zero-fills memory layers and wipes files permanently.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = wipeConfirmCheck1,
                            onCheckedChange = { wipeConfirmCheck1 = it },
                            modifier = Modifier.testTag("wipe_chk_1")
                        )
                        Text("I understand that all files and cryptographic keys will be deleted.", fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = wipeConfirmCheck2,
                            onCheckedChange = { wipeConfirmCheck2 = it },
                            modifier = Modifier.testTag("wipe_chk_2")
                        )
                        Text("I authorize the system to trigger direct memory formatting.", fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = wipePhraseInput,
                        onValueChange = { wipePhraseInput = it },
                        label = { Text("Type 'WIPE' to confirm authorization") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wipe_phrase_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeDialog = false
                        selectedDeviceForLock?.let { dev ->
                            viewModel.triggerRemoteCommand(dev.id, "WIPE_DEVICE")
                        }
                    },
                    enabled = wipeConfirmCheck1 && wipeConfirmCheck2 && wipePhraseInput.trim().equals("WIPE", ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.testTag("confirm_wipe_btn")
                ) {
                    Text("AUTHORIZE ZERO-FILL WIPE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =========================================================================
// HELPER COMPOSABLES
// =========================================================================

@Composable
fun DeviceAdminRationaleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EmeraldNeon,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun EnrolledDeviceLockCard(
    localDeviceId: String,
    device: DeviceEntity,
    onLockClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onLostModeClick: () -> Unit,
    onAlarmClick: () -> Unit,
    onWipeClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                when {
                    device.isLostMode -> DangerRed
                    device.isLocked -> AlertOrange
                    device.isOnline -> EmeraldNeon.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                },
                RoundedCornerShape(16.dp)
            )
            .testTag("enrolled_device_card_${device.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Device Name, Model, and Badges (Online/Offline + Lock Status)
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
                            .background(
                                if (device.isOnline) EmeraldNeon.copy(alpha = 0.15f) else AlertOrange.copy(alpha = 0.15f),
                                CircleShape
                            )
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (device.id == localDeviceId) Icons.Filled.Smartphone else Icons.Filled.Phonelink,
                            contentDescription = "Device Icon",
                            tint = if (device.isOnline) EmeraldNeon else AlertOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${device.manufacturer} ${device.model} • ${device.androidVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Connectivity Badge
                Surface(
                    color = if (device.isOnline) EmeraldNeon.copy(alpha = 0.15f) else AlertOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (device.isOnline) EmeraldNeon else AlertOrange, CircleShape)
                        )
                        Text(
                            text = if (device.isOnline) "ONLINE" else "OFFLINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (device.isOnline) EmeraldNeon else AlertOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Middle Info Row: Network, Battery, Security Lock Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Network: ${device.networkStatus}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Battery: ${device.batteryPercentage}% ${if (device.isCharging) "(Charging)" else ""}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = when {
                        device.isLostMode -> DangerRed.copy(alpha = 0.2f)
                        device.isLocked -> AlertOrange.copy(alpha = 0.2f)
                        else -> EmeraldNeon.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when {
                            device.isLostMode -> "LOST MODE"
                            device.isLocked -> "LOCKED"
                            else -> "UNLOCKED"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            device.isLostMode -> DangerRed
                            device.isLocked -> AlertOrange
                            else -> EmeraldNeon
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Offline dispatch notice indicator
            if (!device.isOnline) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = AlertOrange, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Device is offline. Sent lock commands will queue automatically in payload store.",
                        fontSize = 10.sp,
                        color = AlertOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Lock Command UI Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (device.isLocked) {
                    Button(
                        onClick = onUnlockClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("unlock_device_btn_${device.id}")
                    ) {
                        Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unlock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onLockClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (device.isOnline) AlertOrange else AlertOrange.copy(alpha = 0.85f),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lock_device_btn_${device.id}")
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (device.isOnline) "Lock Device" else "Queue Lock",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Lost Mode Toggle Button
                OutlinedButton(
                    onClick = onLostModeClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (device.isLostMode) EmeraldNeon else AlertOrange
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("lost_mode_toggle_btn_${device.id}")
                ) {
                    Icon(
                        imageVector = if (device.isLostMode) Icons.Filled.CheckCircle else Icons.Filled.GppMaybe,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (device.isLostMode) "Exit Lost" else "Lost Mode", fontSize = 12.sp)
                }

                // Emergency Siren Icon Button
                IconButton(
                    onClick = onAlarmClick,
                    modifier = Modifier.testTag("alarm_btn_${device.id}")
                ) {
                    Icon(
                        imageVector = if (device.isAlarmActive) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Trigger Siren Alarm",
                        tint = if (device.isAlarmActive) DangerRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Wipe Icon Button
                IconButton(
                    onClick = onWipeClick,
                    modifier = Modifier.testTag("wipe_btn_${device.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteForever,
                        contentDescription = "Cryptographic Wipe",
                        tint = DangerRed.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
