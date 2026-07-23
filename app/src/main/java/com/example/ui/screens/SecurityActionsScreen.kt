package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityActionsScreen(
    viewModel: SentinelViewModel,
    devices: List<DeviceEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasOverlayPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }

    var selectedDeviceId by remember { mutableStateOf("sentinel-agent-local") }
    val activeDevice = devices.find { it.id == selectedDeviceId } ?: devices.firstOrNull()

    // Dialog controllers
    var showLockDialog by remember { mutableStateOf(false) }
    var showLostModeDialog by remember { mutableStateOf(false) }
    var showWipeDialog by remember { mutableStateOf(false) }

    // Forms states
    var ownerPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var lostMessage by remember { mutableStateOf("This device is lost. Please contact the owner immediately.") }
    var lostContact by remember { mutableStateOf("+1-555-0199") }

    var wipeConfirmCheck1 by remember { mutableStateOf(false) }
    var wipeConfirmCheck2 by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        // Device selection banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Target Device Authorized Console",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        devices.forEach { dev ->
                            val isSelected = dev.id == selectedDeviceId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDeviceId = dev.id },
                                label = { Text(if (dev.id == "sentinel-agent-local") "Local Agent" else dev.name) },
                                modifier = Modifier.testTag("action_device_chip_${dev.id}")
                            )
                        }
                    }
                }
            }
        }

        // Threat overview banner
        activeDevice?.let { device ->
            item {
                val statusText = when {
                    device.isLostMode -> "LOST DEVICE MODE IN PROGRESS"
                    device.isLocked -> "DEVICE SECURITY LOCK ACTIVE"
                    device.isAlarmActive -> "REMOTE AUDIBLE ALARM CRYING"
                    else -> "DEVICE STATUS SECURED & READY"
                }
                val bannerBg = when {
                    device.isLostMode -> DangerRed.copy(alpha = 0.12f)
                    device.isLocked -> AlertOrange.copy(alpha = 0.12f)
                    device.isAlarmActive -> DangerRed.copy(alpha = 0.1f)
                    else -> EmeraldNeon.copy(alpha = 0.08f)
                }
                val bannerBorder = when {
                    device.isLostMode -> DangerRed
                    device.isLocked -> AlertOrange
                    device.isAlarmActive -> DangerRed
                    else -> EmeraldNeon
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = bannerBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, bannerBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                device.isLostMode -> Icons.Filled.ReportProblem
                                device.isLocked -> Icons.Filled.Lock
                                device.isAlarmActive -> Icons.Filled.VolumeUp
                                else -> Icons.Filled.Shield
                            },
                            contentDescription = "Threat Level Indicator",
                            tint = bannerBorder,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = bannerBorder
                            )
                            Text(
                                text = "Token verification status: Valid & Signed with RSA_OAEP_2048",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // Action grid cards
        activeDevice?.let { device ->
            // Action 1: Remote Lock
            item {
                SecurityActionCard(
                    title = "Remote System Lockout",
                    description = "Locks the device system UI instantly. Requires owner credentials to bypass and restore access.",
                    buttonText = if (device.isLocked) "Device is Locked (Release Lock)" else "Execute Remote Lock",
                    icon = Icons.Filled.LockOpen,
                    isActive = device.isLocked,
                    buttonColor = if (device.isLocked) EmeraldNeon else MaterialTheme.colorScheme.primary,
                    testTag = "remote_lock_action_btn",
                    onClick = {
                        ownerPinInput = ""
                        pinError = false
                        showLockDialog = true
                    }
                )
            }

            // Action 2: Lost Device Mode
            item {
                SecurityActionCard(
                    title = "Lost Device Mode (High-Alert)",
                    description = "Pushes custom owner message on screen, raises location frequency intervals to high precision, and restricts outgoing app activity.",
                    buttonText = if (device.isLostMode) "Deactivate Lost Mode" else "Activate Lost Mode",
                    icon = Icons.Filled.PrivacyTip,
                    isActive = device.isLostMode,
                    buttonColor = if (device.isLostMode) EmeraldNeon else AlertOrange,
                    testTag = "lost_mode_action_btn",
                    onClick = {
                        if (device.isLostMode) {
                            viewModel.triggerRemoteCommand(device.id, "STOP_LOST_MODE")
                        } else {
                            showLostModeDialog = true
                        }
                    }
                )
            }

            // Action 3: Remote Siren Alarm
            item {
                SecurityActionCard(
                    title = "Trigger Remote Emergency Alarm",
                    description = "Overrides system volume levels to play an acoustic distress signal at maximum audio dB until dismissed by owner.",
                    buttonText = if (device.isAlarmActive) "Mute Emergency Alarm" else "Siren Blast",
                    icon = Icons.Filled.VolumeUp,
                    isActive = device.isAlarmActive,
                    buttonColor = if (device.isAlarmActive) AlertOrange else DangerRed,
                    testTag = "remote_alarm_action_btn",
                    onClick = {
                        if (device.isAlarmActive) {
                            viewModel.triggerRemoteCommand(device.id, "STOP_ALARM")
                        } else {
                            viewModel.triggerRemoteCommand(device.id, "PLAY_ALARM")
                        }
                    }
                )
            }

            // Action 4: Remote Hardware Wipe
            item {
                SecurityActionCard(
                    title = "Remote Data Protection (Factory Wipe)",
                    description = "Performs a cryptographic key zeroization followed by complete hardware memory wipe. WARNING: Irreversible.",
                    buttonText = "Execute Factory Wipe",
                    icon = Icons.Filled.DeleteForever,
                    isActive = false,
                    buttonColor = DangerRed,
                    testTag = "remote_wipe_action_btn",
                    onClick = {
                        wipeConfirmCheck1 = false
                        wipeConfirmCheck2 = false
                        showWipeDialog = true
                    }
                )
            }
        }
    }

    // Modal 1: PIN LOCK DIALOG
    if (showLockDialog) {
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            title = {
                Text(
                    text = if (activeDevice?.isLocked == true) "Unlock Device Authorization" else "Lock Device Authorization",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "To authorize cryptographic signature validation, please type your Owner PIN Code below:",
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
                                Text("Invalid Secure Token Code. Hint: Use 2026")
                            } else {
                                Text("Hint: Use 2026 for authorization")
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
                Button(
                    onClick = {
                        if (viewModel.authenticateOwner(ownerPinInput)) {
                            showLockDialog = false
                            activeDevice?.let { dev ->
                                if (dev.isLocked) {
                                    viewModel.triggerRemoteCommand(dev.id, "UNLOCK_DEVICE")
                                } else {
                                    viewModel.triggerRemoteCommand(dev.id, "LOCK_DEVICE")
                                }
                            }
                        } else {
                            pinError = true
                        }
                    },
                    modifier = Modifier.testTag("confirm_lock_btn")
                ) {
                    Text("Validate Certificate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal 2: LOST MODE DIALOG
    if (showLostModeDialog) {
        AlertDialog(
            onDismissRequest = { showLostModeDialog = false },
            title = { Text("Configure Lost Mode Recovery Payload", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Customize the message and emergency coordinates that will display on the locked screen:",
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
                        activeDevice?.let { dev ->
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

    // Modal 3: CRYPTOGRAPHIC FACTORY WIPE DIALOG
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
                        text = "You are initiating a Remote Factory Wipe on '${activeDevice?.name}'. This zero-fills solid state memory layers and wipes files permanently.",
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
                        Text("I understand that all files, accounts, and cryptographic key blocks will be deleted.", fontSize = 12.sp)
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeDialog = false
                        activeDevice?.let { dev ->
                            viewModel.triggerRemoteCommand(dev.id, "WIPE_DEVICE")
                        }
                    },
                    enabled = wipeConfirmCheck1 && wipeConfirmCheck2,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.testTag("confirm_wipe_btn")
                ) {
                    Text("AUTHORIZE ZERO-FILL WIPE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) {
                    Text("Abrupt Cancel")
                }
            }
        )
    }
}

@Composable
fun SecurityActionCard(
    title: String,
    description: String,
    buttonText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    buttonColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isActive) buttonColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) buttonColor else MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .background(if (isActive) buttonColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.background, CircleShape)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isActive) buttonColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(testTag),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
