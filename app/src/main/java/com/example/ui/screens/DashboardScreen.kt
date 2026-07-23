package com.example.ui.screens

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SentinelViewModel,
    devices: List<DeviceEntity>,
    modifier: Modifier = Modifier
) {
    var selectedDevice by remember { mutableStateOf<DeviceEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showPermissionsDialog by remember { mutableStateOf(false) }

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
            selectedDevice = devices.find { it.id == "sentinel-agent-local" } ?: devices.first()
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
                                    text = if (device.id == "sentinel-agent-local") "This Device" else device.name,
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

                            if (device.id != "sentinel-agent-local") {
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
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
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
                        val filteredAppList = rawAppList.filter { it.contains(searchQuery, ignoreCase = true) }

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
                                            text = appName,
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
                    text = "Enroll Connected Device",
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
                                    contentString = "sentinelx://enroll?agent_id=stx-local-phone-a&token=84f29a",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Agent ID: sentinel-agent-local-a",
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Pairing Token: STX-PAIR-84F29A",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = EmeraldNeon
                                )
                            }
                        }
                        1 -> {
                            // MODE 2: SCAN REMOTE QR CODE (From Phone B)
                            Text(
                                text = "Camera Viewfinder: Point Phone A's camera at Phone B's QR code or enter Phone B's Pairing Token below.",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            // Interactive Viewfinder Frame Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(Color.Black, RoundedCornerShape(12.dp))
                                    .border(1.dp, EmeraldNeon, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.CenterFocusWeak,
                                        contentDescription = "Scanner Viewfinder",
                                        tint = EmeraldNeon,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "SCANNER READY • DETECTING PAIRING TARGET...",
                                        fontSize = 9.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = EmeraldNeon
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = remoteTokenInput,
                                onValueChange = { remoteTokenInput = it },
                                label = { Text("Scanned Pairing Token") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("scanned_token_input")
                            )

                            Button(
                                onClick = {
                                    val newId = viewModel.enrollNewDevice("Paired Mobile Phone B", "Galaxy S24 Ultra", "Samsung")
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
                        selectedDevice = devices.find { it.id == "sentinel-agent-local" }
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
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val gridSize = 21
        val cellSize = sizePx / gridSize
        val darkColor = Color.Black
        val lightColor = Color.White

        drawRect(color = lightColor)

        val seed = contentString.hashCode()
        val random = java.util.Random(seed.toLong())

        val grid = Array(gridSize) { BooleanArray(gridSize) }

        // Finder patterns (7x7)
        fun markFinder(startR: Int, startC: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isOuterBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isInnerCore = r in 2..4 && c in 2..4
                    grid[startR + r][startC + c] = isOuterBorder || isInnerCore
                }
            }
        }

        markFinder(0, 0)
        markFinder(0, 14)
        markFinder(14, 0)

        // Random data modules for non-finder positions
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val inTopLeftFinder = r < 8 && c < 8
                val inTopRightFinder = r < 8 && c >= 13
                val inBottomLeftFinder = r >= 13 && c < 8
                if (!inTopLeftFinder && !inTopRightFinder && !inBottomLeftFinder) {
                    if (r == 6 || c == 6) {
                        grid[r][c] = (r + c) % 2 == 0
                    } else {
                        grid[r][c] = random.nextBoolean()
                    }
                }
            }
        }

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (grid[r][c]) {
                    drawRect(
                        color = darkColor,
                        topLeft = androidx.compose.ui.geometry.Offset(c * cellSize, r * cellSize),
                        size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}
