package com.localsync.android.domain.repository

import android.net.Uri
import com.localsync.android.data.model.TransferProgress
import kotlinx.coroutines.flow.Flow
import java.io.File

interface TransferRepository {
    fun uploadFile(
        fileUri: Uri,
        fileName: String,
        fileSize: Long,
        targetDir: String
    ): Flow<TransferProgress>

    fun downloadFile(
        remotePath: String,
        fileName: String,
        destinationFile: File
    ): Flow<TransferProgress>

    suspend fun pauseTransfer(jobId: String)
    suspend fun resumeTransfer(jobId: String)
    suspend fun cancelTransfer(jobId: String): Result<Unit>
    fun verifyHash(file: File, expectedSha256: String): Boolean
}
