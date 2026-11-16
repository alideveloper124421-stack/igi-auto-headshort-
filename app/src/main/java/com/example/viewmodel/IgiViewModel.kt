package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.IgiAssetsLoader
import com.example.data.IgiStorageRepository
import com.example.hardware.IgiHardwareController
import com.example.model.CheatsState
import com.example.model.ContentLayer
import com.example.model.EngineAssetCatalog
import com.example.model.GamepadSnapshot
import com.example.model.GyroSnapshot
import com.example.model.InstallProgress
import com.example.model.MissionInfo
import com.example.model.RendererBackend
import com.example.model.WeaponSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class IgiUiState(
    val isInitialized: Boolean = false,
    val isGameFolderVerified: Boolean = false,
    val statusText: String = "INITIALIZING SYSTEM ASSEMBLIES...",
    val rendererBackend: RendererBackend = RendererBackend.VULKAN,
    val targetFps: Int = 30,
    val rainAudioEnabled: Boolean = true,
    val voiceMicPermissionGranted: Boolean = false,
    val voiceMicRequested: Boolean = false,
    val installProgress: InstallProgress = InstallProgress(),
    val contentLayers: List<ContentLayer> = emptyList(),
    val catalog: EngineAssetCatalog = EngineAssetCatalog(emptyList(), 0, 0, 0),
    val selectedMissionNumber: Int = 1,
    val cheats: CheatsState = CheatsState(),
    val aimAssistConfig: com.example.combat.AimAssistConfig = com.example.combat.AimAssistConfig(),
    val weapons: List<WeaponSlot> = emptyList(),
    val toastMessage: String? = null
)

class IgiViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = IgiStorageRepository(application)
    private val assetsLoader = IgiAssetsLoader(application)
    val hardware = IgiHardwareController(application)

    private val _uiState = MutableStateFlow(IgiUiState())
    val uiState: StateFlow<IgiUiState> = _uiState.asStateFlow()

    val gyroSnapshot: StateFlow<GyroSnapshot> = hardware.gyroSnapshot
    val gamepadSnapshot: StateFlow<GamepadSnapshot> = hardware.gamepadSnapshot

    var launcherBackgroundBitmap: Bitmap? = null
        private set

    private val missionThumbnails = mutableMapOf<Int, Bitmap>()

    init {
        initializeEngineState()
    }

    private fun initializeEngineState() {
        viewModelScope.launch(Dispatchers.IO) {
            launcherBackgroundBitmap = assetsLoader.loadBitmapFromAssets(
                "img.png", "boot-bg.png", "loading.png"
            )
            val catalog = assetsLoader.loadEngineCatalog()
            catalog.missions.forEach { mission ->
                assetsLoader.loadBitmapFromAssets(mission.imageAssetPath)?.let { bmp ->
                    missionThumbnails[mission.number] = bmp
                }
            }
            storage.ensureContentProfile()
            val layers = storage.loadContentLayers()
            val verified = storage.checkExistingGameFolder()
            val backend = storage.getRendererBackend()
            val targetFps = storage.getTargetFps()
            val gyroEnabled = storage.isGyroEnabled()
            val rainEnabled = storage.isRainAudioEnabled()
            val aimAssistConfig = storage.getAimAssistConfig()
            hardware.setGyroEnabled(gyroEnabled)

            _uiState.update {
                it.copy(
                    isInitialized = true,
                    isGameFolderVerified = verified,
                    statusText = if (verified) "GAME FILES VERIFIED & READY!" else "SELECT GAME FOLDER TO CONTINUE",
                    rendererBackend = backend,
                    targetFps = targetFps,
                    rainAudioEnabled = rainEnabled,
                    aimAssistConfig = aimAssistConfig,
                    contentLayers = layers,
                    catalog = catalog,
                    weapons = assetsLoader.defaultArsenal()
                )
            }
        }
    }

    fun getMissionBitmap(missionNumber: Int): Bitmap? = missionThumbnails[missionNumber]

    fun getMissionInfo(missionNumber: Int): MissionInfo? =
        _uiState.value.catalog.missions.find { it.number == missionNumber }

    fun setRendererBackend(backend: RendererBackend) {
        storage.setRendererBackend(backend)
        _uiState.update { it.copy(rendererBackend = backend) }
    }

    fun setTargetFps(fps: Int) {
        storage.setTargetFps(fps)
        _uiState.update { it.copy(targetFps = fps) }
    }

    fun setGyroEnabled(enabled: Boolean) {
        storage.setGyroEnabled(enabled)
        hardware.setGyroEnabled(enabled)
    }

    fun setRainAudioEnabled(enabled: Boolean) {
        storage.setRainAudioEnabled(enabled)
        _uiState.update { it.copy(rainAudioEnabled = enabled) }
        hardware.updateRainAudio(enabled)
    }

    fun syncRainAudioForScene(isSceneActive: Boolean, isCutsceneOrPaused: Boolean) {
        val shouldPlay = isSceneActive && !isCutsceneOrPaused && _uiState.value.rainAudioEnabled
        hardware.updateRainAudio(shouldPlay)
    }

    fun selectMission(missionNumber: Int) {
        _uiState.update { it.copy(selectedMissionNumber = missionNumber) }
    }

    fun updateCheats(transform: (CheatsState) -> CheatsState) {
        _uiState.update { it.copy(cheats = transform(it.cheats)) }
    }

    fun setAimAssistMode(mode: com.example.combat.AimAssistMode) {
        val updated = _uiState.value.aimAssistConfig.copy(mode = mode)
        storage.setAimAssistConfig(updated)
        _uiState.update { it.copy(aimAssistConfig = updated) }
    }

    fun updateAimAssistConfig(transform: (com.example.combat.AimAssistConfig) -> com.example.combat.AimAssistConfig) {
        val updated = transform(_uiState.value.aimAssistConfig)
        storage.setAimAssistConfig(updated)
        _uiState.update { it.copy(aimAssistConfig = updated) }
    }

    fun onGameFolderPicked(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    statusText = "VERIFYING GAME FOLDER STRUCTURE...",
                    installProgress = InstallProgress(isRunning = true, isIndeterminate = true)
                )
            }
            val result = storage.installGameFolderFromTree(
                treeUri = uri,
                onStage = { stage ->
                    _uiState.update { it.copy(statusText = stage) }
                },
                onProgress = { pct, done, total ->
                    val label = "INSTALLING GAME FILES... $pct% (${formatMb(done)} / ${formatMb(total)})"
                    _uiState.update {
                        it.copy(
                            statusText = label,
                            installProgress = InstallProgress(
                                isRunning = true,
                                isIndeterminate = false,
                                label = label,
                                percent = pct,
                                doneBytes = done,
                                totalBytes = total
                            )
                        )
                    }
                }
            )

            when (result) {
                is IgiStorageRepository.CopyResult.Success -> {
                    val layers = storage.loadContentLayers()
                    _uiState.update {
                        it.copy(
                            isGameFolderVerified = true,
                            statusText = result.message,
                            contentLayers = layers,
                            installProgress = InstallProgress(isRunning = false, isIndeterminate = false, percent = 100),
                            toastMessage = "Project I.G.I. game files installed (${result.fileCount} files)."
                        )
                    }
                }
                is IgiStorageRepository.CopyResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            statusText = result.reason,
                            installProgress = InstallProgress(isRunning = false),
                            toastMessage = result.reason
                        )
                    }
                }
            }
        }
    }

    fun onContentPackPicked(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    statusText = "VERIFYING CONTENT PACK...",
                    installProgress = InstallProgress(isRunning = true, isIndeterminate = true)
                )
            }
            val result = storage.installContentPackFromTree(
                treeUri = uri,
                onStage = { stage ->
                    _uiState.update { it.copy(statusText = stage) }
                },
                onProgress = { pct, done, total ->
                    val label = "INSTALLING CONTENT PACK... $pct% (${formatMb(done)} / ${formatMb(total)})"
                    _uiState.update {
                        it.copy(
                            statusText = label,
                            installProgress = InstallProgress(
                                isRunning = true,
                                isIndeterminate = false,
                                label = label,
                                percent = pct,
                                doneBytes = done,
                                totalBytes = total
                            )
                        )
                    }
                }
            )
            when (result) {
                is IgiStorageRepository.CopyResult.Success -> {
                    val layers = storage.loadContentLayers()
                    _uiState.update {
                        it.copy(
                            statusText = result.message,
                            contentLayers = layers,
                            installProgress = InstallProgress(isRunning = false, isIndeterminate = false, percent = 100),
                            toastMessage = "Content pack installed. Manage load order in Configuration > Content."
                        )
                    }
                }
                is IgiStorageRepository.CopyResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            statusText = result.reason,
                            installProgress = InstallProgress(isRunning = false),
                            toastMessage = result.reason
                        )
                    }
                }
            }
        }
    }

    fun disableAllContent() {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = storage.disableAllContent()
            val layers = storage.loadContentLayers()
            _uiState.update {
                it.copy(
                    contentLayers = layers,
                    statusText = if (ok) "ALL CONTENT DISABLED" else "FAILED TO DISABLE CONTENT",
                    toastMessage = if (ok) {
                        "All content packs and local overrides are off. Re-enable them in Configuration > Content."
                    } else {
                        "Could not update content profile."
                    }
                )
            }
        }
    }

    fun toggleLayerEnabled(layerId: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = storage.toggleLayerEnabled(layerId, enabled)
            _uiState.update { it.copy(contentLayers = updated) }
        }
    }

    fun moveLayerPriority(layerId: String, delta: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = storage.moveLayer(layerId, delta)
            _uiState.update { it.copy(contentLayers = updated) }
        }
    }

    fun rescanContentLayers() {
        viewModelScope.launch(Dispatchers.IO) {
            storage.ensureContentProfile()
            val updated = storage.loadContentLayers()
            _uiState.update {
                it.copy(
                    contentLayers = updated,
                    toastMessage = "Content layers rescanned (${updated.size} layers found)."
                )
            }
        }
    }

    fun onVoiceMicPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                voiceMicRequested = true,
                voiceMicPermissionGranted = granted,
                toastMessage = if (granted) {
                    "Co-op voice microphone enabled."
                } else {
                    "Microphone denied; voice send stays off while text and incoming audio continue."
                }
            )
        }
    }

    fun exportLogcat() {
        viewModelScope.launch(Dispatchers.IO) {
            val res = storage.exportLogcatToDownloads()
            _uiState.update {
                it.copy(
                    toastMessage = res.fold(
                        onSuccess = { path -> "Logs saved to $path" },
                        onFailure = { err -> "Failed to export logs: ${err.message}" }
                    )
                )
            }
        }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        hardware.release()
    }

    private fun formatMb(bytes: Long): String =
        String.format(Locale.US, "%.0f MB", bytes / (1024.0 * 1024.0))
}
