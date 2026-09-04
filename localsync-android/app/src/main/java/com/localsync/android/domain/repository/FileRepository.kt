package com.localsync.android.domain.repository

import com.localsync.android.data.model.FileItem
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    suspend fun getStorageRoots(): Result<List<FileItem>>
    suspend fun listFiles(path: String): Result<List<FileItem>>
    suspend fun createFolder(parentPath: String, folderName: String): Result<Unit>
    suspend fun deleteFile(path: String): Result<Unit>
    suspend fun getFileMetadata(path: String): Result<FileItem>
    fun getStreamUrl(filePath: String): String
}
