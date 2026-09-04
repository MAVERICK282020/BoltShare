package com.localsync.android.data.repository

import android.content.Context
import android.net.Uri
import com.localsync.android.data.api.NetworkRouter
import com.localsync.android.data.model.TransferProgress
import com.localsync.android.data.model.TransferStatus
import com.localsync.android.data.model.TransferType
import com.localsync.android.domain.repository.TransferRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class TransferRepositoryImpl(
    private val context: Context,
    private val router: NetworkRouter
) : TransferRepository {

    private val pausedJobs = ConcurrentHashMap<String, Boolean>()
    private val cancelledJobs = ConcurrentHashMap<String, Boolean>()

    override fun uploadFile(
        fileUri: Uri,
        fileName: String,
        fileSize: Long,
        targetDir: String
    ): Flow<TransferProgress> = flow {
        var jobId = ""
        try {
            emit(
                TransferProgress(
                    jobId = "",
                    fileName = fileName,
                    bytesTransferred = 0,
                    totalBytes = fileSize,
                    currentChunk = 0,
                    totalChunks = 1,
                    speedBps = 0,
                    status = TransferStatus.PREPARING,
                    type = TransferType.UPLOAD
                )
            )

            // 1. Initialize transfer with backend
            val initResp = router.api.initTransfer(fileName, fileSize, targetDir)
            if (!initResp.isSuccessful || initResp.body() == null) {
                emit(createErrorProgress(jobId, fileName, fileSize, "Init failed: ${initResp.code()}", TransferType.UPLOAD))
                return@flow
            }
            jobId = initResp.body()!!.jobId
            val totalChunks = initResp.body()!!.totalChunks

            // 2. Upload chunks
            val chunkSize = 10 * 1024 * 1024 // 10 MB chunks
            val buffer = ByteArray(chunkSize)
            var bytesUploaded = 0L
            var startTime = System.currentTimeMillis()

            val contentResolver = context.contentResolver
            contentResolver.openInputStream(fileUri)?.use { inputStream ->
                var chunkIndex = 0
                while (chunkIndex < totalChunks) {
                    if (cancelledJobs.containsKey(jobId)) {
                        emit(createCancelledProgress(jobId, fileName, bytesUploaded, fileSize, TransferType.UPLOAD))
                        return@flow
                    }

                    while (pausedJobs.containsKey(jobId)) {
                        emit(
                            TransferProgress(
                                jobId = jobId,
                                fileName = fileName,
                                bytesTransferred = bytesUploaded,
                                totalBytes = fileSize,
                                currentChunk = chunkIndex,
                                totalChunks = totalChunks,
                                speedBps = 0,
                                status = TransferStatus.PAUSED,
                                type = TransferType.UPLOAD
                            )
                        )
                        delay(1000)
                    }

                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead <= 0) break

                    val chunkBytes = if (bytesRead == chunkSize) buffer else buffer.copyOf(bytesRead)
                    val reqBody = chunkBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("chunk", "$fileName.part$chunkIndex", reqBody)

                    val chunkResp = router.api.uploadChunk(jobId, chunkIndex, part)
                    if (!chunkResp.isSuccessful) {
                        emit(createErrorProgress(jobId, fileName, fileSize, "Chunk $chunkIndex failed", TransferType.UPLOAD))
                        return@flow
                    }

                    bytesUploaded += bytesRead
                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                    val speed = if (elapsedSec > 0) (bytesUploaded / elapsedSec).toLong() else 0L

                    emit(
                        TransferProgress(
                            jobId = jobId,
                            fileName = fileName,
                            bytesTransferred = bytesUploaded,
                            totalBytes = fileSize,
                            currentChunk = chunkIndex + 1,
                            totalChunks = totalChunks,
                            speedBps = speed,
                            status = TransferStatus.TRANSFERRING,
                            type = TransferType.UPLOAD
                        )
                    )
                    chunkIndex++
                }
            }

            // 3. Complete transfer
            val completeResp = router.api.completeTransfer(jobId)
            if (completeResp.isSuccessful) {
                emit(
                    TransferProgress(
                        jobId = jobId,
                        fileName = fileName,
                        bytesTransferred = fileSize,
                        totalBytes = fileSize,
                        currentChunk = totalChunks,
                        totalChunks = totalChunks,
                        speedBps = 0,
                        status = TransferStatus.COMPLETED,
                        type = TransferType.UPLOAD
                    )
                )
            } else {
                emit(createErrorProgress(jobId, fileName, fileSize, "Completion failed", TransferType.UPLOAD))
            }
        } catch (e: Exception) {
            emit(createErrorProgress(jobId, fileName, fileSize, e.message ?: "Upload failed", TransferType.UPLOAD))
        }
    }.flowOn(Dispatchers.IO)

    override fun downloadFile(
        remotePath: String,
        fileName: String,
        destinationFile: File
    ): Flow<TransferProgress> = flow {
        val jobId = "dl-${System.currentTimeMillis()}"
        try {
            emit(
                TransferProgress(
                    jobId = jobId,
                    fileName = fileName,
                    bytesTransferred = 0,
                    totalBytes = -1,
                    currentChunk = 0,
                    totalChunks = 1,
                    speedBps = 0,
                    status = TransferStatus.PREPARING,
                    type = TransferType.DOWNLOAD
                )
            )

            val response = router.api.downloadFile(remotePath)
            if (!response.isSuccessful || response.body() == null) {
                emit(createErrorProgress(jobId, fileName, 0, "Download failed: ${response.code()}", TransferType.DOWNLOAD))
                return@flow
            }

            val body = response.body()!!
            val totalBytes = body.contentLength()
            var bytesRead = 0L
            val startTime = System.currentTimeMillis()

            body.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (cancelledJobs.containsKey(jobId)) {
                            destinationFile.delete()
                            emit(createCancelledProgress(jobId, fileName, bytesRead, totalBytes, TransferType.DOWNLOAD))
                            return@flow
                        }

                        output.write(buffer, 0, read)
                        bytesRead += read

                        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                        val speed = if (elapsedSec > 0) (bytesRead / elapsedSec).toLong() else 0L

                        emit(
                            TransferProgress(
                                jobId = jobId,
                                fileName = fileName,
                                bytesTransferred = bytesRead,
                                totalBytes = totalBytes,
                                currentChunk = 1,
                                totalChunks = 1,
                                speedBps = speed,
                                status = TransferStatus.TRANSFERRING,
                                type = TransferType.DOWNLOAD
                            )
                        )
                    }
                }
            }

            emit(
                TransferProgress(
                    jobId = jobId,
                    fileName = fileName,
                    bytesTransferred = bytesRead,
                    totalBytes = totalBytes,
                    currentChunk = 1,
                    totalChunks = 1,
                    speedBps = 0,
                    status = TransferStatus.COMPLETED,
                    type = TransferType.DOWNLOAD
                )
            )
        } catch (e: Exception) {
            emit(createErrorProgress(jobId, fileName, 0, e.message ?: "Download error", TransferType.DOWNLOAD))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun pauseTransfer(jobId: String) {
        pausedJobs[jobId] = true
    }

    override suspend fun resumeTransfer(jobId: String) {
        pausedJobs.remove(jobId)
    }

    override suspend fun cancelTransfer(jobId: String): Result<Unit> = withContext(Dispatchers.IO) {
        cancelledJobs[jobId] = true
        try {
            router.api.cancelTransfer(jobId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    override fun verifyHash(file: File, expectedSha256: String): Boolean {
        if (!file.exists()) return false
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val hashBytes = digest.digest()
            val computedHash = hashBytes.joinToString("") { "%02x".format(it) }
            computedHash.equals(expectedSha256, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    private fun createErrorProgress(jobId: String, fileName: String, totalBytes: Long, error: String, type: TransferType) =
        TransferProgress(
            jobId = jobId,
            fileName = fileName,
            bytesTransferred = 0,
            totalBytes = totalBytes,
            currentChunk = 0,
            totalChunks = 1,
            speedBps = 0,
            status = TransferStatus.FAILED,
            type = type,
            error = error
        )

    private fun createCancelledProgress(jobId: String, fileName: String, transferred: Long, totalBytes: Long, type: TransferType) =
        TransferProgress(
            jobId = jobId,
            fileName = fileName,
            bytesTransferred = transferred,
            totalBytes = totalBytes,
            currentChunk = 0,
            totalChunks = 1,
            speedBps = 0,
            status = TransferStatus.CANCELLED,
            type = type
        )
}
