package com.localsync.android

import android.app.Application
import com.localsync.android.data.api.NetworkRouter
import com.localsync.android.data.repository.FileRepositoryImpl
import com.localsync.android.data.repository.PairingRepositoryImpl
import com.localsync.android.data.repository.TransferRepositoryImpl
import com.localsync.android.domain.repository.FileRepository
import com.localsync.android.domain.repository.PairingRepository
import com.localsync.android.domain.repository.TransferRepository
import com.localsync.android.domain.usecase.*
import com.localsync.android.security.SecureCredentialRepository
import com.localsync.android.security.SecureCredentialRepositoryImpl

class LocalSyncApplication : Application() {

    lateinit var credentialsRepo: SecureCredentialRepository
        private set

    lateinit var networkRouter: NetworkRouter
        private set

    lateinit var pairingRepository: PairingRepository
        private set

    lateinit var fileRepository: FileRepository
        private set

    lateinit var transferRepository: TransferRepository
        private set

    // UseCases
    lateinit var pairDeviceUseCase: PairDeviceUseCase
        private set
    lateinit var autoReconnectUseCase: AutoReconnectUseCase
        private set
    lateinit var unpairDeviceUseCase: UnpairDeviceUseCase
        private set
    lateinit var browseStorageUseCase: BrowseStorageUseCase
        private set
    lateinit var manageFileUseCase: ManageFileUseCase
        private set
    lateinit var executeTransferUseCase: ExecuteTransferUseCase
        private set

    // Reverse Phone Storage Server (Port 8085)
    lateinit var phoneStorageServer: com.localsync.android.server.PhoneStorageServer
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Security & Credentials
        credentialsRepo = SecureCredentialRepositoryImpl(this)

        // 2. Network Router
        networkRouter = NetworkRouter(credentialsRepo)

        // 3. Repositories
        pairingRepository = PairingRepositoryImpl(networkRouter, credentialsRepo)
        fileRepository = FileRepositoryImpl(networkRouter, credentialsRepo)
        transferRepository = TransferRepositoryImpl(this, networkRouter)

        // 4. UseCases
        pairDeviceUseCase = PairDeviceUseCase(pairingRepository)
        autoReconnectUseCase = AutoReconnectUseCase(pairingRepository)
        unpairDeviceUseCase = UnpairDeviceUseCase(pairingRepository)
        browseStorageUseCase = BrowseStorageUseCase(fileRepository)
        manageFileUseCase = ManageFileUseCase(fileRepository)
        executeTransferUseCase = ExecuteTransferUseCase(transferRepository)

        // 5. Start PhoneStorageServer for direct PC reverse-browsing
        phoneStorageServer = com.localsync.android.server.PhoneStorageServer(this, 8085)
        phoneStorageServer.start()

        // 6. Acquire high-performance Wi-Fi lock safely for responsive LAN transfers
        try {
            val wifiManager = applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            wifiManager?.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF, "BoltShare:WifiLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            android.util.Log.w("LocalSyncApplication", "WifiLock not acquired: ${e.message}")
        }
    }
}
