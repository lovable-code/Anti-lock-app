package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CommandEntity
import com.example.data.DeviceEntity
import com.example.ui.SentinelViewModel
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.EmeraldNeon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FCMConsoleScreen(
    viewModel: SentinelViewModel,
    devices: List<DeviceEntity>,
    commands: List<CommandEntity>,
    modifier: Modifier = Modifier
) {
    var selectedDeviceId by remember { mutableStateOf(viewModel.localDeviceId) }
    var selectedCommandType by remember { mutableStateOf("GET_STATUS") }

    val activeDevice = devices.find { it.id == selectedDeviceId } ?: devices.firstOrNull()

    val commandTypes = listOf(
        "GET_STATUS",
        "REQUEST_LOCATION",
        "LOCK_DEVICE",
        "START_LOST_MODE",
        "PLAY_ALARM",
        "WIPE_DEVICE"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Socket Connection Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FCM Cloud Tunnel Console",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(EmeraldNeon, RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "wss://sentinelx.secure/tunnel (Encrypted TLSv1.3)",
                                    fontSize = 11.sp,
                                    color = EmeraldNeon,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Filled.WifiTethering,
                            contentDescription = "Socket Connected",
                            tint = EmeraldNeon,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricHUDItem(label = "PACKET LOSS", value = "0.00%", color = EmeraldNeon)
                        MetricHUDItem(label = "PING RTT", value = "18 ms", color = MaterialTheme.colorScheme.primary)
                        MetricHUDItem(label = "CIPHER LAYER", value = "AES_256_GCM", color = EmeraldNeon)
                    }
                }
            }
        }

        // Interactive Payload Generator Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Encrypted Command Transceiver",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Generate and sign remote instructions. FCM Cloud commands will cascade in real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Select device
                    Text(
                        text = "Target Device:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        devices.forEach { dev ->
                            val isSelected = dev.id == selectedDeviceId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDeviceId = dev.id },
                                label = { Text(if (dev.id == viewModel.localDeviceId) "Local Agent" else dev.name, fontSize = 11.sp) },
                                modifier = Modifier.testTag("ws_device_chip_${dev.id}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Select Command Type
                    Text(
                        text = "Command Method Payload:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        commandTypes.chunked(2).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chunk.forEach { cmd ->
                                    val isSelected = cmd == selectedCommandType
                                    Button(
                                        onClick = { selectedCommandType = cmd },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .border(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .testTag("cmd_type_$cmd"),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(cmd, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Send command button
                    Button(
                        onClick = {
                            val payload = if (selectedCommandType == "START_LOST_MODE") {
                                mapOf("message" to "Device Lost Mode deployed.", "contact" to "+1-555-0199")
                            } else emptyMap()
                            viewModel.triggerRemoteCommand(selectedDeviceId, selectedCommandType, payload)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("send_payload_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign & Inject Secure Payload", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live Stepper Flowchart for the latest command
        val latestCommand = commands.firstOrNull { it.targetDeviceId == selectedDeviceId }
        latestCommand?.let { cmd ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "FCM Cloud Packet Stepper Flow",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val status = cmd.status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StepIndicator(name = "Queued", active = true, completed = status != "Pending")
                            StepConnector(active = status != "Pending")
                            StepIndicator(name = "Sent", active = status != "Pending", completed = status == "Received" || status == "Completed")
                            StepConnector(active = status == "Received" || status == "Completed")
                            StepIndicator(name = "Verified", active = status == "Received" || status == "Completed", completed = status == "Completed")
                            StepConnector(active = status == "Completed")
                            StepIndicator(name = "Done", active = status == "Completed", completed = status == "Completed")
                        }
                    }
                }
            }
        }

        // Rolling Encrypted Packet Logs Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payload Audit Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { viewModel.clearLogs() },
                    modifier = Modifier.testTag("clear_logs_btn")
                ) {
                    Text("Clear Logs")
                }
            }
        }

        // Log Console entries
        if (commands.isEmpty()) {
            item {
                Text(
                    text = "No FCM Cloud frames sent yet. Inject a payload to monitor crypt-handshakes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(commands) { cmd ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF030814)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Encrypted",
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cmd.type,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = cmd.status.uppercase(),
                                color = if (cmd.status == "Completed") EmeraldNeon else AlertOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "SHA256 SIG: ${cmd.signature}",
                            fontSize = 9.sp,
                            color = Color.LightGray.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "JSON RAW: ${cmd.payloadJson}",
                            fontSize = 9.sp,
                            color = Color.LightGray.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Timestamp: ${SimpleDateFormat("HH:mm:ss.SSS (MMM dd)", Locale.getDefault()).format(Date(cmd.timestamp))}",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricHUDItem(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun StepIndicator(name: String, active: Boolean, completed: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    if (completed) EmeraldNeon else if (active) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (completed) Icons.Filled.Check else Icons.Filled.Adjust,
                contentDescription = name,
                tint = if (completed) Color.Black else Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun RowScope.StepConnector(active: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(2.dp)
            .background(if (active) EmeraldNeon else Color.Gray.copy(alpha = 0.2f))
    )
}
