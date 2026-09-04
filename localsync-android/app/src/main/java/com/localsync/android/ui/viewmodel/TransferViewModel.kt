package com.localsync.android.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localsync.android.data.model.TransferProgress
import com.localsync.android.data.model.TransferStatus
import com.localsync.android.data.model.TransferType
import com.localsync.android.domain.usecase.ExecuteTransferUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class QueuedUpload(
    val uri: Uri,
    val fileName: String,
    val fileSize: Long,
    val targetDir: String,
    val tempId: String = java.util.UUID.randomUUID().toString()
)

class TransferViewModel(
    private val executeTransferUseCase: ExecuteTransferUseCase
) : ViewModel() {

    private val _activeTransfers = MutableStateFlow<Map<String, TransferProgress>>(emptyMap())
    val activeTransfers: StateFlow<Map<String, TransferProgress>> = _activeTransfers.asStateFlow()

    private val uploadChannel = kotlinx.coroutines.channels.Channel<QueuedUpload>(kotlinx.coroutines.channels.Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            for (queued in uploadChannel) {
                try {
                    executeTransferUseCase.upload(queued.uri, queued.fileName, queued.fileSize, queued.targetDir)
                        .collect { progress ->
                            val updated = _activeTransfers.value.toMutableMap()
                            val key = if (progress.jobId.isNotBlank()) progress.jobId else queued.tempId
                            if (progress.jobId.isNotBlank() && updated.containsKey(queued.tempId)) {
                                updated.remove(queued.tempId)
                            }
                            updated[key] = progress.copy(jobId = key)
                            _activeTransfers.value = updated
                        }
                } catch (e: Exception) {
                    val updated = _activeTransfers.value.toMutableMap()
                    updated[queued.tempId] = TransferProgress(
                        jobId = queued.tempId,
                        fileName = queued.fileName,
                        bytesTransferred = 0,
                        totalBytes = queued.fileSize,
                        currentChunk = 0,
                        totalChunks = 1,
                        speedBps = 0,
                        status = TransferStatus.FAILED,
                        type = TransferType.UPLOAD,
                        error = e.message ?: "Upload failed"
                    )
                    _activeTransfers.value = updated
                }
            }
        }
    }

    fun startDownload(remotePath: String, fileName: String, destinationFile: File) {
        viewModelScope.launch {
            executeTransferUseCase.download(remotePath, fileName, destinationFile)
                .collect { progress ->
                    val updated = _activeTransfers.value.toMutableMap()
                    updated[progress.jobId] = progress
                    _activeTransfers.value = updated
                }
        }
    }

    fun startUpload(fileUri: Uri, fileName: String, fileSize: Long, targetDir: String) {
        val item = QueuedUpload(fileUri, fileName, fileSize, targetDir)
        val updated = _activeTransfers.value.toMutableMap()
        updated[item.tempId] = TransferProgress(
            jobId = item.tempId,
            fileName = fileName,
            bytesTransferred = 0,
            totalBytes = fileSize,
            currentChunk = 0,
            totalChunks = 1,
            speedBps = 0,
            status = TransferStatus.PREPARING,
            type = TransferType.UPLOAD
        )
        _activeTransfers.value = updated
        uploadChannel.trySend(item)
    }

    fun pause(jobId: String) {
        viewModelScope.launch {
            executeTransferUseCase.pause(jobId)
        }
    }

    fun resume(jobId: String) {
        viewModelScope.launch {
            executeTransferUseCase.resume(jobId)
        }
    }

    fun cancel(jobId: String) {
        viewModelScope.launch {
            executeTransferUseCase.cancel(jobId)
            val updated = _activeTransfers.value.toMutableMap()
            updated.remove(jobId)
            _activeTransfers.value = updated
        }
    }

    fun clearHistory() {
        val updated = _activeTransfers.value.filterValues {
            it.status != TransferStatus.COMPLETED &&
            it.status != TransferStatus.CANCELLED &&
            it.status != TransferStatus.FAILED
        }
        _activeTransfers.value = updated
    }
}
