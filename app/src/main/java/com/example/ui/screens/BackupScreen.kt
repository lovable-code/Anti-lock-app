package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeviceEntity
import com.example.ui.SentinelViewModel
import com.example.ui.theme.EmeraldNeon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: SentinelViewModel,
    devices: List<DeviceEntity>,
    modifier: Modifier = Modifier
) {
    var backupIntervalHrs by remember { mutableStateOf(24) }
    var photoSyncEnabled by remember { mutableStateOf(true) }
    var docSyncEnabled by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Backup Status Banner
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
                                text = "Secure Cloud Vault",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Zero-Knowledge AES-256 local folder compression",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.CloudQueue,
                            contentDescription = "Cloud Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "LAST COMPLETED BACKUP: Today, 07:14 AM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldNeon,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }

        // Configuration Card
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
                        text = "Vault Backup Scheduler",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Backup Frequency: Every $backupIntervalHrs hours",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = backupIntervalHrs.toFloat(),
                        onValueChange = { backupIntervalHrs = it.toInt() },
                        valueRange = 4f..168f,
                        steps = 41,
                        modifier = Modifier.testTag("backup_interval_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sync items
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Photo, contentDescription = "Photos", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Compress & Backup Photos", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Only sync over Wi-Fi channels.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = photoSyncEnabled,
                            onCheckedChange = { photoSyncEnabled = it },
                            modifier = Modifier.testTag("photo_sync_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FolderZip, contentDescription = "Docs", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Compress & Backup Documents", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Target folder: /sdcard/Documents", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = docSyncEnabled,
                            onCheckedChange = { docSyncEnabled = it },
                            modifier = Modifier.testTag("doc_sync_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.triggerLocalBackup() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("backup_now_btn")
                    ) {
                        Text("Trigger Immediate Crypt-Backup", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Backup statistics
        item {
            Text(
                text = "Target Directory Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BackupDirMetric(folder = "/sdcard/DCIM", files = "1,420 files", size = "11.4 GB", status = "Syncing")
                    BackupDirMetric(folder = "/sdcard/Documents", files = "315 files", size = "112.5 MB", status = "Unqueued")
                    BackupDirMetric(folder = "/sdcard/Download/Invoices", files = "45 files", size = "4.1 MB", status = "Synchronized")
                }
            }
        }
    }
}

@Composable
fun BackupDirMetric(folder: String, files: String, size: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = folder, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text = "$files • $size", fontSize = 11.sp, color = Color.Gray)
        }
        Box(
            modifier = Modifier
                .background(
                    when (status) {
                        "Synchronized" -> EmeraldNeon.copy(alpha = 0.15f)
                        "Syncing" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else -> Color.Gray.copy(alpha = 0.15f)
                    },
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = status.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = when (status) {
                    "Synchronized" -> EmeraldNeon
                    "Syncing" -> MaterialTheme.colorScheme.primary
                    else -> Color.Gray
                }
            )
        }
    }
}
