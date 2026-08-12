package com.example.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeviceEntity
import com.example.ui.SentinelViewModel
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.EmeraldNeon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSubscriptionScreen(
    viewModel: SentinelViewModel,
    devices: List<DeviceEntity>,
    modifier: Modifier = Modifier
) {
    var generatedOtpCode by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("brantonochieng345@gmail.com") }
    var isSubscribedPremium by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Enrolling pairing QR Code OTP section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Device Enrollment System (QR / OTP)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Enrolling a new personal device requires an authorized pairing OTP sequence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Real QR Code Display
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(2.dp, EmeraldNeon, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (generatedOtpCode.isNotEmpty()) {
                            com.example.ui.screens.QRCodeDisplay(
                                contentString = "sentinelx://enroll?token=$generatedOtpCode",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.QrCode,
                                contentDescription = "Pending OTP",
                                tint = Color.LightGray,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (generatedOtpCode.isNotEmpty()) {
                        Text(
                            text = "PAIRING CODE: $generatedOtpCode",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldNeon,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("otp_code_display")
                        )
                        Text(
                            text = "Code valid for 10:00 minutes • SHA256 hashed sign-in",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { generatedOtpCode = viewModel.generateNewPairingCode() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("generate_pairing_btn")
                    ) {
                        Text("Generate Authorized Pairing Token", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Account management details
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
                        text = "Account Identity & Subscription",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "OWNER IDENTITY:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SentinelX Premium Active",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Multi-device tracking • Infinite storage vault",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isSubscribedPremium,
                            onCheckedChange = { isSubscribedPremium = it },
                            modifier = Modifier.testTag("premium_toggle")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Plan Cost:", fontSize = 12.sp, color = Color.Gray)
                        Text(if (isSubscribedPremium) "$9.99 / month" else "$0.00 (Free Plan)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Server Health status Metrics
        item {
            Text(
                text = "Cloud Server Health Panel",
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ServerMetricHUDRow(serviceName = "Node.js Websocket Server", status = "Healthy", metric = "18ms")
                    ServerMetricHUDRow(serviceName = "PostgreSQL DB cluster", status = "Healthy", metric = "4ms")
                    ServerMetricHUDRow(serviceName = "Redis caching node", status = "Healthy", metric = "1ms")
                }
            }
        }
    }
}

@Composable
fun ServerMetricHUDRow(serviceName: String, status: String, metric: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = serviceName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text = "Response delay latency: $metric", fontSize = 11.sp, color = Color.Gray)
        }
        Box(
            modifier = Modifier
                .background(EmeraldNeon.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = status.uppercase(),
                fontSize = 9.sp,
                color = EmeraldNeon,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
