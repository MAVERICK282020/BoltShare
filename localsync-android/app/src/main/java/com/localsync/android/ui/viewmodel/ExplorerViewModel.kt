package com.localsync.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localsync.android.data.api.NetworkRouter
import com.localsync.android.data.api.RouteMode
import com.localsync.android.data.model.FileItem
import com.localsync.android.domain.usecase.BrowseStorageUseCase
import com.localsync.android.domain.usecase.ManageFileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ExplorerUiState {
    object Loading : ExplorerUiState()
    data class Content(val files: List<FileItem>, val currentPath: String) : ExplorerUiState()
    data class Error(val message: String) : ExplorerUiState()
}

class ExplorerViewModel(
    private val browseStorageUseCase: BrowseStorageUseCase,
    private val manageFileUseCase: ManageFileUseCase,
    private val networkRouter: NetworkRouter
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExplorerUiState>(ExplorerUiState.Loading)
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    private val _pathHistory = mutableListOf<String>()
    var currentPath: String = ""
        private set

    val routeModeFlow: StateFlow<RouteMode> = networkRouter.routeModeFlow

    val currentRouteMode: RouteMode
        get() = networkRouter.currentMode

    init {
        loadRoots()
    }

    fun loadRoots() {
        viewModelScope.launch {
            _uiState.value = ExplorerUiState.Loading
            networkRouter.resolveBestRoute()
            val result = browseStorageUseCase.getRoots()
            result.fold(
                onSuccess = { roots ->
                    currentPath = "Storage Roots"
                    _pathHistory.clear()
                    _uiState.value = ExplorerUiState.Content(roots, currentPath)
                },
                onFailure = { error ->
                    _uiState.value = ExplorerUiState.Error(error.message ?: "Failed to load storage roots")
                }
            )
        }
    }

    fun openDirectory(item: FileItem) {
        if (!item.isDirectory) return
        viewModelScope.launch {
            _pathHistory.add(currentPath)
            currentPath = item.path
            loadDirectory(item.path)
        }
    }

    fun navigateUp(): Boolean {
        if (_pathHistory.isEmpty()) {
            return false
        }
        val prevPath = _pathHistory.removeAt(_pathHistory.size - 1)
        currentPath = prevPath
        if (prevPath == "Storage Roots") {
            loadRoots()
        } else {
            loadDirectory(prevPath)
        }
        return true
    }

    private fun loadDirectory(path: String) {
        viewModelScope.launch {
            _uiState.value = ExplorerUiState.Loading
            val result = browseStorageUseCase.listDirectory(path)
            result.fold(
                onSuccess = { files ->
                    _uiState.value = ExplorerUiState.Content(files, path)
                },
                onFailure = { error ->
                    _uiState.value = ExplorerUiState.Error(error.message ?: "Failed to list directory")
                }
            )
        }
    }

    fun deleteFile(path: String) {
        viewModelScope.launch {
            manageFileUseCase.delete(path)
            loadDirectory(currentPath)
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            manageFileUseCase.createFolder(currentPath, name)
            loadDirectory(currentPath)
        }
    }

    fun getStreamingUrl(filePath: String): String {
        return browseStorageUseCase.getStreamingUrl(filePath)
    }
}
