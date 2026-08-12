package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.EmeraldNeon

@Composable
fun DevPortalScreen(modifier: Modifier = Modifier) {
    var activeTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("DB Schema", "REST API", "Docker Setup", "CI/CD & Tests")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SentinelX Developer Specifications Console",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        ScrollableTabRow(
            selectedTabIndex = activeTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("dev_tab_$index")
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (activeTab) {
                    0 -> DatabaseSchemaView()
                    1 -> ApiDocumentationView()
                    2 -> DockerDeploymentView()
                    3 -> CicdTestingView()
                }
            }
        }
    }
}

@Composable
fun DatabaseSchemaView() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                text = "PostgreSQL DB & SQLite Room Schemas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Dual relational state entities synchronized between edge devices and the central DB.",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        item {
            CodeSnippetPanel(
                title = "Table 1: Devices Schema (PostgreSQL / Room)",
                code = """CREATE TABLE "devices" (
  "id" VARCHAR(64) PRIMARY KEY,
  "name" VARCHAR(128) NOT NULL,
  "manufacturer" VARCHAR(128),
  "model" VARCHAR(128),
  "android_version" VARCHAR(32),
  "security_patch" VARCHAR(32),
  "battery_pct" INT DEFAULT 100,
  "is_charging" BOOLEAN DEFAULT FALSE,
  "network_status" VARCHAR(64),
  "storage_total" DOUBLE PRECISION,
  "storage_used" DOUBLE PRECISION,
  "ram_total" DOUBLE PRECISION,
  "ram_used" DOUBLE PRECISION,
  "is_online" BOOLEAN DEFAULT TRUE,
  "last_active" BIGINT NOT NULL,
  "health_score" INT DEFAULT 100,
  "lat" DOUBLE PRECISION,
  "lng" DOUBLE PRECISION,
  "is_lost_mode" BOOLEAN DEFAULT FALSE,
  "custom_message" TEXT,
  "is_locked" BOOLEAN DEFAULT FALSE
);"""
            )
        }

        item {
            CodeSnippetPanel(
                title = "Table 2: Audit Logs (Immutable Event Ledger)",
                code = """CREATE TABLE "audit_logs" (
  "id" SERIAL PRIMARY KEY,
  "timestamp" BIGINT NOT NULL,
  "message" TEXT NOT NULL,
  "level" VARCHAR(16) NOT NULL, -- INFO, WARNING, CRITICAL
  "device_id" VARCHAR(64) REFERENCES devices(id) ON DELETE CASCADE
);"""
            )
        }

        item {
            CodeSnippetPanel(
                title = "Table 3: Command Queue (State Machine logs)",
                code = """CREATE TABLE "commands" (
  "command_id" VARCHAR(64) PRIMARY KEY,
  "type" VARCHAR(64) NOT NULL, -- LOCK_DEVICE, WIPE_DEVICE...
  "target_device_id" VARCHAR(64) NOT NULL,
  "timestamp" BIGINT NOT NULL,
  "payload_json" JSONB,
  "status" VARCHAR(32) DEFAULT 'Pending',
  "signature" VARCHAR(256) NOT NULL
);"""
            )
        }
    }
}

@Composable
fun ApiDocumentationView() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                text = "Secure REST & WebSocket APIs Endpoints",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "NestJS Controllers & Gateway payload formats.",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        item {
            ApiEndpointCard(
                method = "POST",
                path = "/api/v1/auth/enroll",
                desc = "Enroll device agent. Requires cryptographic pair hash. Returns secure OAuth2 Bearer JSON JWT.",
                payload = """{
  "device_id": "sentinel-local",
  "pairing_otp": "XF92AL",
  "public_key": "MIIBIjANBgkqhkiG9w0BAQEFAAOC..."
}"""
            )
        }

        item {
            ApiEndpointCard(
                method = "GET",
                path = "/api/v1/devices",
                desc = "Retrieves all personal registered devices, status metrics, and security health thresholds.",
                payload = "Response: [DeviceEntityJSON, ...]"
            )
        }

        item {
            ApiEndpointCard(
                method = "POST",
                path = "/api/v1/commands/transmit",
                desc = "Sends signed command payload (TLS). Authenticates owner PIN signature before WebSocket push.",
                payload = """{
  "command_id": "84a2f8b5-3d92-4fbc-b40f",
  "type": "LOCK_DEVICE",
  "target_device_id": "local-agent",
  "signature": "c869ea466bcfa0bb43c7df480746b..."
}"""
            )
        }
    }
}

@Composable
fun DockerDeploymentView() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                text = "Docker Multi-Container Orchestration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Deployment compose stack mapping Node.js NestJS, PostgreSQL Cluster, and Redis caching blocks.",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        item {
            CodeSnippetPanel(
                title = "docker-compose.yml",
                code = """version: '3.8'

services:
  sentinel-backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - DB_HOST=postgres-db
      - DB_PORT=5432
      - DB_USER=sentinel_root
      - DB_PASSWORD=vault_secure_pass
      - DB_NAME=sentinel_store
      - REDIS_HOST=redis-cache
    depends_on:
      - postgres-db
      - redis-cache

  postgres-db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_USER=sentinel_root
      - POSTGRES_PASSWORD=vault_secure_pass
      - POSTGRES_DB=sentinel_store
    volumes:
      - pgdata:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis-cache:
    image: redis:7-alpine
    command: redis-server --requirepass secure_cache_pass
    ports:
      - "6379:6379"

volumes:
  pgdata:"""
            )
        }
    }
}

@Composable
fun CicdTestingView() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                text = "Continuous Integration & Automated Testing Suite",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "GitHub Actions automation workflow and local JVM execution instructions.",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        item {
            CodeSnippetPanel(
                title = "CI/CD Pipeline: .github/workflows/ci.yml",
                code = """name: SentinelX JVM & Build CI

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Repository Codebase
        uses: actions/checkout@v3

      - name: Setup JDK Core 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant Gradle execute rights
        run: chmod +x gradlew

      - name: Execute Robolectric Unit Tests
        run: gradle :app:testDebugUnitTest

      - name: Compile and Assemble Debug APK
        run: gradle :app:assembleDebug"""
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Local Developer CLI Testing Commands:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "# Run unit and Robolectric tests\ngradle :app:testDebugUnitTest\n\n# Record visual Roborazzi screenshot benchmarks\ngradle :app:recordRoborazziDebug\n\n# Verify screenshot regression deviations\ngradle :app:verifyRoborazziDebug",
                        fontSize = 10.sp,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun CodeSnippetPanel(title: String, code: String) {
    Column {
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF030814)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
        ) {
            Text(
                text = code,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.LightGray.copy(alpha = 0.85f),
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
fun ApiEndpointCard(method: String, path: String, desc: String, payload: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(
                            when (method) {
                                "POST" -> AlertOrange.copy(alpha = 0.2f)
                                "GET" -> EmeraldNeon.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = method,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = when (method) {
                            "POST" -> AlertOrange
                            "GET" -> EmeraldNeon
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = path,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = desc, fontSize = 11.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "PAYLOAD PAYLOAD:",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = payload,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.LightGray.copy(alpha = 0.7f),
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}
