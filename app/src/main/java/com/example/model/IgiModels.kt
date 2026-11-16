package com.example.model

import kotlinx.serialization.Serializable

enum class RendererBackend(val id: String, val displayName: String) {
    VULKAN("vulkan", "VULKAN"),
    WEBGPU("webgpu", "WEBGPU");

    companion object {
        fun fromId(id: String?): RendererBackend {
            return if (id == WEBGPU.id) WEBGPU else VULKAN
        }
    }
}

data class ContentLayer(
    val id: String,
    val name: String,
    val kind: String,
    val path: String?,
    val enabled: Boolean,
    val fileCount: Int = 0
)

data class CameraPoint(
    val x: Double,
    val y: Double,
    val z: Double,
    val radius: Double,
    val height: Double
)

data class FogPreset(
    val density: Double,
    val heightFalloff: Double,
    val anisotropy: Double,
    val uniformFraction: Double,
    val sunIntensity: Double,
    val ambientIntensity: Double
)

data class MissionInfo(
    val number: Int,
    val title: String,
    val imageAssetPath: String,
    val fogPreset: FogPreset,
    val cameraPoints: List<CameraPoint>,
    val cameraSpeed: Double,
    val hasCustomLut: Boolean
)

data class EngineAssetCatalog(
    val missions: List<MissionInfo>,
    val lampCount: Int,
    val materialOverrideCount: Int,
    val glassMaterialCount: Int
)

data class CheatsState(
    val infiniteHealth: Boolean = false,
    val infiniteAmmo: Boolean = false,
    val allWeapons: Boolean = false
)

data class GamepadSnapshot(
    val connected: Boolean = false,
    val deviceId: Int = -1,
    val deviceName: String = "No controller connected",
    val leftX: Float = 0f,
    val leftY: Float = 0f,
    val rightX: Float = 0f,
    val rightY: Float = 0f,
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,
    val lastInputLabel: String = "Idle"
)

data class GyroSnapshot(
    val available: Boolean = false,
    val enabled: Boolean = true,
    val yawRadians: Float = 0f,
    val pitchRadians: Float = 0f
)

data class InstallProgress(
    val isRunning: Boolean = false,
    val isIndeterminate: Boolean = true,
    val label: String = "",
    val percent: Int = 0,
    val doneBytes: Long = 0L,
    val totalBytes: Long = 0L
)

data class WeaponSlot(
    val slot: Int,
    val name: String,
    val defaultUnlocked: Boolean,
    val magSize: Int,
    val reserveMax: Int
)

sealed interface AppRoute {
    @Serializable
    data object Launcher : AppRoute

    @Serializable
    data object Campaign : AppRoute

    @Serializable
    data object ContentManager : AppRoute

    @Serializable
    data object EngineSettings : AppRoute

    @Serializable
    data class MissionHud(val missionNumber: Int) : AppRoute
}
