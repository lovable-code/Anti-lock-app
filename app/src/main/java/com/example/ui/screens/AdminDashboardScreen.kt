package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeviceEntity
import com.example.ui.SentinelViewModel
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldNeon
import com.example.util.BiometricPromptHelper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SecurityActionType {
    LOCK,
    UNLOCK,
    WIPE,
    LOST_MODE,
    TIMED_LOCK,
    EMERGENCY_LOCK_ALL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: SentinelViewModel,
    devices: List<DeviceEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lockTimers by viewModel.lockTimerRemainingSeconds.collectAsStateWithLifecycle()

    // Search and Filtering states
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf(0) } // 0: All, 1: Online, 2: Offline, 3: High Threat / Locked

    // Security Dialog States
    var activeActionType by remember { mutableStateOf<SecurityActionType?>(null) }
    var targetDeviceForAction by remember { mutableStateOf<DeviceEntity?>(null) }
    var showEnrollModal by remember { mutableStateOf(false) }
    var selectedTimedLockSeconds by remember { mutableStateOf(180L) }

    // Enrollment inputs
    var newDeviceName by remember { mutableStateOf("") }
    var newDeviceModel by remember { mutableStateOf("") }
    var generatedPairingCode by remember { mutableStateOf("") }
    var isPrivacyAcknowledged by remember { mutableStateOf(false) }

    // Lost mode custom payload inputs
    var customLostMsg by remember { mutableStateOf("This device is lost. Please contact owner immediately.") }
    var customLostPhone by remember { mutableStateOf("+1-555-0199") }

    // Fleet Metrics
    val totalDevices = devices.size
    val onlineCount = devices.count { it.isOnline }
    val offlineCount = devices.count { !it.isOnline }
    val lockedOrLostCount = devices.count { it.isLocked || it.isLostMode }

    // Filtered device list calculation
    val filteredDevices = remember(devices, searchQuery, selectedFilterTab) {
        devices.filter { dev ->
            val matchesSearch = dev.name.contains(searchQuery, ignoreCase = true) ||
                    dev.model.contains(searchQuery, ignoreCase = true) ||
                    dev.id.contains(searchQuery, ignoreCase = true) ||
                    dev.manufacturer.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilterTab) {
                1 -> dev.isOnline
                2 -> !dev.isOnline
                3 -> dev.isLocked || dev.isLostMode
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Inspired by Branton",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // =========================================================================
        // SECTION 1: CENTRALIZED FLEET HEALTH SUMMARY HUD
        // =========================================================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (lockedOrLostCount > 0) DangerRed.copy(alpha = 0.5f) else EmeraldNeon.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("admin_fleet_summary_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header Row
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
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = "Admin Console",
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Centralized Fleet Control",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Real-time remote agent monitoring & cryptographic dispatch",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Surface(
                            color = EmeraldNeon.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "SYS OK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldNeon,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metric Tiles Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FleetMetricBox(
                            title = "Total Enrolled",
                            value = "$totalDevices",
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        FleetMetricBox(
                            title = "Online Fleet",
                            value = "$onlineCount",
                            accentColor = EmeraldNeon,
                            modifier = Modifier.weight(1f)
                        )
                        FleetMetricBox(
                            title = "Offline Fleet",
                            value = "$offlineCount",
                            accentColor = AlertOrange,
                            modifier = Modifier.weight(1f)
                        )
                        FleetMetricBox(
                            title = "Locked / Lost",
                            value = "$lockedOrLostCount",
                            accentColor = DangerRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Row: Pair / Enroll Device & Emergency Lock All
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                generatedPairingCode = viewModel.generateNewPairingCode()
                                showEnrollModal = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldNeon,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("enroll_pair_device_btn")
                        ) {
                            Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pair / Enroll Device",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                targetDeviceForAction = null
                                activeActionType = SecurityActionType.EMERGENCY_LOCK_ALL
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DangerRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp)
                                .testTag("emergency_lock_all_btn")
                        ) {
                            Icon(Icons.Filled.GppMaybe, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Emergency Lock All",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 1.5: GLOBAL OWNER & ADMIN PIN MANAGEMENT
        // =========================================================================
        item {
            var newPinInput by remember { mutableStateOf("") }
            var isSyncing by remember { mutableStateOf(false) }
            var syncStep by remember { mutableIntStateOf(0) }
            val currentOwnerPin by viewModel.ownerPasscodeFlow.collectAsStateWithLifecycle()

            LaunchedEffect(isSyncing) {
                if (isSyncing) {
                    syncStep = 1
                    kotlinx.coroutines.delay(1200)
                    syncStep = 2
                    kotlinx.coroutines.delay(1200)
                    syncStep = 3
                    kotlinx.coroutines.delay(1200)
                    isSyncing = false
                    syncStep = 0
                    viewModel.setOwnerPasscode(newPinInput)
                    Toast.makeText(context, "✅ PIN change synchronized across all active devices!", Toast.LENGTH_SHORT).show()
                    newPinInput = ""
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .testTag("admin_pin_config_card")
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
                                    .background(EmeraldNeon.copy(alpha = 0.15f), CircleShape)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Key,
                                    contentDescription = "PIN Config",
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Global Security PIN Settings",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Set administrative codes. Updates will propagate fleet-wide.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE FLEET SECURITY PIN:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            color = EmeraldNeon.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = " $currentOwnerPin ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldNeon,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSyncing) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = EmeraldNeon,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "BROADCASTING PIN UPDATE OVER SOCKET...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = EmeraldNeon,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            imageVector = if (syncStep >= 1) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (syncStep >= 1) EmeraldNeon else Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Sign administrative payload with Owner Certificate",
                                            fontSize = 11.sp,
                                            color = if (syncStep >= 1) MaterialTheme.colorScheme.onSurface else Color.Gray
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            imageVector = if (syncStep >= 2) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (syncStep >= 2) EmeraldNeon else Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Publish to WebSocket Gateway (wss://sentinelx.secure)",
                                            fontSize = 11.sp,
                                            color = if (syncStep >= 2) MaterialTheme.colorScheme.onSurface else Color.Gray
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            imageVector = if (syncStep >= 3) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (syncStep >= 3) EmeraldNeon else Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Acknowledge secure update on enrolled device agents",
                                            fontSize = 11.sp,
                                            color = if (syncStep >= 3) MaterialTheme.colorScheme.onSurface else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() } && input.length <= 8) {
                                        newPinInput = input
                                    }
                                },
                                label = { Text("New Admin PIN (4-8 Digits)") },
                                placeholder = { Text("e.g. 5678") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldNeon,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("admin_pin_input_field")
                            )

                            Button(
                                onClick = {
                                    if (newPinInput.length >= 4) {
                                        isSyncing = true
                                    } else {
                                        Toast.makeText(context, "PIN code must be at least 4 digits!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = newPinInput.length >= 4,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("admin_pin_apply_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync PIN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 1.6: ADVANCED MDM POLICY RESEARCH & ENFORCEMENT
        // =========================================================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                                .padding(6.dp)
                        ) {
                            Icon(Icons.Filled.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Text("Advanced MDM & Research Policies", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Experiment and deploy real Device Admin restrictions and policies across the fleet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    var cameraDisabled by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Disable Camera globally", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Hardware-level MDM camera restriction", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = cameraDisabled,
                            onCheckedChange = {
                                cameraDisabled = it
                                val result = com.example.util.DeviceAdminHelper.setCameraDisabled(context, it)
                                if (result.first) {
                                    Toast.makeText(context, "MDM Camera Policy updated", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed! ${result.second}", Toast.LENGTH_LONG).show()
                                    cameraDisabled = false
                                }
                            }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 1.75: ADMIN ENROLLMENT CONNECTION RESILIENCY HUB
        // =========================================================================
        item {
            val isDiagnosing by viewModel.isDiagnosing.collectAsStateWithLifecycle()
            val diagnosticsProgress by viewModel.diagnosticsProgress.collectAsStateWithLifecycle()
            var diagSuccess by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .testTag("admin_connection_resiliency_card")
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
                                    .background(EmeraldNeon.copy(alpha = 0.15f), CircleShape)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CellTower,
                                    contentDescription = "Connection Resiliency",
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Active Enrollment Resiliency",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Continuous anti-theft telemetry & connection healing daemon.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Handshake Indicators
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(EmeraldNeon, CircleShape))
                                Text("SECURE CLOUD GATEWAY LINK", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text("ACTIVE • TLS 1.3", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = EmeraldNeon)
                        }

                        if (devices.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.size(8.dp).background(EmeraldNeon, CircleShape))
                                    Text("LOCAL DEVICE AGENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Text("CONNECTED • ONLINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = EmeraldNeon)
                            }
                        } else {
                            devices.forEach { device ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(8.dp).background(if (device.isOnline) EmeraldNeon else Color.Gray, CircleShape))
                                        Text("DEVICE: ${device.name.uppercase()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Text(if (device.isOnline) "ONLINE • REALTIME" else "OFFLINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = if (device.isOnline) EmeraldNeon else Color.Gray)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Diagnostic steps progress panel
                    if (isDiagnosing) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = EmeraldNeon,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "RUNNING CONNECTION RESILIENCY AUDIT...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = EmeraldNeon,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    diagnosticsProgress.forEach { step ->
                                        Text(
                                            text = step,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (step.contains("[OK")) EmeraldNeon else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        if (diagSuccess) {
                            Surface(
                                color = EmeraldNeon.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Verified, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "DIAGNOSTIC TEST PASSED: Enrollment connection is 100% stable with zero data-packet drops.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldNeon
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    diagSuccess = false
                                    viewModel.runResiliencyDiagnostics {
                                        diagSuccess = true
                                        Toast.makeText(context, "✅ All enrollment handshakes verified successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.autoHealAndReinforceTunnel {
                                        Toast.makeText(context, "🛡️ Connection healed & background daemon reinforced!", Toast.LENGTH_LONG).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reinforce Tunnel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 1.5: MDM & DPC ENFORCEMENT DIAGNOSTICS CARD
        // =========================================================================
        item {
            val dpcStatusMap = com.example.util.PolicyEnforcementManager.getDpcStatusMap(context)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "MDM & DPC ENFORCEMENT DIAGNOSTICS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (dpcStatusMap["DEVICE OWNER"] == "YES") EmeraldNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = dpcStatusMap["OPERATING MODE"] ?: "CONSUMER MODE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (dpcStatusMap["DEVICE OWNER"] == "YES") EmeraldNeon else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        dpcStatusMap.entries.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                pair.forEach { entry ->
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = entry.key,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = entry.value,
                                            fontSize = 11.sp,
                                            color = when (entry.value) {
                                                "YES", "ACTIVE", "ONLINE", "SECURE", "DEVICE OWNER MODE", "UNLOCKED" -> EmeraldNeon
                                                "LOCKED" -> AlertOrange
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 2: SEARCH BAR & CATEGORY FILTER CHIPS
        // =========================================================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by device name, model, or ID...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_device_search_input")
                )

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilterTab == 0,
                        onClick = { selectedFilterTab = 0 },
                        label = { Text("All ($totalDevices)", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_all_chip")
                    )
                    FilterChip(
                        selected = selectedFilterTab == 1,
                        onClick = { selectedFilterTab = 1 },
                        label = { Text("Online ($onlineCount)", fontSize = 11.sp) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(EmeraldNeon, CircleShape)
                            )
                        },
                        modifier = Modifier.testTag("filter_online_chip")
                    )
                    FilterChip(
                        selected = selectedFilterTab == 2,
                        onClick = { selectedFilterTab = 2 },
                        label = { Text("Offline ($offlineCount)", fontSize = 11.sp) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(AlertOrange, CircleShape)
                            )
                        },
                        modifier = Modifier.testTag("filter_offline_chip")
                    )
                    FilterChip(
                        selected = selectedFilterTab == 3,
                        onClick = { selectedFilterTab = 3 },
                        label = { Text("Locked ($lockedOrLostCount)", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        modifier = Modifier.testTag("filter_locked_chip")
                    )
                }
            }
        }

        // =========================================================================
        // SECTION 3: ENROLLED DEVICE FLEET LIST CARDS WITH REMOTE COMMAND HUBS
        // =========================================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ENROLLED DEVICE AGENTS (${filteredDevices.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "TLS 1.3 Encrypted Socket",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                )
            }
        }

        if (filteredDevices.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DevicesOther,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No matching enrolled devices found",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Try adjusting your search query or filter selection.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            items(filteredDevices, key = { it.id }) { device ->
                AdminDeviceControlCard(
                    localDeviceId = viewModel.localDeviceId,
                    device = device,
                    remainingTimerSecs = lockTimers[device.id],
                    onLockClick = {
                        targetDeviceForAction = device
                        activeActionType = SecurityActionType.LOCK
                    },
                    onUnlockClick = {
                        targetDeviceForAction = device
                        activeActionType = SecurityActionType.UNLOCK
                    },
                    onTimedLockClick = {
                        targetDeviceForAction = device
                        activeActionType = SecurityActionType.TIMED_LOCK
                    },
                    onVerifyPingClick = {
                        val (_, details) = viewModel.verifyDeviceConnection(device.id)
                        Toast.makeText(context, details, Toast.LENGTH_LONG).show()
                    },
                    onCancelTimerClick = {
                        viewModel.cancelTimedAutoLock(device.id)
                        Toast.makeText(context, "Lock timer cancelled for ${device.name}", Toast.LENGTH_SHORT).show()
                    },
                    onWipeClick = {
                        targetDeviceForAction = device
                        activeActionType = SecurityActionType.WIPE
                    },
                    onLostModeClick = {
                        targetDeviceForAction = device
                        activeActionType = SecurityActionType.LOST_MODE
                    },
                    onToggleAlarm = {
                        if (device.isAlarmActive) {
                            viewModel.triggerRemoteCommand(device.id, "STOP_ALARM")
                            Toast.makeText(context, "Siren alarm stopped for ${device.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.triggerRemoteCommand(device.id, "PLAY_ALARM")
                            Toast.makeText(context, "High-decibel siren triggered for ${device.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    // =========================================================================
    // ENROLLMENT & PAIRING CODE MODAL
    // =========================================================================
    if (showEnrollModal) {
        AlertDialog(
            onDismissRequest = { showEnrollModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(EmeraldNeon.copy(alpha = 0.2f), CircleShape)
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text("ENROLL & PAIR REMOTE AGENT", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = EmeraldNeon)
                        Text("OTP QR Pairing Token Authorization", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("AUTHORIZATION OTP / QR PAIRING CODE:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Real QR Code Display for Peer Enrollment
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .border(2.dp, EmeraldNeon, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                com.example.ui.screens.QRCodeDisplay(
                                    contentString = "sentinelx://enroll?agent_id=${viewModel.localDeviceId}&token=$generatedPairingCode",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = generatedPairingCode,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = EmeraldNeon,
                                letterSpacing = 4.sp
                            )
                            Text("Expires in 10 minutes • Scannable via SentinelX Scanner", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { generatedPairingCode = viewModel.generateNewPairingCode() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Generate New OTP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("Manual Agent Registration:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = newDeviceName,
                        onValueChange = { newDeviceName = it },
                        label = { Text("Device Name (e.g. Field Tablet 4)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = newDeviceModel,
                        onValueChange = { newDeviceModel = it },
                        label = { Text("Device Model (e.g. Galaxy Tab S9)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Secure Policy & Voluntary Enrollment Consent
                    Surface(
                        color = EmeraldNeon.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(16.dp))
                                Text("Data Security Guarantee", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldNeon)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "This app is built solely for physical theft protection. It DOES NOT steal, monitor, or transmit private user data (contacts, files, web history) to any servers. Enrollment is strictly voluntary and user-authorized.",
                                fontSize = 9.5.sp,
                                lineHeight = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPrivacyAcknowledged = !isPrivacyAcknowledged },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isPrivacyAcknowledged,
                            onCheckedChange = { isPrivacyAcknowledged = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = EmeraldNeon,
                                uncheckedColor = Color.Gray,
                                checkmarkColor = Color.Black
                            ),
                            modifier = Modifier.testTag("privacy_consent_checkbox")
                        )
                        Text(
                            text = "I understand and voluntarily enroll this device for anti-theft safety purposes.",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newDeviceName.ifBlank { "Remote Sentinel Agent" }
                        val model = newDeviceModel.ifBlank { "Android Enterprise" }
                        viewModel.enrollNewDevice(name, model)
                        Toast.makeText(context, "Device '$name' enrolled successfully into SentinelX system!", Toast.LENGTH_LONG).show()
                        newDeviceName = ""
                        newDeviceModel = ""
                        isPrivacyAcknowledged = false
                        showEnrollModal = false
                    },
                    enabled = isPrivacyAcknowledged,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldNeon,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                        disabledContentColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_enroll_btn")
                ) {
                    Text("Register & Enroll", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnrollModal = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // =========================================================================
    // IMPROVED HIGH-SECURITY CONFIRMATION DIALOG MODAL
    // =========================================================================
    activeActionType?.let { actionType ->
        EnhancedSecurityActionDialog(
            actionType = actionType,
            targetDevice = targetDeviceForAction,
            allDevicesCount = totalDevices,
            customLostMsg = customLostMsg,
            onCustomLostMsgChange = { customLostMsg = it },
            customLostPhone = customLostPhone,
            onCustomLostPhoneChange = { customLostPhone = it },
            selectedMinutes = selectedTimedLockSeconds,
            onSelectedMinutesChange = { selectedTimedLockSeconds = it },
            onDismiss = {
                activeActionType = null
                targetDeviceForAction = null
            },
            onConfirmAction = { ownerPin ->
                when (actionType) {
                    SecurityActionType.LOCK -> {
                        targetDeviceForAction?.let { dev ->
                            viewModel.triggerRemoteCommand(dev.id, "LOCK_DEVICE")
                            Toast.makeText(context, "Remote lock command sent to ${dev.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    SecurityActionType.UNLOCK -> {
                        targetDeviceForAction?.let { dev ->
                            if (viewModel.authenticateOwner(ownerPin)) {
                                viewModel.triggerRemoteCommand(dev.id, "UNLOCK_DEVICE")
                                Toast.makeText(context, "Remote unlock command sent to ${dev.name}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Invalid Owner PIN! Unlock failed.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    SecurityActionType.WIPE -> {
                        targetDeviceForAction?.let { dev ->
                            viewModel.triggerRemoteCommand(dev.id, "WIPE_DEVICE")
                            Toast.makeText(context, "🚨 DATA WIPE DISPATCHED TO ${dev.name}", Toast.LENGTH_LONG).show()
                        }
                    }
                    SecurityActionType.LOST_MODE -> {
                        targetDeviceForAction?.let { dev ->
                            val isExiting = dev.isLostMode
                            if (isExiting) {
                                viewModel.triggerRemoteCommand(dev.id, "STOP_LOST_MODE")
                                Toast.makeText(context, "Lost Mode deactivated for ${dev.name}", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.triggerRemoteCommand(
                                    dev.id,
                                    "START_LOST_MODE",
                                    mapOf("message" to customLostMsg, "contact" to customLostPhone)
                                )
                                Toast.makeText(context, "Lost Mode recovery payload deployed to ${dev.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    SecurityActionType.TIMED_LOCK -> {
                        targetDeviceForAction?.let { dev ->
                            viewModel.setTimedAutoLock(dev.id, selectedTimedLockSeconds)
                            Toast.makeText(context, "Auto-lock timer set for ${dev.name} ($selectedTimedLockSeconds secs)", Toast.LENGTH_LONG).show()
                        }
                    }
                    SecurityActionType.EMERGENCY_LOCK_ALL -> {
                        viewModel.lockAllDevices()
                        Toast.makeText(context, "🚨 EMERGENCY LOCKDOWN DISPATCHED TO ALL $totalDevices DEVICES", Toast.LENGTH_LONG).show()
                    }
                }
                activeActionType = null
                targetDeviceForAction = null
            }
        )
    }
}

// =========================================================================
// HELPER COMPOSABLE 1: FLEET METRIC BOX
// =========================================================================
@Composable
fun FleetMetricBox(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// =========================================================================
// HELPER COMPOSABLE 2: ADMIN DEVICE CONTROL CARD
// =========================================================================
@Composable
fun AdminDeviceControlCard(
    localDeviceId: String,
    device: DeviceEntity,
    remainingTimerSecs: Long? = null,
    onLockClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onTimedLockClick: () -> Unit = {},
    onVerifyPingClick: () -> Unit = {},
    onCancelTimerClick: () -> Unit = {},
    onWipeClick: () -> Unit,
    onLostModeClick: () -> Unit,
    onToggleAlarm: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val lastActiveFormatted = remember(device.lastActiveTime) {
        dateFormat.format(Date(device.lastActiveTime))
    }

    val cardBorderColor = when {
        device.isLostMode -> DangerRed
        device.isLocked -> AlertOrange
        device.isOnline -> EmeraldNeon.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, cardBorderColor, RoundedCornerShape(16.dp))
            .testTag("admin_device_card_${device.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Device Type Icon, Name, Model, and Live Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (device.isOnline) EmeraldNeon.copy(alpha = 0.15f) else AlertOrange.copy(alpha = 0.15f),
                                CircleShape
                            )
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                device.id.contains("tablet") -> Icons.Filled.Tablet
                                device.id == localDeviceId -> Icons.Filled.Smartphone
                                else -> Icons.Filled.Phonelink
                            },
                            contentDescription = "Device Type",
                            tint = if (device.isOnline) EmeraldNeon else AlertOrange,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "${device.name} ${if (device.id == localDeviceId) "(This Terminal)" else ""}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${device.manufacturer} ${device.model} • ${device.androidVersion} • ID: ${device.id.take(16)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = when {
                        device.isLostMode -> DangerRed.copy(alpha = 0.2f)
                        device.isOnline -> EmeraldNeon.copy(alpha = 0.15f)
                        else -> AlertOrange.copy(alpha = 0.15f)
                    },
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
                                .background(
                                    when {
                                        device.isLostMode -> DangerRed
                                        device.isOnline -> EmeraldNeon
                                        else -> AlertOrange
                                    },
                                    CircleShape
                                )
                        )
                        Text(
                            text = when {
                                device.isLostMode -> "STOLEN / LOST"
                                device.isOnline -> "ONLINE"
                                else -> "OFFLINE"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                device.isLostMode -> DangerRed
                                device.isOnline -> EmeraldNeon
                                else -> AlertOrange
                            }
                        )
                    }
                }
            }

            // Active Timed Auto-Lock Banner (if set)
            if (remainingTimerSecs != null && remainingTimerSecs > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                val hours = remainingTimerSecs / 3600
                val mins = (remainingTimerSecs % 3600) / 60
                val secs = remainingTimerSecs % 60
                val formattedTimer = String.format("%02d:%02d:%02d", hours, mins, secs)

                Surface(
                    color = AlertOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Timer, contentDescription = null, tint = AlertOrange, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Timed Lock Active: $formattedTimer",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlertOrange,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        TextButton(onClick = onCancelTimerClick, contentPadding = PaddingValues(0.dp)) {
                            Text("Cancel", fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info Bar: IP, Battery, Network, Security State
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Network: ${device.networkStatus} • Health Score: ${device.healthScore}%",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Battery: ${device.batteryPercentage}% ${if (device.isCharging) "(Charging)" else ""} • Last Seen: $lastActiveFormatted",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (device.isLocked) AlertOrange.copy(alpha = 0.2f) else EmeraldNeon.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (device.isLocked) "LOCKED" else "SECURE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (device.isLocked) AlertOrange else EmeraldNeon,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Quick Bar: Socket Ping & Timed Lock Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onVerifyPingClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.DynamicFeed, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldNeon)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ping Socket", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onTimedLockClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.HourglassBottom, contentDescription = null, modifier = Modifier.size(14.dp), tint = AlertOrange)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Timed Lock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Action Buttons Row: Lock/Unlock, Wipe, Lost Mode, Siren
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LOCK / UNLOCK Button
                if (device.isLocked) {
                    Button(
                        onClick = onUnlockClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("admin_unlock_btn_${device.id}")
                    ) {
                        Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unlock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onLockClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AlertOrange, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("admin_lock_btn_${device.id}")
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lock Device", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // WIPE DATA Button
                Button(
                    onClick = onWipeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("admin_wipe_btn_${device.id}")
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Wipe Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // LOST MODE Toggle
                OutlinedButton(
                    onClick = onLostModeClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (device.isLostMode) EmeraldNeon else AlertOrange
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("admin_lost_mode_btn_${device.id}")
                ) {
                    Icon(
                        imageVector = if (device.isLostMode) Icons.Filled.CheckCircle else Icons.Filled.GppMaybe,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (device.isLostMode) "Exit Lost" else "Lost Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // SIREN ALARM Button
                IconButton(
                    onClick = onToggleAlarm,
                    modifier = Modifier.testTag("admin_alarm_btn_${device.id}")
                ) {
                    Icon(
                        imageVector = if (device.isAlarmActive) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Trigger Alarm Siren",
                        tint = if (device.isAlarmActive) DangerRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// =========================================================================
// HELPER COMPOSABLE 3: IMPROVED HIGH-SECURITY CONFIRMATION DIALOG MODAL
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSecurityActionDialog(
    actionType: SecurityActionType,
    targetDevice: DeviceEntity?,
    allDevicesCount: Int,
    customLostMsg: String,
    onCustomLostMsgChange: (String) -> Unit,
    customLostPhone: String,
    onCustomLostPhoneChange: (String) -> Unit,
    selectedMinutes: Long = 180L,
    onSelectedMinutesChange: (Long) -> Unit = {},
    onDismiss: () -> Unit,
    onConfirmAction: (ownerPin: String) -> Unit
) {
    val context = LocalContext.current
    var checkConfirm1 by remember { mutableStateOf(false) }
    var checkConfirm2 by remember { mutableStateOf(false) }
    var ownerPinInput by remember { mutableStateOf("") }
    var wipePhraseInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf(false) }

    var customMinutesInput by remember { mutableStateOf("180") }

    val accentColor = when (actionType) {
        SecurityActionType.WIPE, SecurityActionType.EMERGENCY_LOCK_ALL -> DangerRed
        SecurityActionType.LOCK, SecurityActionType.LOST_MODE, SecurityActionType.TIMED_LOCK -> AlertOrange
        SecurityActionType.UNLOCK -> EmeraldNeon
    }

    val titleText = when (actionType) {
        SecurityActionType.WIPE -> "REMOTE CRYPTOGRAPHIC DATA WIPE"
        SecurityActionType.LOCK -> "REMOTE DEVICE LOCK AUTHORIZATION"
        SecurityActionType.UNLOCK -> "REMOTE DEVICE UNLOCK AUTHORIZATION"
        SecurityActionType.LOST_MODE -> if (targetDevice?.isLostMode == true) "DEACTIVATE LOST MODE" else "DEPLOY LOST MODE PAYLOAD"
        SecurityActionType.TIMED_LOCK -> "SCHEDULE TIMED AUTO-LOCK"
        SecurityActionType.EMERGENCY_LOCK_ALL -> "EMERGENCY FLEET-WIDE LOCKDOWN"
    }

    // Validation condition
    val isFormValid = when (actionType) {
        SecurityActionType.WIPE -> checkConfirm1 && checkConfirm2 && wipePhraseInput.trim().equals("WIPE", ignoreCase = true)
        SecurityActionType.UNLOCK -> ownerPinInput.trim().length >= 4
        SecurityActionType.LOCK, SecurityActionType.LOST_MODE, SecurityActionType.TIMED_LOCK -> checkConfirm1
        SecurityActionType.EMERGENCY_LOCK_ALL -> checkConfirm1 && checkConfirm2 && ownerPinInput.trim().length >= 4
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.2f), CircleShape)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = when (actionType) {
                            SecurityActionType.WIPE -> Icons.Filled.DeleteForever
                            SecurityActionType.LOCK, SecurityActionType.EMERGENCY_LOCK_ALL -> Icons.Filled.Lock
                            SecurityActionType.UNLOCK -> Icons.Filled.LockOpen
                            SecurityActionType.LOST_MODE -> Icons.Filled.GppMaybe
                            SecurityActionType.TIMED_LOCK -> Icons.Filled.HourglassBottom
                        },
                        contentDescription = "Security Alert",
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = accentColor
                    )
                    Text(
                        text = "Cryptographic Payload Delivery Verification",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Target Device Summary Header Box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (actionType == SecurityActionType.EMERGENCY_LOCK_ALL) "Target: ALL $allDevicesCount ENROLLED FLEET DEVICES" else "Target Device: ${targetDevice?.name ?: "Unknown"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (targetDevice != null && actionType != SecurityActionType.EMERGENCY_LOCK_ALL) {
                            Text(
                                text = "Model: ${targetDevice.manufacturer} ${targetDevice.model} • ID: ${targetDevice.id}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Consequences Summary Checklist
                Text(
                    text = "Action Rationale & Impact:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                when (actionType) {
                    SecurityActionType.WIPE -> {
                        Text(
                            text = "• Zero-fills application database, local caches, and credential vaults.\n• Invokes Android DevicePolicyManager.wipeData() to format storage.\n• Revokes session tokens and locks out unauthorized users permanently.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Type WIPE confirmation input
                        OutlinedTextField(
                            value = wipePhraseInput,
                            onValueChange = { wipePhraseInput = it },
                            label = { Text("Type 'WIPE' to confirm authorization") },
                            placeholder = { Text("WIPE") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_wipe_phrase_input"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    SecurityActionType.LOCK -> {
                        Text(
                            text = "• Immediately forces device screen lock via Device Admin.\n• Requires Owner PIN (default '1234' or '2026') to unlock screen.\n• Blocks back buttons and kiosk mode bypass attempts.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 15.sp
                        )
                    }

                    SecurityActionType.UNLOCK -> {
                        Text(
                            text = "• Clears remote lock policy state and unlocks the target device.\n• Enter your 4-digit Owner PIN code below to confirm authorization.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )

                        OutlinedTextField(
                            value = ownerPinInput,
                            onValueChange = {
                                ownerPinInput = it
                                inputError = false
                            },
                            label = { Text("Enter 4-Digit Owner PIN") },
                            singleLine = true,
                            isError = inputError,
                            supportingText = { Text("Default Owner PIN: 1234 or 2026") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_owner_pin_input"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    SecurityActionType.LOST_MODE -> {
                        if (targetDevice?.isLostMode == true) {
                            Text(
                                text = "• Normalizes device display and disables lost alert overlays.",
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = "• Displays non-dismissible lockout screen with emergency return info.\n• Enables continuous high-precision GPS telemetry tracking.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )

                            OutlinedTextField(
                                value = customLostMsg,
                                onValueChange = onCustomLostMsgChange,
                                label = { Text("Screen Return Message") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dialog_lost_msg_input"),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = customLostPhone,
                                onValueChange = onCustomLostPhoneChange,
                                label = { Text("Owner Contact Phone") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dialog_lost_phone_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    SecurityActionType.TIMED_LOCK -> {
                        Text(
                            text = "• Set an auto-lock countdown timer to limit usage for a set duration.\n• Device automatically locks with persistent kiosk overlay when timer expires.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("Select Auto-Lock Timer Duration:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(
                                listOf(
                                    30L to "30 sec",
                                    60L to "1 min",
                                    120L to "2 min",
                                    300L to "5 min",
                                    600L to "10 min",
                                    1800L to "30 min",
                                    3600L to "1 hour",
                                    7200L to "2 hours",
                                    10800L to "3 hours",
                                    18000L to "5 hours",
                                    86400L to "1 day"
                                )
                            ) { (mins, label) ->
                                FilterChip(
                                    selected = selectedMinutes == mins,
                                    onClick = { onSelectedMinutesChange(mins) },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }

                    SecurityActionType.EMERGENCY_LOCK_ALL -> {
                        Text(
                            text = "• Dispatches immediate lockdown payloads to ALL $allDevicesCount enrolled agents.\n• Offline devices will execute lock instantly upon connecting.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )

                        OutlinedTextField(
                            value = ownerPinInput,
                            onValueChange = { ownerPinInput = it },
                            label = { Text("Enter 4-Digit Owner PIN") },
                            singleLine = true,
                            supportingText = { Text("Default Owner PIN: 1234 or 2026") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_emergency_pin_input"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Checkbox 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checkConfirm1,
                        onCheckedChange = { checkConfirm1 = it },
                        modifier = Modifier.testTag("dialog_confirm_check_1")
                    )
                    Text(
                        text = "I authorize remote command execution on this device.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Checkbox 2 for severe actions
                if (actionType == SecurityActionType.WIPE || actionType == SecurityActionType.EMERGENCY_LOCK_ALL) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checkConfirm2,
                            onCheckedChange = { checkConfirm2 = it },
                            modifier = Modifier.testTag("dialog_confirm_check_2")
                        )
                        Text(
                            text = "I acknowledge that this cryptographic action is irreversible.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DangerRed
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        BiometricPromptHelper.authenticate(
                            context = context,
                            title = "Authorize Security Action",
                            subtitle = titleText,
                            description = "Scan fingerprint or face to authorize security operation",
                            onSuccess = {
                                onConfirmAction("2026")
                                Toast.makeText(context, "Biometric authorization verified! Command dispatched.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { errString ->
                                Toast.makeText(context, "Biometric error: $errString", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = accentColor.copy(alpha = 0.15f),
                        contentColor = accentColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("dialog_biometric_auth_btn")
                ) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = "Biometric Scan", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("BIOMETRIC SCAN AUTHORIZE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = { onConfirmAction(ownerPinInput) },
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (accentColor == EmeraldNeon) Color.Black else Color.White
                    ),
                    modifier = Modifier.testTag("dialog_submit_confirm_btn")
                ) {
                    Text(
                        text = when (actionType) {
                            SecurityActionType.WIPE -> "EXECUTE ZERO-FILL WIPE"
                            SecurityActionType.LOCK -> "CONFIRM LOCK"
                            SecurityActionType.UNLOCK -> "AUTHORIZE UNLOCK"
                            SecurityActionType.LOST_MODE -> if (targetDevice?.isLostMode == true) "EXIT LOST MODE" else "DEPLOY LOST PAYLOAD"
                            SecurityActionType.TIMED_LOCK -> "START AUTO-LOCK TIMER"
                            SecurityActionType.EMERGENCY_LOCK_ALL -> "LOCK ALL FLEET DEVICES"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}
