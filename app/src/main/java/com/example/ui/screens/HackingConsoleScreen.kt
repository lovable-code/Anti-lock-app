package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DeviceEntity
import com.example.ui.SentinelViewModel
import com.example.ui.components.EducationalPermissionsDialog
import com.example.ui.components.MatrixRainEffect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HackingConsoleScreen(
    viewModel: SentinelViewModel,
    devices: List<DeviceEntity>,
    modifier: Modifier = Modifier
) {
    val localDevice = devices.find { it.id == "sentinel-agent-local" } ?: devices.firstOrNull()

    var terminalLogs by remember {
        mutableStateOf(
            listOf(
                "[INIT] Cyber Security Matrix Kernel v4.19 loaded.",
                "[PROXY] Routing socket payload via TOR_RELAY_NODE_89...",
                "[AUTH] Root shell granted for 'sentinel-agent-local'.",
                "[INFO] Type 'help' or tap quick actions below to execute cyber commands."
            )
        )
    }

    var commandInput by remember { mutableStateOf("") }
    var showMatrixRainBackground by remember { mutableStateOf(true) }
    var showPermissionsDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun appendLog(msg: String) {
        terminalLogs = terminalLogs + msg
        scope.launch {
            if (terminalLogs.isNotEmpty()) {
                listState.animateScrollToItem(terminalLogs.size - 1)
            }
        }
    }

    fun runCyberCommand(cmdRaw: String) {
        val cmd = cmdRaw.trim().lowercase()
        appendLog("> $cmdRaw")
        when {
            cmd == "help" -> {
                appendLog("[SYSTEM HELP] Available commands:")
                appendLog("  - lock : Remote Online Lock execution")
                appendLog("  - unlock : Remote Online Unlock execution")
                appendLog("  - scan : Cyber Nmap vulnerability port sweep")
                appendLog("  - permissions : Launch camera & location rationale")
                appendLog("  - matrix : Toggle matrix digital rain mode")
                appendLog("  - brute : Simulate code unlock brute force test")
                appendLog("  - clear : Clear console log buffer")
            }
            cmd == "lock" || cmd.contains("lock") && !cmd.contains("unlock") -> {
                appendLog("[CYBER_EXEC] Triggering Remote Online Locking via WebSocket socket...")
                localDevice?.let { viewModel.triggerRemoteCommand(it.id, "LOCK_DEVICE") }
            }
            cmd == "unlock" || cmd.contains("unlock") -> {
                appendLog("[CYBER_EXEC] Triggering Remote Online Toggle Unlocking via WebSocket...")
                localDevice?.let { viewModel.triggerRemoteCommand(it.id, "UNLOCK_DEVICE") }
            }
            cmd.contains("scan") || cmd.contains("nmap") -> {
                appendLog("[NMAP] Sweeping IP 192.168.1.0/24...")
                appendLog("  Port 22/tcp OPEN (OpenSSH 8.9p1)")
                appendLog("  Port 443/tcp OPEN (TLS 1.3 AES-256-GCM)")
                appendLog("  Port 8080/tcp OPEN (SentinelX Web Console)")
                appendLog("[NMAP] Scan complete. Zero critical vulnerabilities found.")
            }
            cmd.contains("permission") || cmd == "perms" -> {
                appendLog("[PERM_RATIONALE] Opening educational camera and location access dialog...")
                showPermissionsDialog = true
            }
            cmd.contains("matrix") -> {
                showMatrixRainBackground = !showMatrixRainBackground
                appendLog("[MATRIX] Matrix Digital Rain Effect is now ${if (showMatrixRainBackground) "ENABLED" else "DISABLED"}.")
            }
            cmd.contains("brute") -> {
                appendLog("[BRUTE_FORCE] Testing passcode permutations against hash 'ae829c3f'...")
                appendLog("  Attempting key 0000.. ERROR")
                appendLog("  Attempting key 1111.. ERROR")
                appendLog("  Attempting key 2026.. SUCCESS [MATCH FOUND]")
                appendLog("[BRUTE_FORCE] Passcode code '2026' authenticated successfully.")
            }
            cmd == "clear" -> {
                terminalLogs = emptyList()
            }
            else -> {
                appendLog("[COMMAND UNKNOWN] '$cmdRaw'. Type 'help' for cyber terminal command listing.")
            }
        }
        commandInput = ""
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background Matrix Rain Effect
        if (showMatrixRainBackground) {
            MatrixRainEffect(modifier = Modifier.fillMaxSize())
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF030A06))
            )
        }

        // Overlay Dark Cyber Interface Container
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (showMatrixRainBackground) 0.65f else 0.95f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hacker Header Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1610).copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Hacker Man Avatar Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF00FF66), CircleShape)
                                .background(Color(0xFF030A06)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_hacker),
                                contentDescription = "Hacker Man Avatar Icon",
                                modifier = Modifier.size(38.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "HACKING & TERMINAL CONSOLE",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00FF66),
                                    letterSpacing = 1.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SYSTEM STATUS: ROOT AUTHORIZED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Tunnel: TOR_NODE_77 | Key: RSA_AES_256",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Toggle Matrix Background Rain button
                        IconButton(
                            onClick = { showMatrixRainBackground = !showMatrixRainBackground },
                            modifier = Modifier
                                .background(
                                    if (showMatrixRainBackground) Color(0xFF00FF66).copy(alpha = 0.2f) else Color(0xFF1E293B),
                                    CircleShape
                                )
                                .testTag("toggle_matrix_rain_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Code,
                                contentDescription = "Toggle Matrix Rain",
                                tint = if (showMatrixRainBackground) Color(0xFF00FF66) else Color.Gray
                            )
                        }
                    }
                }
            }

            // Online Locking & Online Toggle Unlocking & Code Unlocking Control Center Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E16).copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    imageVector = if (localDevice?.isLocked == true) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                    contentDescription = "Lock State",
                                    tint = if (localDevice?.isLocked == true) Color(0xFFEF4444) else Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ONLINE REMOTE LOCK CONTROLLER",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Online Toggle Unlock Switch
                            Switch(
                                checked = localDevice?.isLocked == true,
                                onCheckedChange = { isChecked ->
                                    localDevice?.let { dev ->
                                        viewModel.toggleDeviceLockOnline(dev.id)
                                    }
                                },
                                modifier = Modifier.testTag("online_lock_toggle_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFEF4444),
                                    checkedTrackColor = Color(0xFF7F1D1D),
                                    uncheckedThumbColor = Color(0xFF10B981),
                                    uncheckedTrackColor = Color(0xFF064E3B)
                                )
                            )
                        }

                        Divider(color = Color(0xFF00FF66).copy(alpha = 0.15f))

                        // Status pill breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Online Lock Status
                            Surface(
                                color = if (localDevice?.isLocked == true) Color(0xFF450A0A) else Color(0xFF022C22),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("ONLINE LOCK", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    Text(
                                        text = if (localDevice?.isLocked == true) "LOCKED" else "UNLOCKED",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (localDevice?.isLocked == true) Color(0xFFF87171) else Color(0xFF34D399),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Code Unlock Mode Status
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("CODE UNLOCK", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    Text(
                                        text = "PIN: 2026",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF38BDF8),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Online Lock Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    localDevice?.let { viewModel.triggerRemoteCommand(it.id, "LOCK_DEVICE") }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("online_lock_now_btn")
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Online Lock", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    localDevice?.let { viewModel.triggerRemoteCommand(it.id, "UNLOCK_DEVICE") }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF065F46)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("online_unlock_now_btn")
                            ) {
                                Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Online Unlock", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Educational Permissions Dialog Rationale Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
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
                                    .background(Color(0xFF0284C7).copy(alpha = 0.25f), CircleShape)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Camera,
                                    contentDescription = "Permissions",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Accompanist Permissions Rationale",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Educational rationale for Camera & Location access",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }

                        Button(
                            onClick = { showPermissionsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("open_permissions_dialog_btn")
                        ) {
                            Text("Explain Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Cyber Action Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "QUICK TERMINAL COMMANDS:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FF66),
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { runCyberCommand("nmap_scan") },
                            label = { Text("nmap scan", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            icon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.testTag("chip_nmap")
                        )
                        SuggestionChip(
                            onClick = { runCyberCommand("lock") },
                            label = { Text("online lock", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            icon = { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.testTag("chip_lock")
                        )
                        SuggestionChip(
                            onClick = { runCyberCommand("unlock") },
                            label = { Text("online unlock", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            icon = { Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.testTag("chip_unlock")
                        )
                        SuggestionChip(
                            onClick = { runCyberCommand("brute") },
                            label = { Text("brute force", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            icon = { Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.testTag("chip_brute")
                        )
                    }
                }
            }

            // Live Cyber Console Output Window
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF030A06).copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 320.dp)
                        .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF00FF66), CircleShape)
                                )
                                Text(
                                    text = "TERMINAL LOG STREAM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FF66),
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            TextButton(
                                onClick = { terminalLogs = emptyList() },
                                modifier = Modifier.testTag("clear_terminal_btn")
                            ) {
                                Text("Clear", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Divider(color = Color(0xFF00FF66).copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 6.dp))

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(terminalLogs) { log ->
                                Text(
                                    text = log,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = when {
                                        log.startsWith(">") -> Color(0xFF38BDF8)
                                        log.contains("ERROR") || log.contains("FAILED") -> Color(0xFFF87171)
                                        log.contains("SUCCESS") || log.contains("AUTHORIZED") -> Color(0xFF34D399)
                                        else -> Color(0xFF00FF66)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Command Input Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        placeholder = {
                            Text("Type command (e.g. lock, unlock, nmap)...", fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF66),
                            unfocusedBorderColor = Color(0xFF00FF66).copy(alpha = 0.3f),
                            focusedTextColor = Color(0xFF00FF66),
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("terminal_command_input")
                    )

                    Button(
                        onClick = {
                            if (commandInput.isNotBlank()) {
                                runCyberCommand(commandInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("send_command_btn")
                    ) {
                        Text("EXEC", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }

    // Educational Permissions Dialog Instance
    EducationalPermissionsDialog(
        showDialog = showPermissionsDialog,
        onDismiss = { showPermissionsDialog = false }
    )
}
