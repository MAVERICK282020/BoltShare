package com.localsync.android.data.model

import android.net.Uri
import com.squareup.moshi.JsonClass

data class QrPayload(
    val token: String,
    val host: String,
    val port: Int,
    val sig: String,
    val exp: Long,
    val localUrl: String?,
    val remoteUrl: String?
) {
    companion object {
        /**
         * Parses localsync://pair?token=...&host=...&sig=...&exp=...
         */
        fun parse(qrContent: String): QrPayload? {
            return try {
                val uri = Uri.parse(qrContent)
                if (uri.scheme != "localsync" || uri.host != "pair") return null

                val token = uri.getQueryParameter("token") ?: return null
                val host = uri.getQueryParameter("host") ?: return null
                val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 8080
                val sig = uri.getQueryParameter("sig") ?: return null
                val exp = uri.getQueryParameter("exp")?.toLongOrNull() ?: 0L
                val localUrl = uri.getQueryParameter("localUrl") ?: "http://$host:$port"
                val remoteUrl = uri.getQueryParameter("remoteUrl")

                QrPayload(token, host, port, sig, exp, localUrl, remoteUrl)
            } catch (e: Exception) {
                null
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class PairRequest(
    val token: String,
    val sig: String,
    val exp: Long,
    val deviceName: String,
    val deviceType: String
)

@JsonClass(generateAdapter = true)
data class ReconnectRequest(
    val deviceToken: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val token: String,
    val deviceId: String,
    val deviceName: String?,
    val deviceToken: String?,
    val localUrl: String?,
    val remoteUrl: String?
)

@JsonClass(generateAdapter = true)
data class NetworkStatusResponse(
    val localIp: String,
    val localUrl: String,
    val remoteUrl: String?,
    val remoteEnabled: Boolean,
    val mode: String
)
