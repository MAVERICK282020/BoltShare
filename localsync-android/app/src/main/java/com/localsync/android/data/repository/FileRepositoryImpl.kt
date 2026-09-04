package com.localsync.android.data.repository

import com.localsync.android.data.api.NetworkRouter
import com.localsync.android.data.model.FileItem
import com.localsync.android.domain.repository.FileRepository
import com.localsync.android.security.SecureCredentialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class FileRepositoryImpl(
    private val router: NetworkRouter,
    private val credentialsRepo: SecureCredentialRepository
) : FileRepository {

    override suspend fun getStorageRoots(): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            val response = router.api.getStorageRoots()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to load storage roots: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listFiles(path: String): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            val response = router.api.listFiles(path)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to list files: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createFolder(parentPath: String, folderName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fullPath = if (parentPath.endsWith("/") || parentPath.endsWith("\\")) {
                "$parentPath$folderName"
            } else {
                "$parentPath/$folderName"
            }
            val response = router.api.createDirectory(mapOf("path" to fullPath))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Create folder failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = router.api.deleteFile(path)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFileMetadata(path: String): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val response = router.api.getFileInfo(path)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Metadata fetch failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getStreamUrl(filePath: String): String {
        val baseUrl = credentialsRepo.getLocalUrl() ?: "http://localhost:8080"
        val encodedPath = URLEncoder.encode(filePath, "UTF-8")
        val token = credentialsRepo.getAccessToken() ?: ""
        return "$baseUrl/api/files/stream?path=$encodedPath&token=$token"
    }
}
