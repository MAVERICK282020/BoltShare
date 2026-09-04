package com.localsync.android.ui.screens

import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localsync.android.data.model.FileItem
import com.localsync.android.ui.components.BoltBottomBar
import com.localsync.android.ui.components.BoltTab
import com.localsync.android.ui.viewmodel.ExplorerUiState
import com.localsync.android.ui.viewmodel.ExplorerViewModel
import com.localsync.android.ui.viewmodel.TransferViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    viewModel: ExplorerViewModel,
    transferViewModel: TransferViewModel,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateTransfers: () -> Unit,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showToastMessage by remember { mutableStateOf<String?>(null) }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME) ?: -1
                val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE) ?: -1
                cursor?.moveToFirst()
                val name = if (nameIndex >= 0) cursor?.getString(nameIndex) ?: "file" else "file"
                val size = if (sizeIndex >= 0) cursor?.getLong(sizeIndex) ?: 0L else 0L
                cursor?.close()
                transferViewModel.startUpload(uri, name, size, viewModel.currentPath)
            }
            showToastMessage = "Sent ${uris.size} file(s) to Laptop!"
            Toast.makeText(context, "⚡ Sent ${uris.size} file(s) to Laptop!", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler {
        if (!viewModel.navigateUp()) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "File Browser",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFF8FAFC)
                        )
                        Text(
                            text = "Browse and manage your files",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!viewModel.navigateUp()) onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* toggle search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8))
                    }
                    IconButton(onClick = { viewModel.loadRoots() }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color(0xFF94A3B8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0C0F17))
            )
        },
        bottomBar = {
            BoltBottomBar(
                currentTab = BoltTab.FILES,
                onSelectTab = { tab ->
                    when (tab) {
                        BoltTab.HOME -> onNavigateHome()
                        BoltTab.FILES -> {}
                        BoltTab.TRANSFERS -> onNavigateTransfers()
                        BoltTab.SETTINGS -> onNavigateSettings()
                    }
                }
            )
        },
        containerColor = Color(0xFF0C0F17)
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. My Laptop Storage Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B29)),
                border = BorderStroke(1.dp, Color(0xFF242C44))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF232B42), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💻", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "My Laptop",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFF8FAFC)
                                )
                                Text(
                                    text = "Windows Storage",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Button(
                            onClick = { uploadLauncher.launch(arrayOf("*/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("+ Upload", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (viewModel.currentPath.isNotBlank() && viewModel.currentPath != "Storage Roots") viewModel.currentPath else "C:\\",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "639.0 GB / 914.8 GB (70%)",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { 0.70f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = Color(0xFF818CF8),
                        trackColor = Color(0xFF232B40),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. PC Storage Header & Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PC Storage",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFFF8FAFC)
                )

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFF242C44),
                        focusedContainerColor = Color(0xFF161B29),
                        unfocusedContainerColor = Color(0xFF161B29)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .width(160.dp)
                        .height(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. File List
            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is ExplorerUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color(0xFF818CF8)
                        )
                    }
                    is ExplorerUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("⚠️ ${state.message}", color = Color(0xFFEF4444), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadRoots() }) { Text("Retry") }
                        }
                    }
                    is ExplorerUiState.Content -> {
                        val filtered = state.files.filter {
                            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
                        }

                        if (filtered.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("📂 Empty directory", color = Color(0xFF64748B))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filtered) { file ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (file.isDirectory) {
                                                    viewModel.openDirectory(file)
                                                }
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B29)),
                                        border = BorderStroke(1.dp, Color(0xFF20273D))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (file.isDirectory) "📁" else "📄",
                                                fontSize = 20.sp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = file.name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFFF8FAFC)
                                                )
                                                Text(
                                                    text = if (file.isDirectory) "Folder · 04 Sept 2026" else "${file.size / 1024} KB",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }

                                            // Send to Phone Action
                                            IconButton(
                                                onClick = {
                                                    showToastMessage = "Sent ${file.name} to phone!"
                                                    Toast.makeText(context, "⚡ Sent ${file.name} to phone!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "Send to Phone",
                                                    tint = Color(0xFF818CF8),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Delete Action
                                            IconButton(
                                                onClick = { viewModel.deleteFile(file.path) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = Color(0xFF64748B),
                                                    modifier = Modifier.size(16.dp)
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

            // 4. Bottom Drop Zone Pill
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uploadLauncher.launch(arrayOf("*/*")) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131724)),
                border = BorderStroke(1.dp, Color(0xFF26304A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔗", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Drag Phone files here or drop OS files to Save on PC",
                        fontSize = 11.sp,
                        color = Color(0xFF818CF8),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 5. Toast Pill if any
            if (showToastMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF064E3B), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✓", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(showToastMessage ?: "", color = Color(0xFFECFDF5), fontSize = 11.sp)
                    }
                    Text(
                        text = "×",
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { showToastMessage = null }
                    )
                }
            }
        }
    }
}
