package com.localsync.android.domain.usecase

import android.net.Uri
import com.localsync.android.data.model.TransferProgress
import com.localsync.android.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import java.io.File

class ExecuteTransferUseCase(
    private val transferRepository: TransferRepository
) {
    fun upload(fileUri: Uri, name: String, size: Long, targetDir: String): Flow<TransferProgress> =
        transferRepository.uploadFile(fileUri, name, size, targetDir)

    fun download(remotePath: String, name: String, destFile: File): Flow<TransferProgress> =
        transferRepository.downloadFile(remotePath, name, destFile)

    suspend fun pause(jobId: String) = transferRepository.pauseTransfer(jobId)

    suspend fun resume(jobId: String) = transferRepository.resumeTransfer(jobId)

    suspend fun cancel(jobId: String) = transferRepository.cancelTransfer(jobId)

    fun verifyIntegrity(file: File, expectedHash: String): Boolean =
        transferRepository.verifyHash(file, expectedHash)
}
