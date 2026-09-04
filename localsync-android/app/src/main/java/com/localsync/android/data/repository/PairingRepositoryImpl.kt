package com.localsync.android.data.repository

import com.localsync.android.data.api.NetworkRouter
import com.localsync.android.data.api.RouteMode
import com.localsync.android.data.model.PairRequest
import com.localsync.android.data.model.QrPayload
import com.localsync.android.data.model.ReconnectRequest
import com.localsync.android.domain.repository.PairingRepository
import com.localsync.android.security.SecureCredentialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PairingRepositoryImpl(
    private val router: NetworkRouter,
    private val credentialsRepo: SecureCredentialRepository
) : PairingRepository {

    override suspend fun pairWithQr(qrPayload: QrPayload, deviceName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Update router target URL to scanned host
            val targetUrl = qrPayload.localUrl ?: "http://${qrPayload.host}:${qrPayload.port}"
            router.updateBaseUrl(targetUrl)

            val request = PairRequest(
                token = qrPayload.token,
                sig = qrPayload.sig,
                exp = qrPayload.exp,
                deviceName = deviceName,
                deviceType = "android"
            )

            val response = router.api.pairDevice(request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                credentialsRepo.saveCredentials(
                    deviceId = body.deviceId,
                    deviceToken = body.deviceToken ?: "",
                    accessToken = body.token,
                    localUrl = body.localUrl ?: targetUrl,
                    remoteUrl = body.remoteUrl ?: qrPayload.remoteUrl
                )
                router.currentMode = RouteMode.LAN_DIRECT
                Result.success(body.deviceName ?: deviceName)
            } else {
                Result.failure(Exception("Pairing failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun autoReconnect(): Result<String> = withContext(Dispatchers.IO) {
        val deviceToken = credentialsRepo.getDeviceToken()
            ?: return@withContext Result.failure(Exception("No paired device token found"))

        try {
            // Resolve best route (LAN or Remote)
            router.resolveBestRoute()

            val response = router.api.reconnect(ReconnectRequest(deviceToken))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                credentialsRepo.updateAccessToken(body.token)
                Result.success(body.deviceName ?: "Laptop")
            } else {
                Result.failure(Exception("Auto-reconnect failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun unpair(): Result<Unit> {
        credentialsRepo.clearCredentials()
        return Result.success(Unit)
    }

    override fun isPaired(): Boolean = credentialsRepo.isPaired()
}
