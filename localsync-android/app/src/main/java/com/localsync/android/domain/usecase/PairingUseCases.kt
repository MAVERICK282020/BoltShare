package com.localsync.android.domain.usecase

import com.localsync.android.data.model.QrPayload
import com.localsync.android.domain.repository.PairingRepository

class PairDeviceUseCase(
    private val pairingRepository: PairingRepository
) {
    suspend operator fun invoke(qrContent: String, deviceName: String): Result<String> {
        val payload = QrPayload.parse(qrContent)
            ?: return Result.failure(IllegalArgumentException("Invalid LocalSync QR code format"))
        return pairingRepository.pairWithQr(payload, deviceName)
    }
}

class AutoReconnectUseCase(
    private val pairingRepository: PairingRepository
) {
    suspend operator fun invoke(): Result<String> {
        return pairingRepository.autoReconnect()
    }
}

class UnpairDeviceUseCase(
    private val pairingRepository: PairingRepository
) {
    operator fun invoke(): Result<Unit> {
        return pairingRepository.unpair()
    }
}
