package com.localsync.android.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

interface SecureCredentialRepository {
    fun getDeviceId(): String
    fun getDeviceToken(): String?
    fun getAccessToken(): String?
    fun getLocalUrl(): String?
    fun getRemoteUrl(): String?
    fun isPaired(): Boolean
    fun saveCredentials(
        deviceId: String,
        deviceToken: String,
        accessToken: String,
        localUrl: String,
        remoteUrl: String?
    )
    fun updateAccessToken(newToken: String)
    fun clearCredentials()
    val isPairedFlow: Flow<Boolean>
}

class SecureCredentialRepositoryImpl(
    private val context: Context,
    private val keystoreManager: KeystoreManager = KeystoreManager()
) : SecureCredentialRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _isPairedFlow = MutableStateFlow(isPaired())
    override val isPairedFlow: Flow<Boolean> = _isPairedFlow.asStateFlow()

    override fun getDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    override fun getDeviceToken(): String? {
        val encrypted = prefs.getString(KEY_DEVICE_TOKEN, null) ?: return null
        return try { keystoreManager.decrypt(encrypted) } catch (e: Exception) { null }
    }

    override fun getAccessToken(): String? {
        val encrypted = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        return try { keystoreManager.decrypt(encrypted) } catch (e: Exception) { null }
    }

    override fun getLocalUrl(): String? = prefs.getString(KEY_LOCAL_URL, null)

    override fun getRemoteUrl(): String? = prefs.getString(KEY_REMOTE_URL, null)

    override fun isPaired(): Boolean = getDeviceToken() != null

    override fun saveCredentials(
        deviceId: String,
        deviceToken: String,
        accessToken: String,
        localUrl: String,
        remoteUrl: String?
    ) {
        prefs.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_DEVICE_TOKEN, keystoreManager.encrypt(deviceToken))
            .putString(KEY_ACCESS_TOKEN, keystoreManager.encrypt(accessToken))
            .putString(KEY_LOCAL_URL, localUrl)
            .putString(KEY_REMOTE_URL, remoteUrl ?: "")
            .apply()
        _isPairedFlow.value = true
    }

    override fun updateAccessToken(newToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, keystoreManager.encrypt(newToken))
            .apply()
    }

    override fun clearCredentials() {
        prefs.edit()
            .remove(KEY_DEVICE_TOKEN)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_LOCAL_URL)
            .remove(KEY_REMOTE_URL)
            .apply()
        _isPairedFlow.value = false
    }

    companion object {
        private const val PREFS_NAME = "localsync_secure_credentials"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_TOKEN = "device_token_enc"
        private const val KEY_ACCESS_TOKEN = "access_token_enc"
        private const val KEY_LOCAL_URL = "local_url"
        private const val KEY_REMOTE_URL = "remote_url"
    }
}
