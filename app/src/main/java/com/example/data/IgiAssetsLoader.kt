package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.model.CameraPoint
import com.example.model.EngineAssetCatalog
import com.example.model.FogPreset
import com.example.model.MissionInfo
import com.example.model.WeaponSlot
import org.json.JSONArray
import org.json.JSONObject

class IgiAssetsLoader(private val context: Context) {

    fun loadBitmapFromAssets(vararg candidatePaths: String): Bitmap? {
        for (path in candidatePaths) {
            try {
                context.assets.open(path).use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) return bmp
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun loadEngineCatalog(): EngineAssetCatalog {
        val camerasJson = readAssetText("menu-cameras.json")
        val fogJson = readAssetText("fog-presets.json")
        val lampsJson = readAssetText("lamps.json")
        val materialsJson = readAssetText("materials.json")

        val fogRoot = if (fogJson.isNotBlank()) JSONObject(fogJson) else JSONObject()
        val defaultFogObj = fogRoot.optJSONObject("default") ?: JSONObject()
        val missionFogs = fogRoot.optJSONObject("missions") ?: JSONObject()

        val defaultFog = parseFog(defaultFogObj, null)
        val customLutMissions = setOf(1, 3, 5, 7)

        val missions = mutableListOf<MissionInfo>()
        if (camerasJson.isNotBlank()) {
            val arr = JSONArray(camerasJson)
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val number = item.optInt("mission", i + 1)
                val name = item.optString("comment", "Mission $number")
                val speed = item.optDouble("speed", 0.12)
                val ptsArr = item.optJSONArray("points") ?: JSONArray()
                val pts = mutableListOf<CameraPoint>()
                for (p in 0 until ptsArr.length()) {
                    val pt = ptsArr.getJSONObject(p)
                    pts.add(
                        CameraPoint(
                            x = pt.optDouble("x", 0.0),
                            y = pt.optDouble("y", 0.0),
                            z = pt.optDouble("z", 0.0),
                            radius = pt.optDouble("radius", 25.0),
                            height = pt.optDouble("height", 10.0)
                        )
                    )
                }
                val missionFogObj = missionFogs.optJSONObject(number.toString())
                val fog = if (missionFogObj != null) parseFog(missionFogObj, defaultFog) else defaultFog
                missions.add(
                    MissionInfo(
                        number = number,
                        title = name,
                        imageAssetPath = "mission_images/mission$number.png",
                        fogPreset = fog,
                        cameraPoints = pts,
                        cameraSpeed = speed,
                        hasCustomLut = number in customLutMissions
                    )
                )
            }
        }

        val lampCount = if (lampsJson.isNotBlank()) {
            JSONObject(lampsJson).optJSONArray("lamps")?.length() ?: 0
        } else 0

        var materialCount = 0
        var glassCount = 0
        if (materialsJson.isNotBlank()) {
            val texturesObj = JSONObject(materialsJson).optJSONObject("textures")
            if (texturesObj != null) {
                val keys = texturesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key.startsWith("$")) continue
                    val obj = texturesObj.optJSONObject(key) ?: continue
                    materialCount++
                    if (obj.optBoolean("glass", false)) {
                        glassCount++
                    }
                }
            }
        }

        return EngineAssetCatalog(
            missions = missions.sortedBy { it.number },
            lampCount = lampCount,
            materialOverrideCount = materialCount,
            glassMaterialCount = glassCount
        )
    }

    fun defaultArsenal(): List<WeaponSlot> = listOf(
        WeaponSlot(1, "Combat Knife", defaultUnlocked = true, magSize = 1, reserveMax = 1),
        WeaponSlot(2, "Glock 17", defaultUnlocked = true, magSize = 17, reserveMax = 85),
        WeaponSlot(3, "MP5SD3", defaultUnlocked = true, magSize = 30, reserveMax = 150),
        WeaponSlot(4, "AK-47", defaultUnlocked = false, magSize = 30, reserveMax = 180),
        WeaponSlot(5, "SPAS-12", defaultUnlocked = false, magSize = 8, reserveMax = 32),
        WeaponSlot(6, "Dragunov SVD", defaultUnlocked = false, magSize = 10, reserveMax = 40),
        WeaponSlot(7, "M16A2 / M203", defaultUnlocked = false, magSize = 30, reserveMax = 150),
        WeaponSlot(8, "LAW 80", defaultUnlocked = false, magSize = 1, reserveMax = 3),
        WeaponSlot(9, "Frag Grenades", defaultUnlocked = true, magSize = 1, reserveMax = 6)
    )

    private fun parseFog(obj: JSONObject, fallback: FogPreset?): FogPreset {
        return FogPreset(
            density = obj.optDouble("density", fallback?.density ?: 0.0),
            heightFalloff = obj.optDouble("heightFalloff", fallback?.heightFalloff ?: 24.0),
            anisotropy = obj.optDouble("anisotropy", fallback?.anisotropy ?: 0.7),
            uniformFraction = obj.optDouble("uniformFraction", fallback?.uniformFraction ?: 0.12),
            sunIntensity = obj.optDouble("sunIntensity", fallback?.sunIntensity ?: 1.0),
            ambientIntensity = obj.optDouble("ambientIntensity", fallback?.ambientIntensity ?: 1.0)
        )
    }

    private fun readAssetText(path: String): String {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            ""
        }
    }
}
