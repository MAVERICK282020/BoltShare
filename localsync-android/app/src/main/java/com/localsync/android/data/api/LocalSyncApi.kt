package com.localsync.android.data.api

import com.localsync.android.data.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface LocalSyncApi {

    // --- Authentication ---
    @POST("/api/auth/pair")
    suspend fun pairDevice(@Body request: PairRequest): Response<AuthResponse>

    @POST("/api/auth/reconnect")
    suspend fun reconnect(@Body request: ReconnectRequest): Response<AuthResponse>

    @GET("/api/auth/network-status")
    suspend fun getNetworkStatus(): Response<NetworkStatusResponse>

    @GET("/api/health")
    suspend fun healthCheck(): Response<Map<String, Any>>

    // --- File Storage ---
    @GET("/api/files/roots")
    suspend fun getStorageRoots(): Response<List<FileItem>>

    @GET("/api/files")
    suspend fun listFiles(@Query("path") path: String): Response<List<FileItem>>

    @GET("/api/files/info")
    suspend fun getFileInfo(@Query("path") path: String): Response<FileItem>

    @Streaming
    @GET("/api/files/download")
    suspend fun downloadFile(@Query("path") path: String): Response<ResponseBody>

    @DELETE("/api/files")
    suspend fun deleteFile(@Query("path") path: String): Response<Map<String, String>>

    @POST("/api/files/mkdir")
    suspend fun createDirectory(@Body body: Map<String, String>): Response<Map<String, String>>

    // --- Chunked Resumable Transfers ---
    @POST("/api/transfer/init")
    suspend fun initTransfer(
        @Query("fileName") fileName: String,
        @Query("totalSize") totalSize: Long,
        @Query("targetDir") targetDir: String
    ): Response<InitTransferResponse>

    @Multipart
    @POST("/api/transfer/chunk/{jobId}")
    suspend fun uploadChunk(
        @Path("jobId") jobId: String,
        @Query("chunkIndex") chunkIndex: Int,
        @Part chunk: MultipartBody.Part
    ): Response<Map<String, Any>>

    @GET("/api/transfer/status/{jobId}")
    suspend fun getTransferStatus(@Path("jobId") jobId: String): Response<TransferStatusDto>

    @POST("/api/transfer/complete/{jobId}")
    suspend fun completeTransfer(
        @Path("jobId") jobId: String,
        @Query("sha256") expectedSha256: String? = null
    ): Response<Map<String, Any>>

    @DELETE("/api/transfer/{jobId}")
    suspend fun cancelTransfer(@Path("jobId") jobId: String): Response<Map<String, String>>
}
