package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EducationalPermissionsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!showDialog) return

    val context = LocalContext.current
    var hasOverlayPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    
    var isAdminActive by remember {
        mutableStateOf(
            (context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager)
                .isAdminActive(android.content.ComponentName(context, com.example.receiver.SentinelDeviceAdminReceiver::class.java))
        )
    }
val runtimePermissionsGranted = permissionsState.allPermissionsGranted
    val allGranted = runtimePermissionsGranted && hasOverlayPermission && isAdminActive
    

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("educational_permissions_dialog"),
        shape = RoundedCornerShape(20.dp),
        containerColor = Color(0xFF0F172A), // Dark slate security console style
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "Security Shield",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "SECURITY ACCESS REQUIRED",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Educational Privacy Rationale",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "SentinelX requires explicit authorization for Camera and Location services to perform active threat detection and lost device tracking.",
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    lineHeight = 18.sp
                )

                // Rationale Card 1: Camera
                PermissionRationaleCard(
                    title = "Why Camera Access?",
                    description = "Captures automatic silent security snapshots whenever unauthorized access attempts or PIN bypass failures are detected.",
                    icon = Icons.Filled.PhotoCamera,
                    isGranted = permissionsState.permissions.find { it.permission == Manifest.permission.CAMERA }?.status?.isGranted == true,
                    testTag = "camera_rationale_card"
                )

                // Rationale Card 2: Location
                val locationGranted = permissionsState.permissions.filter {
                    it.permission == Manifest.permission.ACCESS_FINE_LOCATION || it.permission == Manifest.permission.ACCESS_COARSE_LOCATION
                }.any { it.status.isGranted }

                PermissionRationaleCard(
                    title = "Why Location Access?",
                    description = "Tracks real-time GPS coordinates during Lost Device Mode to pinpoint missing hardware on live security maps.",
                    icon = Icons.Filled.MyLocation,
                    isGranted = locationGranted,
                    testTag = "location_rationale_card"
                )

                // Rationale Card 3: Display Over Other Apps
                PermissionRationaleCard(
                    title = "Display Over Other Apps (Overlay)",
                    description = "Required so the anti-theft lock screen and wallpaper can draw over other apps during active Lost Mode.",
                    icon = Icons.Filled.Layers,
                    isGranted = hasOverlayPermission,
                    testTag = "overlay_rationale_card"
                )

                
                // Rationale Card 4: Device Admin
                PermissionRationaleCard(
                    title = "Device Administrator",
                    description = "Required to enforce device lock, wipe data on unauthorized access, and prevent app uninstallation by thieves.",
                    icon = Icons.Filled.Security,
                    isGranted = isAdminActive,
                    testTag = "admin_rationale_card"
                )
// Privacy Commitment Note
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Encrypted",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Zero Data Sharing: All captured metrics are encrypted locally and never transmitted to third parties.",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (allGranted) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.testTag("permissions_granted_close_btn")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Access Granted (Close)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        if (!runtimePermissionsGranted) {
                            permissionsState.launchMultiplePermissionRequest()
                        }
                        
                        if (!isAdminActive) {
                            val componentName = android.content.ComponentName(context, com.example.receiver.SentinelDeviceAdminReceiver::class.java)
                            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                                putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "SentinelX requires Device Admin access to enforce remote lock and wipe capabilities.")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
if (!hasOverlayPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("grant_permissions_request_btn")
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (!hasOverlayPermission && runtimePermissionsGranted) "Grant Overlay Permission" else "Grant Required Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!allGranted) {
                    TextButton(
                        onClick = {
                            // Open System Settings
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.testTag("open_settings_permissions_btn")
                    ) {
                        Text("App Settings", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_permissions_btn")
                ) {
                    Text("Dismiss", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    )
}

@Composable
private fun PermissionRationaleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0xFF064E3B).copy(alpha = 0.3f) else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isGranted) Color(0xFF10B981).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (isGranted) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        CircleShape
                    )
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Surface(
                        color = if (isGranted) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF334155),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = if (isGranted) "AUTHORIZED" else "REQUIRED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isGranted) Color(0xFF34D399) else Color(0xFFF59E0B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    lineHeight = 15.sp
                )
            }
        }
    }
}
