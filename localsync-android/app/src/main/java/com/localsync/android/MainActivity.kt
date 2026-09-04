package com.localsync.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.localsync.android.ui.screens.DashboardScreen
import com.localsync.android.ui.screens.ExplorerScreen
import com.localsync.android.ui.screens.PairingScreen
import com.localsync.android.ui.screens.TransferScreen
import com.localsync.android.ui.screens.SettingsScreen
import com.localsync.android.ui.theme.LocalSyncTheme
import com.localsync.android.ui.viewmodel.ExplorerViewModel
import com.localsync.android.ui.viewmodel.PairingViewModel
import com.localsync.android.ui.viewmodel.TransferViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as LocalSyncApplication

        // ViewModels
        val pairingViewModel = PairingViewModel(
            pairDeviceUseCase = app.pairDeviceUseCase,
            autoReconnectUseCase = app.autoReconnectUseCase,
            unpairDeviceUseCase = app.unpairDeviceUseCase,
            credentialsRepo = app.credentialsRepo
        )

        val explorerViewModel = ExplorerViewModel(
            browseStorageUseCase = app.browseStorageUseCase,
            manageFileUseCase = app.manageFileUseCase,
            networkRouter = app.networkRouter
        )

        val transferViewModel = TransferViewModel(
            executeTransferUseCase = app.executeTransferUseCase
        )

        setContent {
            LocalSyncTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val isPaired by app.credentialsRepo.isPairedFlow.collectAsState(initial = app.credentialsRepo.isPaired())

                    val startDestination = if (isPaired) "dashboard" else "pairing"

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable("pairing") {
                            PairingScreen(
                                viewModel = pairingViewModel,
                                onPairedSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("pairing") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("dashboard") {
                            DashboardScreen(
                                pairingViewModel = pairingViewModel,
                                explorerViewModel = explorerViewModel,
                                transferViewModel = transferViewModel,
                                onNavigateToExplorer = { navController.navigate("explorer") },
                                onNavigateToTransfers = { navController.navigate("transfers") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onUnpair = {
                                    pairingViewModel.unpair()
                                    navController.navigate("pairing") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("explorer") {
                            ExplorerScreen(
                                viewModel = explorerViewModel,
                                transferViewModel = transferViewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateHome = { navController.navigate("dashboard") },
                                onNavigateTransfers = { navController.navigate("transfers") },
                                onNavigateSettings = { navController.navigate("settings") }
                            )
                        }

                        composable("transfers") {
                            TransferScreen(
                                viewModel = transferViewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateHome = { navController.navigate("dashboard") },
                                onNavigateExplorer = { navController.navigate("explorer") },
                                onNavigateSettings = { navController.navigate("settings") }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onNavigateHome = { navController.navigate("dashboard") },
                                onNavigateExplorer = { navController.navigate("explorer") },
                                onNavigateTransfers = { navController.navigate("transfers") },
                                onUnpair = {
                                    pairingViewModel.unpair()
                                    navController.navigate("pairing") {
                                        popUpTo("settings") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
