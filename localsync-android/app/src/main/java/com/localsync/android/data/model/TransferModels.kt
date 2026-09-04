package com.localsync.android.data.model

import com.squareup.moshi.JsonClass

enum class TransferStatus {
    IDLE,
    PREPARING,
    TRANSFERRING,
    PAUSED,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class TransferType {
    DOWNLOAD,
    UPLOAD
}

data class TransferProgress(
    val jobId: String,
    val fileName: String,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val currentChunk: Int,
    val totalChunks: Int,
    val speedBps: Long,
    val status: TransferStatus,
    val type: TransferType,
    val error: String? = null
) {
    val percent: Int
        get() = if (totalBytes > 0) ((bytesTransferred.toDouble() / totalBytes) * 100).toInt() else 0

    val formattedSpeed: String
        get() {
            if (speedBps <= 0) return "—"
            val mbps = speedBps / (1024.0 * 1024.0)
            return "%.1f MB/s".format(mbps)
        }
}

@JsonClass(generateAdapter = true)
data class InitTransferResponse(
    val jobId: String,
    val chunkSize: Int,
    val totalChunks: Int
)

@JsonClass(generateAdapter = true)
data class TransferStatusDto(
    val jobId: String,
    val fileName: String,
    val lastChunkReceived: Int,
    val totalChunks: Int,
    val totalSize: Long,
    val status: String
)
