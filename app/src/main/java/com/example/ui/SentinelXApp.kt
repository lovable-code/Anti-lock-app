package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.SentinelDatabase
import com.example.data.SentinelRepository
import com.example.ui.components.MatrixRainEffect
import com.example.ui.screens.*
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SlateDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentinelXApp(
    viewModel: SentinelViewModel = viewModel(
        factory = SentinelViewModelFactory(
            LocalContext.current,
            SentinelRepository(SentinelDatabase.getDatabase(LocalContext.current).sentinelDao())
        )
    )
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val commands by viewModel.commands.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()

    val localDevice = devices.find { it.id == "sentinel-agent-local" }
    val context = LocalContext.current

    // Launch Foreground Service for persistent remote connection & anti-theft monitoring
    LaunchedEffect(Unit) {
        try {
            com.example.service.SentinelForegroundService.startService(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var currentScreen by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Passcode states for app login gate & device bypass lock screen
    var keyboardInputPin by remember { mutableStateOf("") }
    var inputErrorState by remember { mutableStateOf(false) }

    MyApplicationTheme(
        isMatrixTheme = themeMode == SentinelViewModel.AppThemeMode.MATRIX,
        darkTheme = themeMode == SentinelViewModel.AppThemeMode.DARK
    ) {
        if (!isAppUnlocked) {
            // APP STARTUP LOGIN / OWNER ACCESS GATE SCREEN
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (themeMode == SentinelViewModel.AppThemeMode.MATRIX) {
                    MatrixRainEffect(modifier = Modifier.fillMaxSize())
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (themeMode == SentinelViewModel.AppThemeMode.MATRIX)
                                Color.Black.copy(alpha = 0.82f)
                            else MaterialTheme.colorScheme.background
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(EmeraldNeon.copy(alpha = 0.15f), CircleShape)
                                .border(2.dp, EmeraldNeon, CircleShape)
                                .padding(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = "SentinelX Lock",
                                tint = EmeraldNeon,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "SENTINELX OWNER GATE",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = 2.sp
                        )

                        Text(
                            text = "Owner Passcode Required (Default: 1234 or 2026)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        Text(
                            text = if (inputErrorState) "INVALID OWNER PIN • TRY 1234" else "ENTER OWNER PIN CODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (inputErrorState) DangerRed else EmeraldNeon,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // PIN Display dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(bottom = 20.dp)
                        ) {
                            repeat(4) { idx ->
                                val active = keyboardInputPin.length > idx
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(
                                            if (active) EmeraldNeon else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (active) EmeraldNeon else Color.Transparent,
                                            CircleShape
                                        )
                                )
                            }
                        }

                        // Grid 3x4 Keypad
                        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Clear", "0", "Enter")
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(260.dp)
                        ) {
                            keys.chunked(3).forEach { rowKeys ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowKeys.forEach { key ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surface,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    EmeraldNeon.copy(alpha = 0.3f),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    inputErrorState = false
                                                    when (key) {
                                                        "Clear" -> keyboardInputPin = ""
                                                        "Enter" -> {
                                                            val success = viewModel.authenticateOwner(keyboardInputPin)
                                                            if (!success) {
                                                                keyboardInputPin = ""
                                                                inputErrorState = true
                                                            } else {
                                                                keyboardInputPin = ""
                                                            }
                                                        }
                                                        else -> {
                                                            if (keyboardInputPin.length < 4) {
                                                                keyboardInputPin += key
                                                            }
                                                        }
                                                    }
                                                }
                                                .testTag("app_pin_key_$key"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = key,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Biometric / Demo Unlock Bypass Button
                        OutlinedButton(
                            onClick = { viewModel.authenticateOwner("1234") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = EmeraldNeon.copy(alpha = 0.12f),
                                contentColor = EmeraldNeon
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldNeon.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .testTag("biometric_quick_unlock_btn")
                        ) {
                            Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Biometric / Quick Owner Login", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.width(280.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Drawer header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "SentinelX",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SENTINELX",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Owner Command Hub",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Drawer Items
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.CloudQueue, contentDescription = "Backups") },
                    label = { Text("Secure Cloud Backup") },
                    selected = currentScreen == 4,
                    onClick = {
                        currentScreen = 4
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).testTag("drawer_backup_item")
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Admin") },
                    label = { Text("Admin & Billing") },
                    selected = currentScreen == 5,
                    onClick = {
                        currentScreen = 5
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).testTag("drawer_admin_item")
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.IntegrationInstructions, contentDescription = "Developer") },
                    label = { Text("Developer Specs") },
                    selected = currentScreen == 6,
                    onClick = {
                        currentScreen = 6
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).testTag("drawer_dev_item")
                )

                NavigationDrawerItem(
                    icon = { Icon(painter = painterResource(id = R.drawable.ic_hacker), contentDescription = "Hacking Terminal", modifier = Modifier.size(20.dp)) },
                    label = { Text("Cyber Terminal & Matrix") },
                    selected = currentScreen == 7,
                    onClick = {
                        currentScreen = 7
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).testTag("drawer_hacking_item")
                )

                // Global Matrix Theme Selector
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "THEME ENGINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = themeMode == SentinelViewModel.AppThemeMode.MATRIX,
                            onClick = { viewModel.setThemeMode(SentinelViewModel.AppThemeMode.MATRIX) },
                            label = { Text("Matrix", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            leadingIcon = { Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.weight(1f).testTag("matrix_theme_chip")
                        )
                        FilterChip(
                            selected = themeMode == SentinelViewModel.AppThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(SentinelViewModel.AppThemeMode.DARK) },
                            label = { Text("Dark", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag("dark_theme_chip")
                        )
                        FilterChip(
                            selected = themeMode == SentinelViewModel.AppThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(SentinelViewModel.AppThemeMode.LIGHT) },
                            label = { Text("Light", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag("light_theme_chip")
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Drawer footer
                Text(
                    text = "SentinelX Security Suite v2.6.1\nCryptographic layer: RSA_AES_SHA256",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SENTINEL",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "X",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("menu_drawer_btn")
                            ) {
                                Icon(Icons.Filled.Menu, contentDescription = "Open Drawer")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    val nextMode = when (themeMode) {
                                        SentinelViewModel.AppThemeMode.MATRIX -> SentinelViewModel.AppThemeMode.DARK
                                        SentinelViewModel.AppThemeMode.DARK -> SentinelViewModel.AppThemeMode.LIGHT
                                        SentinelViewModel.AppThemeMode.LIGHT -> SentinelViewModel.AppThemeMode.MATRIX
                                    }
                                    viewModel.setThemeMode(nextMode)
                                },
                                modifier = Modifier.testTag("theme_switcher_top_btn")
                            ) {
                                Icon(
                                    imageVector = when (themeMode) {
                                        SentinelViewModel.AppThemeMode.MATRIX -> Icons.Filled.Code
                                        SentinelViewModel.AppThemeMode.DARK -> Icons.Filled.DarkMode
                                        SentinelViewModel.AppThemeMode.LIGHT -> Icons.Filled.LightMode
                                    },
                                    contentDescription = "Switch Global Theme",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(onClick = { viewModel.triggerLocalBackup() }) {
                                Icon(Icons.Filled.CloudSync, contentDescription = "Force Backup Sync", tint = EmeraldNeon)
                            }

                            IconButton(
                                onClick = { viewModel.lockApp() },
                                modifier = Modifier.testTag("lock_app_top_btn")
                            ) {
                                Icon(Icons.Filled.LockPerson, contentDescription = "Lock SentinelX App", tint = AlertOrange)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == 0,
                            onClick = { currentScreen = 0 },
                            icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Status", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("bottom_tab_status")
                        )
                        NavigationBarItem(
                            selected = currentScreen == 1,
                            onClick = { currentScreen = 1 },
                            icon = { Icon(Icons.Filled.MyLocation, contentDescription = "Map") },
                            label = { Text("Map", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("bottom_tab_map")
                        )
                        NavigationBarItem(
                            selected = currentScreen == 2,
                            onClick = { currentScreen = 2 },
                            icon = { Icon(Icons.Filled.Security, contentDescription = "Security") },
                            label = { Text("Actions", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("bottom_tab_actions")
                        )
                        NavigationBarItem(
                            selected = currentScreen == 3,
                            onClick = { currentScreen = 3 },
                            icon = { Icon(Icons.Filled.Terminal, contentDescription = "WebSocket") },
                            label = { Text("Socket", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("bottom_tab_socket")
                        )
                        NavigationBarItem(
                            selected = currentScreen == 7,
                            onClick = { currentScreen = 7 },
                            icon = { Icon(painter = painterResource(id = R.drawable.ic_hacker), contentDescription = "Hacking Terminal", modifier = Modifier.size(20.dp)) },
                            label = { Text("Hacking", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("bottom_tab_hacking")
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        0 -> DashboardScreen(viewModel = viewModel, devices = devices)
                        1 -> LocationScreen(viewModel = viewModel, devices = devices)
                        2 -> SecurityActionsScreen(viewModel = viewModel, devices = devices)
                        3 -> WebSocketConsoleScreen(viewModel = viewModel, devices = devices, commands = commands)
                        4 -> BackupScreen(viewModel = viewModel, devices = devices)
                        5 -> AdminSubscriptionScreen(viewModel = viewModel, devices = devices)
                        6 -> DevPortalScreen()
                        7 -> HackingConsoleScreen(viewModel = viewModel, devices = devices)
                    }
                }
            }

            // -------------------------------------------------------------
            // HIGH-PRIORITY LOCK SCREEN OVERLAYS (DEVICE AGENT LOCAL POLICIES)
            // -------------------------------------------------------------

            // NON-BYPASSABLE BACK HANDLER FOR KIOSK LOCK PROTECTION
            BackHandler(enabled = localDevice?.isLocked == true || localDevice?.isLostMode == true) {
                // Intercept back button and gestures while locked
            }

            // 1. LOST DEVICE MODE FULLSCREEN LOCKOUT
            AnimatedVisibility(
                visible = localDevice?.isLostMode == true,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF260808), Color(0xFF0C0202))
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(DangerRed.copy(alpha = 0.15f), CircleShape)
                                .border(2.dp, DangerRed, CircleShape)
                                .padding(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GppMaybe,
                                contentDescription = "Lost Mode Active",
                                tint = DangerRed,
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "LOST DEVICE PROTECTION ACTIVE",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = DangerRed,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, DangerRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = localDevice?.customLostMessage ?: "This terminal is lost.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "If found, please contact immediately:",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = localDevice?.customLostContact ?: "+1-555-0199",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldNeon,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        // Pin unlock bypass section
                        Text(
                            text = "Authentication Bypass For Owner:",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Quick simulation unlock
                                    keyboardInputPin = ""
                                    inputErrorState = false
                                    viewModel.triggerRemoteCommand("sentinel-agent-local", "STOP_LOST_MODE")
                                    viewModel.triggerRemoteCommand("sentinel-agent-local", "UNLOCK_DEVICE")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("lost_bypass_btn")
                            ) {
                                Text("Owner PIN Bypass (2026)", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 2. REMOTE SCREEN PROTECTION OVERLAY
            AnimatedVisibility(
                visible = localDevice?.isLocked == true && localDevice.isLostMode == false,
                enter = fadeIn() + expandIn(),
                exit = fadeOut() + shrinkOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF030A06)),
                    contentAlignment = Alignment.Center
                ) {
                    // Background Matrix Rain Effect for Cyber Lock Overlay
                    MatrixRainEffect(modifier = Modifier.fillMaxSize())

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Shield Locked",
                                tint = EmeraldNeon,
                                modifier = Modifier.size(60.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "SENTINELX ONLINE LOCK TERMINAL",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00FF66),
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Supports Online Socket Toggle Unlocking & 4-Digit Code '2026'",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // ONLINE TOGGLE UNLOCKING BUTTON
                            OutlinedButton(
                                onClick = {
                                    viewModel.toggleDeviceLockOnline("sentinel-agent-local")
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF065F46).copy(alpha = 0.8f),
                                    contentColor = Color(0xFF34D399)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .testTag("online_toggle_unlock_btn")
                            ) {
                                Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ONLINE TOGGLE UNLOCK (SOCKET)", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Divider(color = Color(0xFF00FF66).copy(alpha = 0.2f), modifier = Modifier.width(260.dp))

                            Spacer(modifier = Modifier.height(12.dp))

                            // CODE UNLOCKING KEYPAD SECTION
                            Text(
                                text = if (inputErrorState) "INVALID PIN (TRY 2026)" else "OR ENTER PASSCODE KEY (2026)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (inputErrorState) DangerRed else Color(0xFF00FF66),
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // PIN Display dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                repeat(4) { idx ->
                                    val active = keyboardInputPin.length > idx
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                if (active) Color(0xFF00FF66) else Color.Gray.copy(alpha = 0.3f),
                                                CircleShape
                                            )
                                    )
                                }
                            }

                            // Grid 3x4 layout for passcode keyboard
                            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Clear", "0", "Unlock")
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(260.dp)
                            ) {
                                keys.chunked(3).forEach { rowKeys ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowKeys.forEach { key ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .background(Color(0xFF0A1610), RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        inputErrorState = false
                                                        when (key) {
                                                            "Clear" -> keyboardInputPin = ""
                                                            "Unlock" -> {
                                                                if (keyboardInputPin == "2026") {
                                                                    keyboardInputPin = ""
                                                                    viewModel.triggerRemoteCommand("sentinel-agent-local", "UNLOCK_DEVICE")
                                                                } else {
                                                                    keyboardInputPin = ""
                                                                    inputErrorState = true
                                                                }
                                                            }
                                                            else -> {
                                                                if (keyboardInputPin.length < 4) {
                                                                    keyboardInputPin += key
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .testTag("pin_keyboard_$key"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = key,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace
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
        }
    }
    }
    }
}
