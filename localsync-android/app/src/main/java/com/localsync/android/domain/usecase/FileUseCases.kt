package com.localsync.android.domain.usecase

import com.localsync.android.data.model.FileItem
import com.localsync.android.domain.repository.FileRepository

class BrowseStorageUseCase(
    private val fileRepository: FileRepository
) {
    suspend fun getRoots(): Result<List<FileItem>> = fileRepository.getStorageRoots()

    suspend fun listDirectory(path: String): Result<List<FileItem>> = fileRepository.listFiles(path)

    fun getStreamingUrl(filePath: String): String = fileRepository.getStreamUrl(filePath)
}

class ManageFileUseCase(
    private val fileRepository: FileRepository
) {
    suspend fun createFolder(parentPath: String, name: String): Result<Unit> =
        fileRepository.createFolder(parentPath, name)

    suspend fun delete(path: String): Result<Unit> =
        fileRepository.deleteFile(path)
}
