package com.localsync.android.ui.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localsync.android.domain.usecase.AutoReconnectUseCase
import com.localsync.android.domain.usecase.PairDeviceUseCase
import com.localsync.android.domain.usecase.UnpairDeviceUseCase
import com.localsync.android.security.SecureCredentialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PairingUiState {
    object Idle : PairingUiState()
    object Loading : PairingUiState()
    data class Success(val deviceName: String) : PairingUiState()
    data class Error(val message: String) : PairingUiState()
}

class PairingViewModel(
    private val pairDeviceUseCase: PairDeviceUseCase,
    private val autoReconnectUseCase: AutoReconnectUseCase,
    private val unpairDeviceUseCase: UnpairDeviceUseCase,
    private val credentialsRepo: SecureCredentialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PairingUiState>(PairingUiState.Idle)
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    val isPaired: Boolean
        get() = credentialsRepo.isPaired()

    init {
        // Zero-friction reconnect on app start
        if (credentialsRepo.isPaired()) {
            attemptAutoReconnect()
        }
    }

    fun onQrScanned(qrContent: String) {
        viewModelScope.launch {
            _uiState.value = PairingUiState.Loading
            val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
            val result = pairDeviceUseCase(qrContent, deviceModel)
            result.fold(
                onSuccess = { name ->
                    _uiState.value = PairingUiState.Success(name)
                },
                onFailure = { error ->
                    _uiState.value = PairingUiState.Error(error.message ?: "Pairing failed")
                }
            )
        }
    }

    fun attemptAutoReconnect() {
        viewModelScope.launch {
            _uiState.value = PairingUiState.Loading
            val result = autoReconnectUseCase()
            result.fold(
                onSuccess = { name ->
                    _uiState.value = PairingUiState.Success(name)
                },
                onFailure = { error ->
                    _uiState.value = PairingUiState.Error(error.message ?: "Reconnection failed")
                }
            )
        }
    }

    fun unpair() {
        unpairDeviceUseCase()
        _uiState.value = PairingUiState.Idle
    }
}
