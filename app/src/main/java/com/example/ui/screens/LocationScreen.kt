package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
fun LocationScreen(
    viewModel: SentinelViewModel,
    devices: List<DeviceEntity>,
    modifier: Modifier = Modifier
) {
    var selectedDeviceId by remember { mutableStateOf("sentinel-agent-local") }
    var updateIntervalSec by remember { mutableStateOf(10) }
    var trackingModeHighPrecision by remember { mutableStateOf(true) }

    val activeDevice = devices.find { it.id == selectedDeviceId } ?: devices.firstOrNull()

    val breadcrumbs = remember(activeDevice?.id, activeDevice?.latitude, activeDevice?.longitude) {
        val dev = activeDevice ?: return@remember emptyList()
        listOf(
            Pair(System.currentTimeMillis() - 300000, dev.latitude - 0.0004 to dev.longitude + 0.0003),
            Pair(System.currentTimeMillis() - 1200000, dev.latitude - 0.0011 to dev.longitude + 0.0008),
            Pair(System.currentTimeMillis() - 3600000, dev.latitude - 0.0018 to dev.longitude - 0.0002),
            Pair(System.currentTimeMillis() - 3600000 * 3, dev.latitude - 0.0025 to dev.longitude - 0.0012)
        )
    }

    // Animating Radar Grid Sweeper
    val transition = rememberInfiniteTransition(label = "RadarSweep")
    val radarProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweepFloat"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-fidelity Canvas Map Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Custom Draw Canvas Vector Grid Map
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF030814))
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val centerX = canvasWidth / 2f
                        val centerY = canvasHeight / 2f

                        // Draw Grid Coordinates
                        val gridSpacing = 60f
                        var x = 0f
                        while (x < canvasWidth) {
                            drawLine(
                                color = Color(0xFF00E676).copy(alpha = 0.05f),
                                start = Offset(x, 0f),
                                end = Offset(x, canvasHeight),
                                strokeWidth = 1f
                            )
                            x += gridSpacing
                        }
                        var y = 0f
                        while (y < canvasHeight) {
                            drawLine(
                                color = Color(0xFF00E676).copy(alpha = 0.05f),
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1f
                            )
                            y += gridSpacing
                        }

                        // Draw Simulated Streets/Roads
                        drawLine(Color(0xFF2979FF).copy(alpha = 0.12f), Offset(0f, centerY - 80), Offset(canvasWidth, centerY + 120), strokeWidth = 6f)
                        drawLine(Color(0xFF2979FF).copy(alpha = 0.12f), Offset(centerX - 150, 0f), Offset(centerX + 80, canvasHeight), strokeWidth = 6f)
                        drawLine(Color(0xFF2979FF).copy(alpha = 0.12f), Offset(centerX + 200, 0f), Offset(centerX - 240, canvasHeight), strokeWidth = 4f)

                        // Draw Radar sweeping circle waves
                        val maxRadarRadius = Math.max(canvasWidth, canvasHeight) / 2.5f
                        drawCircle(
                            color = Color(0xFF00E676).copy(alpha = 0.2f * (1f - radarProgress)),
                            radius = maxRadarRadius * radarProgress,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 2f)
                        )
                        drawCircle(
                            color = Color(0xFF00E676).copy(alpha = 0.08f),
                            radius = maxRadarRadius,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 1f)
                        )

                        // Draw Enrolled Devices Ping Circles
                        devices.forEach { dev ->
                            val isCurrentSelected = dev.id == selectedDeviceId
                            // Offset coordinates around central mapping zone based on device coordinates
                            val coordOffsetMultiplier = 8000f
                            val mapX = centerX + ((dev.longitude - (-122.4194)) * coordOffsetMultiplier).toFloat()
                            val mapY = centerY - ((dev.latitude - 37.7749) * coordOffsetMultiplier).toFloat()

                            // Draw location accuracy halo
                            val accuracyRadius = if (isCurrentSelected) 50f else 35f
                            drawCircle(
                                color = if (dev.isOnline) Color(0xFF00E676).copy(alpha = 0.12f) else Color.Gray.copy(alpha = 0.1f),
                                radius = accuracyRadius,
                                center = Offset(mapX, mapY)
                            )

                            // Inner Glowing Center Dot
                            drawCircle(
                                color = if (isCurrentSelected) Color(0xFF2979FF) else if (dev.isOnline) Color(0xFF00E676) else Color.Gray,
                                radius = if (isCurrentSelected) 8f else 5f,
                                center = Offset(mapX, mapY)
                            )
                        }
                    }

                    // Tactical HUD Layer Overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SECURE GPS TUNNEL ACTIVE",
                                    color = EmeraldNeon,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "3 SATELLITES FEED",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Coordinates HUD at bottom
                        activeDevice?.let { dev ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = dev.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "LAT: ${"%.6f".format(dev.latitude)}  LNG: ${"%.6f".format(dev.longitude)}",
                                            color = Color.LightGray,
                                            fontSize = 10.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = "ACCURACY: ±${"%.1f".format(dev.locationAccuracyMeters)}m",
                                        color = EmeraldNeon,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Device Selector Toggles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                devices.forEach { dev ->
                    val isSelected = dev.id == selectedDeviceId
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDeviceId = dev.id },
                        label = { Text(if (dev.id == "sentinel-agent-local") "This Device" else dev.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("location_chip_${dev.id}")
                    )
                }
            }
        }

        // Interval & Power Savings Panel
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
                        text = "Battery-Aware Tracking Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Optimize update frequency to conserve hardware battery cells remotely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Update Interval Slider
                    Text(
                        text = "Location Sync Interval: $updateIntervalSec seconds",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = updateIntervalSec.toFloat(),
                        onValueChange = { updateIntervalSec = it.toInt() },
                        valueRange = 5f..120f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("interval_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tracking mode switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "High-Precision GPS Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Uses GNSS triangulation. Higher battery discharge.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = trackingModeHighPrecision,
                            onCheckedChange = { trackingModeHighPrecision = it },
                            modifier = Modifier.testTag("gps_precision_switch")
                        )
                    }
                }
            }
        }

        // Location Breadcrumbs Title
        item {
            Text(
                text = "Breadcrumbs History Tracking (Last 12 hours)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Mock Breadcrumbs Timeline
        items(breadcrumbs) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "Breadcrumb",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Geofence Ping Reported: Verified",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Coords: (${"%.6f".format(item.second.first)}, ${"%.6f".format(item.second.second)}) — Last updated ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(item.first))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
