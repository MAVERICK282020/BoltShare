package com.localsync.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localsync.android.ui.components.QRScanner
import com.localsync.android.ui.viewmodel.PairingUiState
import com.localsync.android.ui.viewmodel.PairingViewModel

import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.localsync.android.R

@Composable
fun PairingScreen(
    viewModel: PairingViewModel,
    onPairedSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is PairingUiState.Success) {
            onPairedSuccess()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0C0F17)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.bolt_logo),
                contentDescription = "BoltShare Logo",
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(18.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "BoltShare",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp
                ),
                color = Color(0xFFF8FAFC)
            )

            Text(
                text = "Point camera at the QR code displayed on your laptop's BoltShare dashboard to pair permanently.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Decoupled QR Scanner Component
            QRScanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                onQrScanned = { qrData ->
                    viewModel.onQrScanned(qrData)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState) {
                is PairingUiState.Loading -> {
                    CircularProgressIndicator(
                        color = Color(0xFF6366F1),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Establishing secure cryptographic pairing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4B5563),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                is PairingUiState.Error -> {
                    Text(
                        text = (uiState as PairingUiState.Error).message,
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
                is PairingUiState.Success -> {
                    Text(
                        text = "✅ Paired with ${(uiState as PairingUiState.Success).deviceName}",
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                PairingUiState.Idle -> {
                    Text(
                        text = "🔒 Pairing is permanent (1-Year Device Token)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }
}
