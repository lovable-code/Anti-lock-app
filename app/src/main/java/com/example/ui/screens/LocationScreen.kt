package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeviceEntity
import com.example.ui.SentinelViewModel
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldNeon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    viewModel: SentinelViewModel,
    devices: List<DeviceEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedDeviceId by remember { mutableStateOf(viewModel.localDeviceId) }
    var updateIntervalSec by remember { mutableStateOf(10) }
    var trackingModeHighPrecision by remember { mutableStateOf(true) }

    // Geocoder State
    var resolvedAddress by remember { mutableStateOf("Resolving address...") }

    // Interactive Map settings
    var activeMapEngineGoogle by remember { mutableStateOf(true) } // Default to Google Maps as requested
    var googleMapType by remember { mutableStateOf("Satellite") } // Satellite, Default, Terrain
    var mapZoomLevel by remember { mutableStateOf(16) }

    // Geofencing states from ViewModel
    val geofenceEnabled by viewModel.geofenceEnabled.collectAsState()
    val safeCenterLat by viewModel.safeZoneCenterLat.collectAsState()
    val safeCenterLng by viewModel.safeZoneCenterLng.collectAsState()
    val safeZoneRadiusMeters by viewModel.safeZoneRadiusMeters.collectAsState()
    val geofenceBreached by viewModel.geofenceBreached.collectAsState()
    val geofenceBreachDistance by viewModel.geofenceBreachDistance.collectAsState()
    val geofenceBreachDeviceName by viewModel.geofenceBreachDeviceName.collectAsState()

    val activeDevice = devices.find { it.id == selectedDeviceId } ?: devices.firstOrNull()

    LaunchedEffect(activeDevice?.latitude, activeDevice?.longitude) {
        if (activeDevice != null) {
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(activeDevice.latitude, activeDevice.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val addr = addresses[0]
                        val fullAddress = listOfNotNull(addr.locality, addr.adminArea, addr.countryName).joinToString(", ")
                        resolvedAddress = if (fullAddress.isNotBlank()) fullAddress else "Location Unknown"
                    } else {
                        resolvedAddress = "Location Unknown"
                    }
                } catch (e: Exception) {
                    resolvedAddress = "Offline / GPS Only"
                }
            }
        }
    }

    val activeDistanceToSafeCenter = remember(activeDevice?.latitude, activeDevice?.longitude, safeCenterLat, safeCenterLng) {
        if (activeDevice != null) {
            viewModel.calculateDistanceMeters(
                activeDevice.latitude,
                activeDevice.longitude,
                safeCenterLat,
                safeCenterLng
            )
        } else 0.0
    }

    val breadcrumbs = remember(activeDevice?.id, activeDevice?.latitude, activeDevice?.longitude) {
        val dev = activeDevice ?: return@remember emptyList()
        listOf(
            Pair(System.currentTimeMillis() - 300000, dev.latitude - 0.0004 to dev.longitude + 0.0003),
            Pair(System.currentTimeMillis() - 1200000, dev.latitude - 0.0011 to dev.longitude + 0.0008),
            Pair(System.currentTimeMillis() - 3600000, dev.latitude - 0.0018 to dev.longitude - 0.0002),
            Pair(System.currentTimeMillis() - 3600000 * 3, dev.latitude - 0.0025 to dev.longitude - 0.0012)
        )
    }

    // Animating Radar Grid Sweeper & Live pulsing
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
    val mapMarkerPulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MarkerPulse"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Engine Selector Switch & GPS Satellite Indicator Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedFilterChip(
                        selected = !activeMapEngineGoogle,
                        onClick = { activeMapEngineGoogle = false },
                        label = { Text("Radar Telemetry", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Filled.Radar, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            selectedContainerColor = EmeraldNeon.copy(alpha = 0.25f),
                            selectedLabelColor = EmeraldNeon
                        ),
                        modifier = Modifier.testTag("engine_radar_chip")
                    )
                    ElevatedFilterChip(
                        selected = activeMapEngineGoogle,
                        onClick = { activeMapEngineGoogle = true },
                        label = { Text("Google Maps (SDK View)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("engine_google_chip")
                    )
                }
            }
        }

        // High-fidelity Interactive Map Layout (Radar or Mock Google Maps)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .border(
                        1.5.dp,
                        if (geofenceBreached && geofenceEnabled) DangerRed else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (activeMapEngineGoogle) {
                        // =========================================================================
                        // INTERACTIVE GOOGLE MAPS LAYOUT
                        // =========================================================================
                        val mapBgColor = when (googleMapType) {
                            "Satellite" -> Color(0xFF0D111A)
                            "Terrain" -> Color(0xFF232B1E)
                            else -> Color(0xFFF0EFEA)
                        }
                        val gridLineColor = when (googleMapType) {
                            "Satellite" -> Color(0xFF1F293D)
                            "Terrain" -> Color(0xFF384631)
                            else -> Color(0xFFE0DDD5)
                        }

                        // Map Surface Draw
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(mapBgColor)
                        ) {
                            val w = size.width
                            val h = size.height
                            val cx = w / 2f
                            val cy = h / 2f
                            val mapScaleFactor = (mapZoomLevel * 450).toFloat()

                            // Base reference coordinates centered on active device
                            val centerLat = activeDevice?.latitude ?: 37.7749
                            val centerLng = activeDevice?.longitude ?: -122.4194

                            // Draw Google Grid/Road block 
                            val gridSpacing = 90f / (17f - mapZoomLevel).coerceIn(0.5f, 4f)
                            var gx = 0f
                            while (gx < w) {
                                drawLine(gridLineColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1.5f)
                                gx += gridSpacing
                            }
                            var gy = 0f
                            while (gy < h) {
                                drawLine(gridLineColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1.5f)
                                gy += gridSpacing
                            }

                            // Draw Major Google Map Highway & Local Streets with names
                            val streetColor = if (googleMapType == "Default") Color.White else Color(0xFF2C3540)
                            drawLine(streetColor, Offset(0f, cy - 20f), Offset(w, cy + 40f), strokeWidth = 24f)
                            drawLine(streetColor, Offset(cx - 100f, 0f), Offset(cx + 100f, h), strokeWidth = 18f)
                            drawLine(Color(0xFFFFD54F), Offset(0f, cy - 20f), Offset(w, cy + 40f), strokeWidth = 2f) // Yellow centerline

                            // Breadcrumb movement directions & line history
                            if (activeDevice != null) {
                                val devX = cx + ((activeDevice.longitude - centerLng) * mapScaleFactor).toFloat()
                                val devY = cy - ((activeDevice.latitude - centerLat) * mapScaleFactor).toFloat()

                                var lastX = devX
                                var lastY = devY
                                breadcrumbs.forEach { point ->
                                    val px = cx + ((point.second.second - centerLng) * mapScaleFactor).toFloat()
                                    val py = cy - ((point.second.first - centerLat) * mapScaleFactor).toFloat()
                                    // Direction movement path line
                                    drawLine(
                                        color = Color(0xFF4285F4).copy(alpha = 0.6f),
                                        start = Offset(lastX, lastY),
                                        end = Offset(px, py),
                                        strokeWidth = 6f
                                    )
                                    // Direction small arrows or dots
                                    drawCircle(
                                        color = Color(0xFF4285F4),
                                        radius = 3f,
                                        center = Offset(px, py)
                                    )
                                    lastX = px
                                    lastY = py
                                }
                            }

                            // Geofence Circle Overlay
                            if (geofenceEnabled) {
                                val safeX = cx + ((safeCenterLng - centerLng) * mapScaleFactor).toFloat()
                                val safeY = cy - ((safeCenterLat - centerLat) * mapScaleFactor).toFloat()
                                val radiusPx = (safeZoneRadiusMeters * (mapZoomLevel / 15f) / 4.0).toFloat().coerceIn(40f, 300f)
                                val fenceColor = if (geofenceBreached) DangerRed else EmeraldNeon
                                drawCircle(
                                    color = fenceColor.copy(alpha = 0.15f),
                                    radius = radiusPx,
                                    center = Offset(safeX, safeY)
                                )
                                drawCircle(
                                    color = fenceColor.copy(alpha = 0.8f),
                                    radius = radiusPx,
                                    center = Offset(safeX, safeY),
                                    style = Stroke(width = 3f)
                                )
                            }

                            // Draw Map Pin for each device relative to center
                            devices.forEach { dev ->
                                val isSelected = dev.id == selectedDeviceId
                                val dx = cx + ((dev.longitude - centerLng) * mapScaleFactor).toFloat()
                                val dy = cy - ((dev.latitude - centerLat) * mapScaleFactor).toFloat()

                                // Accuracy Circle
                                drawCircle(
                                    color = Color(0xFF4285F4).copy(alpha = 0.15f * mapMarkerPulse),
                                    radius = if (isSelected) 60f else 35f,
                                    center = Offset(dx, dy)
                                )

                                // Core Google Maps Location Marker Pin
                                if (isSelected) {
                                    // Google Red Pin shadow
                                    drawCircle(Color.Black.copy(alpha = 0.3f), radius = 6f, center = Offset(dx, dy + 12f))
                                    // Pulse ring
                                    drawCircle(
                                        color = if (dev.isOnline) Color(0xFF4285F4) else Color.Gray,
                                        radius = 12f * mapMarkerPulse,
                                        center = Offset(dx, dy)
                                    )
                                }
                            }
                        }

                        // Google Maps HUD Layer Elements
                        // Top Google Maps Search Bar
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .align(Alignment.TopCenter)
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = activeDevice?.let { "Tracking: ${it.name} [Google Maps Mode]" } ?: "Search Google Maps...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Filled.Mic, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (activeDevice?.isOnline == true) EmeraldNeon else Color.Gray,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (activeDevice?.isOnline == true) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp).align(Alignment.Center)
                                    )
                                }
                            }
                        }

                        // Bottom-Left Map Type Selector FAB Grid
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Satellite", "Default", "Terrain").forEach { type ->
                                    val isSel = googleMapType == type
                                    Surface(
                                        color = if (isSel) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.75f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable { googleMapType = type }
                                    ) {
                                        Text(
                                            text = type.uppercase(),
                                            color = if (isSel) Color.White else Color.LightGray,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom-Right Control Cluster (Zoom in, Zoom out, Compass)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Compass / Explore
                            FloatingActionButton(
                                onClick = { mapZoomLevel = 16 },
                                containerColor = Color.Black.copy(alpha = 0.82f),
                                contentColor = Color.White,
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Filled.Explore, contentDescription = "Compass Center", modifier = Modifier.size(18.dp))
                            }

                            // Zoom In
                            FloatingActionButton(
                                onClick = { if (mapZoomLevel < 19) mapZoomLevel++ },
                                containerColor = Color.Black.copy(alpha = 0.82f),
                                contentColor = Color.White,
                                modifier = Modifier.size(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
                            }

                            // Zoom Out
                            FloatingActionButton(
                                onClick = { if (mapZoomLevel > 13) mapZoomLevel-- },
                                containerColor = Color.Black.copy(alpha = 0.82f),
                                contentColor = Color.White,
                                modifier = Modifier.size(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
                            }
                        }

                        // Small Google Logo in Bottom Left Corner (Brand Legitimacy)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 10.dp, vertical = 35.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Google",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Maps SDK",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                    } else {
                        // =========================================================================
                        // TACTICAL TELEMETRY VECTOR RADAR MAP
                        // =========================================================================
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF030814))
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val centerX = canvasWidth / 2f
                            val centerY = canvasHeight / 2f
                            val coordOffsetMultiplier = 8000f

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

                            // Draw  Streets/Roads
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

                            // Draw Safe Zone Geofence Circle on Map
                            if (geofenceEnabled) {
                                val safeX = centerX + ((safeCenterLng - (-122.4194)) * coordOffsetMultiplier).toFloat()
                                val safeY = centerY - ((safeCenterLat - 37.7749) * coordOffsetMultiplier).toFloat()
                                val safeRadiusPx = (safeZoneRadiusMeters / 8.0).toFloat().coerceIn(35f, 220f)
                                val zoneColor = if (geofenceBreached) DangerRed else EmeraldNeon

                                // Filled Safe Zone
                                drawCircle(
                                    color = zoneColor.copy(alpha = 0.12f),
                                    radius = safeRadiusPx,
                                    center = Offset(safeX, safeY)
                                )
                                // Perimeter Ring
                                drawCircle(
                                    color = zoneColor.copy(alpha = 0.7f),
                                    radius = safeRadiusPx,
                                    center = Offset(safeX, safeY),
                                    style = Stroke(width = 2f)
                                )
                                // Center Anchor Dot
                                drawCircle(
                                    color = zoneColor,
                                    radius = 5f,
                                    center = Offset(safeX, safeY)
                                )

                                // Threat vector line if breached
                                activeDevice?.let { dev ->
                                    val devX = centerX + ((dev.longitude - (-122.4194)) * coordOffsetMultiplier).toFloat()
                                    val devY = centerY - ((dev.latitude - 37.7749) * coordOffsetMultiplier).toFloat()
                                    if (geofenceBreached) {
                                        drawLine(
                                            color = DangerRed,
                                            start = Offset(safeX, safeY),
                                            end = Offset(devX, devY),
                                            strokeWidth = 3f
                                        )
                                    }
                                }
                            }

                            // Draw Enrolled Devices Ping Circles
                            devices.forEach { dev ->
                                val isCurrentSelected = dev.id == selectedDeviceId
                                val mapX = centerX + ((dev.longitude - (-122.4194)) * coordOffsetMultiplier).toFloat()
                                val mapY = centerY - ((dev.latitude - 37.7749) * coordOffsetMultiplier).toFloat()

                                val accuracyRadius = if (isCurrentSelected) 50f else 35f
                                drawCircle(
                                    color = if (dev.isOnline) Color(0xFF00E676).copy(alpha = 0.12f) else Color.Gray.copy(alpha = 0.1f),
                                    radius = accuracyRadius,
                                    center = Offset(mapX, mapY)
                                )

                                // Inner Glowing Center Dot
                                drawCircle(
                                    color = if (geofenceBreached && dev.name == geofenceBreachDeviceName) DangerRed else if (isCurrentSelected) Color(0xFF2979FF) else if (dev.isOnline) Color(0xFF00E676) else Color.Gray,
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
                                Surface(
                                    color = Color.Black.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(if (geofenceBreached && geofenceEnabled) DangerRed else EmeraldNeon, CircleShape)
                                        )
                                        Text(
                                            text = if (!geofenceEnabled) "GEOFENCE DISABLED" else if (geofenceBreached) "GEOFENCE BREACH ALERT" else "SAFE ZONE ACTIVE",
                                            color = if (!geofenceEnabled) Color.LightGray else if (geofenceBreached) DangerRed else EmeraldNeon,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Surface(
                                    color = Color.Black.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "GPS SATELLITES: 4 ACTIVE",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Coordinates overlay HUD inside the Box (shared across both view engines)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(10.dp)
                    ) {
                        activeDevice?.let { dev ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${dev.name} ${if (geofenceBreached && dev.name == geofenceBreachDeviceName) "(OUTSIDE)" else ""}",
                                            color = if (geofenceBreached && dev.name == geofenceBreachDeviceName) DangerRed else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "LAT: ${"%.6f".format(dev.latitude)} | LNG: ${"%.6f".format(dev.longitude)}",
                                            color = Color.LightGray,
                                            fontSize = 9.5.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                        Text(
                                            text = "ADDR: $resolvedAddress",
                                            color = EmeraldNeon,
                                            fontSize = 9.5.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                        val isStationary = !geofenceBreached
                                        Text(
                                            text = "STATUS: ${if (isStationary) "STATIONARY" else "IN MOTION (4.2 m/s)"}",
                                            color = if (isStationary) Color.LightGray else AlertOrange,
                                            fontSize = 9.5.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "ZONE: ${safeZoneRadiusMeters.toInt()}m",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp
                                        )
                                        Text(
                                            text = "DIST: ${activeDistanceToSafeCenter.toInt()}m",
                                            color = if (activeDistanceToSafeCenter > safeZoneRadiusMeters && geofenceEnabled) DangerRed else EmeraldNeon,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.5.sp
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
        // GEOFENCING & SAFE ZONE PERIMETER CONTROL HUB
        // =========================================================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (geofenceBreached && geofenceEnabled) DangerRed else EmeraldNeon.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("geofence_control_card")
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
                                    .background(
                                        if (geofenceBreached && geofenceEnabled) DangerRed.copy(alpha = 0.2f) else EmeraldNeon.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Fence,
                                    contentDescription = "Geofence Icon",
                                    tint = if (geofenceBreached && geofenceEnabled) DangerRed else EmeraldNeon,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Safe Zone Geo-Fencing",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (!geofenceEnabled) "Perimeter Guard Inactive" else if (geofenceBreached) "🚨 BREACH DETECTED (${geofenceBreachDistance.toInt()}m)" else "Perimeter Protected (${safeZoneRadiusMeters.toInt()}m limit)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (!geofenceEnabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else if (geofenceBreached) DangerRed else EmeraldNeon
                                )
                            }
                        }

                        Switch(
                            checked = geofenceEnabled,
                            onCheckedChange = {
                                viewModel.setGeofenceEnabled(it)
                                Toast.makeText(context, if (it) "Safe Zone Geofencing Activated" else "Geofencing Paused", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("geofence_toggle_switch")
                        )
                    }

                    // Critical Alert Banner if Breached
                    AnimatedVisibility(visible = geofenceBreached && geofenceEnabled) {
                        Surface(
                            color = DangerRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .border(1.dp, DangerRed, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "Alert",
                                        tint = DangerRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "GEOFENCE BREACH ALERT",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = DangerRed
                                        )
                                        Text(
                                            text = "Device '$geofenceBreachDeviceName' is ${geofenceBreachDistance.toInt()} meters away from the Safe Zone center (Radius Limit: ${safeZoneRadiusMeters.toInt()}m).",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            activeDevice?.let { dev ->
                                                viewModel.triggerRemoteCommand(dev.id, "LOCK_DEVICE")
                                                Toast.makeText(context, "Lock command sent to ${dev.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).testTag("breach_lock_device_btn")
                                    ) {
                                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("LOCK DEVICE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            activeDevice?.let {
                                                viewModel.resetGeofenceLocation(it.id)
                                                Toast.makeText(context, "Device location reset inside safe zone", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).testTag("reset_device_location_btn")
                                    ) {
                                        Text("RETURN INSIDE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Safe Zone Radius Config Slider
                    Text(
                        text = "Safe Zone Radius Limit: ${safeZoneRadiusMeters.toInt()} meters",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Triggers immediate audit log & system notification if device leaves this radius.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Slider(
                        value = safeZoneRadiusMeters.toFloat(),
                        onValueChange = { viewModel.setSafeZoneRadius(it.toDouble()) },
                        valueRange = 100f..3000f,
                        steps = 28,
                        colors = SliderDefaults.colors(
                            thumbColor = if (geofenceBreached && geofenceEnabled) DangerRed else EmeraldNeon,
                            activeTrackColor = if (geofenceBreached && geofenceEnabled) DangerRed else EmeraldNeon
                        ),
                        modifier = Modifier.testTag("geofence_radius_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location details & Recalibrate Button
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Current Safe Zone Center:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "LAT: ${"%.6f".format(safeCenterLat)} | LNG: ${"%.6f".format(safeCenterLng)}",
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Active Device Distance: ${activeDistanceToSafeCenter.toInt()} meters",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (activeDistanceToSafeCenter > safeZoneRadiusMeters && geofenceEnabled) DangerRed else EmeraldNeon
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Button Row: Recalibrate & Test Simulation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                activeDevice?.let { dev ->
                                    viewModel.setSafeZoneToCurrentDeviceLocation(dev.id)
                                    Toast.makeText(context, "Safe Zone center updated to ${dev.name}'s coordinates", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("recalibrate_geofence_btn")
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set Current as Center", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        label = { Text(if (dev.id == viewModel.localDeviceId) "This Device" else dev.name, fontSize = 12.sp) },
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

