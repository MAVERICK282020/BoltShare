package com.localsync.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localsync.android.data.model.TransferProgress
import com.localsync.android.data.model.TransferStatus
import com.localsync.android.data.model.TransferType
import com.localsync.android.ui.components.BoltBottomBar
import com.localsync.android.ui.components.BoltTab
import com.localsync.android.ui.viewmodel.TransferViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    viewModel: TransferViewModel,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateExplorer: () -> Unit,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTransfers by viewModel.activeTransfers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Transfer Queue",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC),
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    val hasCompleted = activeTransfers.values.any {
                        it.status == TransferStatus.COMPLETED || it.status == TransferStatus.CANCELLED || it.status == TransferStatus.FAILED
                    }
                    if (hasCompleted) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear History", tint = Color(0xFFF87171))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0C0F17))
            )
        },
        bottomBar = {
            BoltBottomBar(
                currentTab = BoltTab.TRANSFERS,
                onSelectTab = { tab ->
                    when (tab) {
                        BoltTab.HOME -> onNavigateHome()
                        BoltTab.FILES -> onNavigateExplorer()
                        BoltTab.TRANSFERS -> {}
                        BoltTab.SETTINGS -> onNavigateSettings()
                    }
                }
            )
        },
        containerColor = Color(0xFF0C0F17)
    ) { padding ->
        if (activeTransfers.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No active transfers",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Transfers from Phone and Laptop will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeTransfers.values.toList()) { transfer ->
                    TransferCard(
                        transfer = transfer,
                        onPause = { viewModel.pause(transfer.jobId) },
                        onResume = { viewModel.resume(transfer.jobId) },
                        onCancel = { viewModel.cancel(transfer.jobId) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransferCard(
    transfer: TransferProgress,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638)),
        border = BorderStroke(1.dp, Color(0xFF354058))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val icon = if (transfer.type == TransferType.DOWNLOAD) Icons.Default.Download else Icons.Default.Upload
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF242C3D), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = transfer.fileName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFFF8FAFC),
                            maxLines = 1
                        )
                        Text(
                            text = if (transfer.type == TransferType.UPLOAD) "📱 Phone ➔ 💻 PC" else "💻 PC ➔ 📱 Phone",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Text(
                    text = transfer.status.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (transfer.status) {
                        TransferStatus.COMPLETED -> Color(0xFF10B981)
                        TransferStatus.TRANSFERRING -> Color(0xFF818CF8)
                        TransferStatus.FAILED -> Color(0xFFEF4444)
                        TransferStatus.PREPARING -> Color(0xFFF59E0B)
                        else -> Color(0xFF94A3B8)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { transfer.percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Color(0xFF818CF8),
                trackColor = Color(0xFF232B40),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${transfer.percent}% · ${transfer.formattedSpeed}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Row {
                    if (transfer.status == TransferStatus.TRANSFERRING) {
                        IconButton(onClick = onPause, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    } else if (transfer.status == TransferStatus.PAUSED) {
                        IconButton(onClick = onResume, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
