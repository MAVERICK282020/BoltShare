package com.localsync.android.domain.repository

import com.localsync.android.data.model.QrPayload

interface PairingRepository {
    suspend fun pairWithQr(qrPayload: QrPayload, deviceName: String): Result<String>
    suspend fun autoReconnect(): Result<String>
    fun unpair(): Result<Unit>
    fun isPaired(): Boolean
}
