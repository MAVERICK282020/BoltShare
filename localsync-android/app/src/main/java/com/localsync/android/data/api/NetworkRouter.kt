package com.localsync.android.data.api

import com.localsync.android.security.SecureCredentialRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

enum class RouteMode {
    LAN_DIRECT,
    REMOTE_GATEWAY,
    DISCONNECTED
}

class NetworkRouter(
    private val credentialsRepo: SecureCredentialRepository
) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private var activeBaseUrl: String = credentialsRepo.getLocalUrl() ?: "http://localhost:8080"

    private val _routeMode = MutableStateFlow(
        if (credentialsRepo.isPaired()) RouteMode.LAN_DIRECT else RouteMode.DISCONNECTED
    )
    val routeModeFlow: StateFlow<RouteMode> = _routeMode.asStateFlow()

    var currentMode: RouteMode
        get() = _routeMode.value
        set(value) {
            _routeMode.value = value
        }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = credentialsRepo.getAccessToken()
        val requestBuilder = original.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private var retrofit: Retrofit = buildRetrofit(activeBaseUrl)
    var api: LocalSyncApi = retrofit.create(LocalSyncApi::class.java)
        private set

    private fun buildRetrofit(url: String): Retrofit {
        val sanitized = if (url.endsWith("/")) url else "$url/"
        return Retrofit.Builder()
            .baseUrl(sanitized)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun updateBaseUrl(newUrl: String) {
        if (newUrl.isNotBlank() && newUrl != activeBaseUrl) {
            activeBaseUrl = newUrl
            retrofit = buildRetrofit(activeBaseUrl)
            api = retrofit.create(LocalSyncApi::class.java)
        }
    }

    /**
     * Smart Ping: checks if LAN Direct (Wi-Fi) is reachable.
     * If not, automatically falls back to Remote Gateway (Anywhere Access)!
     */
    suspend fun resolveBestRoute(): RouteMode = withContext(Dispatchers.IO) {
        val localUrl = credentialsRepo.getLocalUrl()
        val remoteUrl = credentialsRepo.getRemoteUrl()

        // 1. Try LAN Direct first (highest speed, zero internet usage)
        if (!localUrl.isNullOrBlank()) {
            val isLanAlive = pingEndpoint("$localUrl/api/health")
            if (isLanAlive) {
                updateBaseUrl(localUrl)
                currentMode = RouteMode.LAN_DIRECT
                return@withContext RouteMode.LAN_DIRECT
            }
        }

        // 2. Fallback to Remote Gateway (Mobile data / Remote network)
        if (!remoteUrl.isNullOrBlank() && remoteUrl.startsWith("http")) {
            val isRemoteAlive = pingEndpoint("$remoteUrl/api/health")
            if (isRemoteAlive) {
                updateBaseUrl(remoteUrl)
                currentMode = RouteMode.REMOTE_GATEWAY
                return@withContext RouteMode.REMOTE_GATEWAY
            }
        }

        // 3. If ping failed due to ping timeout but localUrl is known, keep LAN_DIRECT as working default
        if (!localUrl.isNullOrBlank()) {
            updateBaseUrl(localUrl)
            currentMode = RouteMode.LAN_DIRECT
            return@withContext RouteMode.LAN_DIRECT
        }

        currentMode = RouteMode.DISCONNECTED
        RouteMode.DISCONNECTED
    }

    private fun pingEndpoint(url: String): Boolean {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(3000, TimeUnit.MILLISECONDS)
                .readTimeout(3000, TimeUnit.MILLISECONDS)
                .build()
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
